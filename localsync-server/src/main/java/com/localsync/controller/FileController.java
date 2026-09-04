package com.localsync.controller;

import com.localsync.dto.FileInfoDto;
import com.localsync.service.FileManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileManagerService fileManagerService;

    /**
     * List directory contents.
     * GET /api/files?path=/Users/aksha/Documents
     */
    @GetMapping
    public ResponseEntity<List<FileInfoDto>> listFiles(@RequestParam String path) {
        try {
            log.info("listFiles requested path: '{}'", path);
            List<FileInfoDto> files = fileManagerService.listDirectory(path);
            return ResponseEntity.ok(files);
        } catch (SecurityException e) {
            log.warn("Access denied for path '{}': {}", path, e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (IOException e) {
            log.warn("IO error for path '{}': {}", path, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get storage roots (drives + standard folders).
     * GET /api/files/roots
     */
    @GetMapping("/roots")
    public ResponseEntity<List<FileInfoDto>> getStorageRoots() {
        return ResponseEntity.ok(fileManagerService.getStorageRoots());
    }

    /**
     * Download a file.
     * GET /api/files/download?path=/path/to/file.mp4
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path) {
        try {
            Path filePath = fileManagerService.validateAndResolvePath(path);
            if (!Files.isRegularFile(filePath)) {
                return ResponseEntity.badRequest().build();
            }
            Resource resource = new PathResource(filePath);
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) mimeType = "application/octet-stream";

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filePath.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(filePath)))
                .body(resource);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Stream or preview files directly (videos, photos, docs) without full export/import.
     * Supports HTTP 206 Partial Content and Range headers for instant playback.
     * GET /api/files/stream?path=/path/to/video.mp4
     */
    @GetMapping("/stream")
    public ResponseEntity<ResourceRegion> streamFile(
            @RequestParam String path,
            @RequestHeader HttpHeaders headers) {
        try {
            Path filePath = fileManagerService.validateAndResolvePath(path);
            if (!Files.isRegularFile(filePath)) {
                return ResponseEntity.badRequest().build();
            }
            Resource resource = new PathResource(filePath);
            long contentLength = Files.size(filePath);
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) mimeType = "application/octet-stream";

            HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);
            ResourceRegion region;
            if (range != null) {
                long start = range.getRangeStart(contentLength);
                long end = range.getRangeEnd(contentLength);
                long rangeLength = Math.min(1024 * 1024 * 5, end - start + 1); // 5MB buffer
                region = new ResourceRegion(resource, start, rangeLength);
            } else {
                long rangeLength = Math.min(1024 * 1024 * 5, contentLength);
                region = new ResourceRegion(resource, 0, rangeLength);
            }

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                .body(region);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Upload a single file to a directory.
     * POST /api/files/upload?targetDir=/path/to/dir
     */
    @PostMapping("/upload")
    public ResponseEntity<FileInfoDto> uploadFile(
            @RequestParam String targetDir,
            @RequestParam("file") MultipartFile file) {
        try {
            Path dir = fileManagerService.validateAndResolvePath(targetDir);
            Files.createDirectories(dir);
            Path dest = dir.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            FileInfoDto info = fileManagerService.getFileInfo(dest.toString());
            return ResponseEntity.ok(info);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (IOException e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete a file or directory.
     * DELETE /api/files?path=/path/to/file
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteFile(@RequestParam String path) {
        try {
            fileManagerService.delete(path);
            return ResponseEntity.ok(Map.of("status", "deleted", "path", path));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a new directory.
     * POST /api/files/mkdir
     */
    @PostMapping("/mkdir")
    public ResponseEntity<Map<String, String>> createDirectory(@RequestBody Map<String, String> body) {
        try {
            fileManagerService.createDirectory(body.get("path"));
            return ResponseEntity.ok(Map.of("status", "created"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get metadata for a single file.
     * GET /api/files/info?path=/path/to/file
     */
    @GetMapping("/info")
    public ResponseEntity<FileInfoDto> getFileInfo(@RequestParam String path) {
        try {
            return ResponseEntity.ok(fileManagerService.getFileInfo(path));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get storage capacity and usage for PC drive.
     * GET /api/files/storage-info
     */
    @GetMapping("/storage-info")
    public ResponseEntity<Map<String, Object>> getStorageInfo() {
        java.io.File cDrive = new java.io.File("C:\\");
        long totalBytes = cDrive.getTotalSpace();
        long freeBytes = cDrive.getFreeSpace();
        long usedBytes = totalBytes - freeBytes;
        int usedPercentage = totalBytes > 0 ? (int) Math.round(((double) usedBytes / totalBytes) * 100) : 0;
        return ResponseEntity.ok(Map.of(
            "drive", "C:\\",
            "totalBytes", totalBytes,
            "usedBytes", usedBytes,
            "freeBytes", freeBytes,
            "usedPercentage", usedPercentage
        ));
    }
}
