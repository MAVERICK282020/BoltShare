package com.localsync.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.localsync.dto.FileInfoDto;
import com.localsync.model.Device;
import com.localsync.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneStorageService {

    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(3000))
        .build();

    /**
     * Resolve the IP address of the connected phone device.
     */
    public String resolvePhoneIp(String deviceId) {
        if (deviceId != null && !deviceId.isBlank() && !"default".equalsIgnoreCase(deviceId)) {
            Optional<Device> dev = deviceRepository.findById(deviceId);
            if (dev.isPresent() && dev.get().getIpAddress() != null && !dev.get().getIpAddress().isBlank()) {
                return dev.get().getIpAddress();
            }
        }
        // Check first trusted device with an IP address
        return deviceRepository.findAll().stream()
            .filter(Device::isTrusted)
            .sorted((a, b) -> {
                if (a.getLastSeen() != null && b.getLastSeen() != null) {
                    return b.getLastSeen().compareTo(a.getLastSeen());
                }
                return 0;
            })
            .map(Device::getIpAddress)
            .filter(ip -> ip != null && !ip.isBlank())
            .findFirst()
            .orElse(null);
    }

    /**
     * Get storage roots for the phone directly over LAN (Port 8085).
     */
    public List<FileInfoDto> getPhoneRoots(String deviceId) {
        String phoneIp = resolvePhoneIp(deviceId);
        if (phoneIp != null) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + phoneIp + ":8085/roots"))
                    .timeout(Duration.ofMillis(3500))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    log.info("Successfully fetched live storage roots from phone ({}:8085)", phoneIp);
                    List<FileInfoDto> phoneRoots = objectMapper.readValue(response.body(), new TypeReference<List<FileInfoDto>>() {});
                    if (!phoneRoots.isEmpty()) {
                        return phoneRoots;
                    }
                }
            } catch (Exception e) {
                log.warn("Direct phone roots query to {}:8085 failed ({})", phoneIp, e.getMessage());
            }
        }

        // Return empty list if no phone is connected or reachable
        return Collections.emptyList();
    }

    /**
     * List files in phone directory directly from the phone device.
     */
    public List<FileInfoDto> listPhoneDirectory(String deviceId, String requestedPath) throws IOException {
        if (requestedPath == null || requestedPath.isBlank() || requestedPath.equals("/")) {
            return getPhoneRoots(deviceId);
        }

        String phoneIp = resolvePhoneIp(deviceId);
        if (phoneIp != null) {
            try {
                String encodedPath = URLEncoder.encode(requestedPath, StandardCharsets.UTF_8);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + phoneIp + ":8085/files?path=" + encodedPath))
                    .timeout(Duration.ofMillis(4000))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    log.info("Successfully listed live files from phone for path: {}", requestedPath);
                    return objectMapper.readValue(response.body(), new TypeReference<List<FileInfoDto>>() {});
                }
            } catch (Exception e) {
                log.warn("Direct phone directory listing for {} failed ({})", requestedPath, e.getMessage());
            }
        }

        return Collections.emptyList();
    }

    /**
     * Fetch file input stream from phone over LAN (supports inline preview and download).
     */
    public HttpResponse<InputStream> fetchPhoneFileResponse(String deviceId, String requestedPath, boolean inline) throws IOException, InterruptedException {
        String phoneIp = resolvePhoneIp(deviceId);
        if (phoneIp != null) {
            String encodedPath = URLEncoder.encode(requestedPath, StandardCharsets.UTF_8);
            String url = "http://" + phoneIp + ":8085/download?path=" + encodedPath + (inline ? "&inline=true" : "");
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(15))
                .GET()
                .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        }
        return null;
    }

    /**
     * Save local disk file directly to phone storage.
     */
    public FileInfoDto saveLocalFileToPhoneStorage(String deviceId, String targetPath, Path localFilePath) throws IOException {
        String phoneIp = resolvePhoneIp(deviceId);
        String fileName = localFilePath.getFileName().toString();
        long fileSize = Files.size(localFilePath);
        String resolvedTarget = (targetPath != null && targetPath.startsWith("/")) ? targetPath : "/storage/emulated/0/Download";

        if (phoneIp != null) {
            try {
                String encodedPath = URLEncoder.encode(resolvedTarget, StandardCharsets.UTF_8);
                String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + phoneIp + ":8085/upload?path=" + encodedPath + "&name=" + encodedName))
                    .timeout(Duration.ofMinutes(15))
                    .POST(HttpRequest.BodyPublishers.ofFile(localFilePath))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    log.info("Direct local-to-phone upload succeeded for {}", fileName);
                    return FileInfoDto.builder()
                        .name(fileName)
                        .path(resolvedTarget + "/" + fileName)
                        .isDirectory(false)
                        .size(fileSize)
                        .lastModified(System.currentTimeMillis())
                        .build();
                } else {
                    throw new IOException("Phone rejected upload with status: " + response.statusCode());
                }
            } catch (Exception e) {
                log.error("Direct local-to-phone upload failed: {}", e.getMessage());
                throw new IOException("Upload to phone failed: " + e.getMessage(), e);
            }
        }

        throw new IOException("Cannot upload: No phone device is connected or reachable on LAN");
    }

    /**
     * Save file directly to phone storage over LAN.
     */
    public FileInfoDto saveFileToPhoneStorage(String deviceId, String targetPath, MultipartFile file) throws IOException {
        String phoneIp = resolvePhoneIp(deviceId);
        if (phoneIp != null) {
            try {
                // If targetPath is empty or relative, default to Download directory on phone
                String resolvedTarget = targetPath;
                if (resolvedTarget == null || resolvedTarget.isBlank() || !resolvedTarget.startsWith("/")) {
                    resolvedTarget = "/storage/emulated/0/Download";
                }
                String encodedPath = URLEncoder.encode(resolvedTarget, StandardCharsets.UTF_8);
                String fileName = (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) 
                    ? file.getOriginalFilename() 
                    : "upload_" + System.currentTimeMillis();
                String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

                log.info("Streaming upload to phone {}:8085 -> target: {}, file: {}, size: {} bytes", phoneIp, resolvedTarget, fileName, file.getSize());

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + phoneIp + ":8085/upload?path=" + encodedPath + "&name=" + encodedName))
                    .timeout(Duration.ofMinutes(10))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    log.info("Direct upload to phone succeeded! Response: {}", response.body());
                    return FileInfoDto.builder()
                        .name(fileName)
                        .path(resolvedTarget + "/" + fileName)
                        .isDirectory(false)
                        .size(file.getSize())
                        .lastModified(System.currentTimeMillis())
                        .build();
                } else {
                    throw new IOException("Phone rejected upload with status: " + response.statusCode());
                }
            } catch (Exception e) {
                log.error("Direct upload to phone failed: {}", e.getMessage());
                throw new IOException("Upload to phone failed: " + e.getMessage(), e);
            }
        }

        throw new IOException("Cannot upload: No phone device is connected or reachable on LAN");
    }

    /**
     * Delete file from phone
     */
    public boolean deletePhoneFile(String deviceId, String requestedPath) {
        String phoneIp = resolvePhoneIp(deviceId);
        if (phoneIp != null && requestedPath != null && requestedPath.startsWith("/")) {
            try {
                String encodedPath = URLEncoder.encode(requestedPath, StandardCharsets.UTF_8);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + phoneIp + ":8085/delete?path=" + encodedPath))
                    .timeout(Duration.ofMillis(3000))
                    .GET()
                    .build();
                HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return res.statusCode() == 200;
            } catch (Exception e) {
                log.warn("Phone delete failed", e);
            }
        }
        try {
            Path p = Paths.get(requestedPath);
            return Files.deleteIfExists(p);
        } catch (Exception e) {
            return false;
        }
    }

}
