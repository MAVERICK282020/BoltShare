package com.localsync.controller;

import com.localsync.dto.FileInfoDto;
import com.localsync.model.Device;
import com.localsync.repository.DeviceRepository;
import com.localsync.service.PhoneStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/phone")
@RequiredArgsConstructor
@Slf4j
public class PhoneStorageController {

    private final PhoneStorageService phoneStorageService;
    private final DeviceRepository deviceRepository;

    private String resolveDeviceId(String deviceId) {
        if (deviceId != null && !deviceId.isBlank() && !"null".equalsIgnoreCase(deviceId)) {
            return deviceId;
        }
        return deviceRepository.findAll().stream()
            .filter(Device::isTrusted)
            .sorted((a, b) -> {
                if (a.getLastSeen() != null && b.getLastSeen() != null) {
                    return b.getLastSeen().compareTo(a.getLastSeen());
                }
                if (a.getPairedAt() != null && b.getPairedAt() != null) {
                    return b.getPairedAt().compareTo(a.getPairedAt());
                }
                return 0;
            })
            .map(Device::getDeviceId)
            .findFirst()
            .orElse(null);
    }

    /**
     * Get Phone storage roots (Internal Storage, Photos, Downloads, Documents, etc.)
     * GET /api/phone/roots
     */
    @GetMapping("/roots")
    public ResponseEntity<List<FileInfoDto>> getPhoneRoots(@RequestParam(required = false) String deviceId) {
        String activeId = resolveDeviceId(deviceId);
        if (activeId == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        return ResponseEntity.ok(phoneStorageService.getPhoneRoots(activeId));
    }

    /**
     * List files in phone directory
     * GET /api/phone/files?path=...
     */
    @GetMapping("/files")
    public ResponseEntity<List<FileInfoDto>> listPhoneFiles(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String deviceId) {
        try {
            String activeId = resolveDeviceId(deviceId);
            return ResponseEntity.ok(phoneStorageService.listPhoneDirectory(activeId, path));
        } catch (IOException e) {
            log.error("Failed to list phone files for path: {}", path, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Download or view/stream a file from phone storage
     * GET /api/phone/download?path=...&inline=true
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadPhoneFile(
            @RequestParam String path,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false, defaultValue = "false") boolean inline) {
        String activeId = resolveDeviceId(deviceId);

        // 1. Try streaming directly from phone over LAN
        try {
            HttpResponse<InputStream> phoneRes = phoneStorageService.fetchPhoneFileResponse(activeId, path, inline);
            if (phoneRes != null && phoneRes.statusCode() == 200) {
                String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : "file";
                String contentType = phoneRes.headers().firstValue("content-type").orElse("application/octet-stream");
                long contentLength = phoneRes.headers().firstValueAsLong("content-length").orElse(-1L);

                String disposition = inline ? "inline" : "attachment";
                var builder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

                if (contentLength > 0) {
                    builder.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
                }
                return builder.body(new InputStreamResource(phoneRes.body()));
            }
        } catch (Exception e) {
            log.warn("Direct stream from phone failed ({}), checking local file", e.getMessage());
        }

        // 2. Fallback to local PC filesystem
        try {
            Path filePath = Paths.get(path).normalize().toAbsolutePath();
            if (!Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new PathResource(filePath);
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) mimeType = "application/octet-stream";

            String disposition = inline ? "inline" : "attachment";
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filePath.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(filePath)))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Preview or stream file directly (alias with inline=true)
     * GET /api/phone/stream?path=...
     */
    @GetMapping("/stream")
    public ResponseEntity<Resource> streamPhoneFile(
            @RequestParam String path,
            @RequestParam(required = false) String deviceId) {
        return downloadPhoneFile(path, deviceId, true);
    }

    /**
     * Upload / push a file to phone storage from PC
     * POST /api/phone/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<FileInfoDto> uploadToPhone(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String targetPath,
            @RequestParam("file") MultipartFile file) {
        try {
            String activeId = resolveDeviceId(deviceId);
            FileInfoDto result = phoneStorageService.saveFileToPhoneStorage(activeId, targetPath, file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("Phone upload failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete file from phone storage
     * DELETE /api/phone/files?path=...
     */
    @DeleteMapping("/files")
    public ResponseEntity<Map<String, Object>> deletePhoneFile(
            @RequestParam String path,
            @RequestParam(required = false) String deviceId) {
        String activeId = resolveDeviceId(deviceId);
        boolean deleted = phoneStorageService.deletePhoneFile(activeId, path);
        return ResponseEntity.ok(Map.of("deleted", deleted, "path", path));
    }

    private final java.util.concurrent.ConcurrentHashMap<String, Map<String, Object>> lastKnownStorageCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.net.http.HttpClient sharedHttpClient = java.net.http.HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofMillis(3500))
        .build();

    public void clearCacheForDevice(String deviceId) {
        if (deviceId != null) {
            lastKnownStorageCache.remove(deviceId);
        } else {
            lastKnownStorageCache.clear();
        }
    }

    /**
     * Get storage capacity and usage for Phone internal storage.
     * GET /api/phone/storage-info
     */
    @GetMapping("/storage-info")
    public ResponseEntity<Map<String, Object>> getPhoneStorageInfo(@RequestParam(required = false) String deviceId) {
        String activeId = resolveDeviceId(deviceId);
        if (activeId == null) {
            // No device is paired or connected!
            return ResponseEntity.ok(Map.of("connected", false, "totalBytes", 0L, "usedBytes", 0L, "freeBytes", 0L, "usedPercentage", 0));
        }

        String phoneIp = phoneStorageService.resolvePhoneIp(activeId);
        if (phoneIp != null) {
            try {
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://" + phoneIp + ":8085/storage-info"))
                    .timeout(java.time.Duration.ofMillis(3500))
                    .GET()
                    .build();
                java.net.http.HttpResponse<String> resp = sharedHttpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(resp.body(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    long total = Long.parseLong(map.getOrDefault("totalBytes", "0").toString());
                    long used = Long.parseLong(map.getOrDefault("usedBytes", "0").toString());
                    long free = Long.parseLong(map.getOrDefault("freeBytes", "0").toString());
                    int usedPercentage = total > 0 ? (int) Math.round(((double) used / total) * 100) : 0;
                    map.put("usedPercentage", usedPercentage);
                    map.put("drive", "Internal Storage");
                    map.put("connected", true);
                    lastKnownStorageCache.put(activeId, map);
                    return ResponseEntity.ok(map);
                }
            } catch (Exception e) {
                log.debug("Live storage-info query timed out or unreachable: {}", e.getMessage());
            }
        }

        // If we have a cached reading for THIS active paired device, return it (handles phone screen-off Doze mode)
        Map<String, Object> cached = lastKnownStorageCache.get(activeId);
        if (cached != null) {
            return ResponseEntity.ok(cached);
        }

        return ResponseEntity.ok(Map.of("connected", false, "totalBytes", 0L, "usedBytes", 0L, "freeBytes", 0L, "usedPercentage", 0));
    }
}
