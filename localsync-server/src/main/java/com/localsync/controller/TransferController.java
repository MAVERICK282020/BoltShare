package com.localsync.controller;

import com.localsync.dto.FileInfoDto;
import com.localsync.model.Device;
import com.localsync.model.TransferJob;
import com.localsync.repository.DeviceRepository;
import com.localsync.repository.TransferRepository;
import com.localsync.security.DevicePrincipal;
import com.localsync.service.PhoneStorageService;
import com.localsync.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;
    private final TransferRepository transferRepository;
    private final DeviceRepository deviceRepository;
    private final PhoneStorageService phoneStorageService;

    private String resolveDeviceId(DevicePrincipal principal) {
        if (principal != null && principal.getDeviceId() != null && !principal.getDeviceId().isBlank()) {
            return principal.getDeviceId();
        }
        return deviceRepository.findAll().stream()
            .filter(Device::isTrusted)
            .map(Device::getDeviceId)
            .findFirst()
            .orElse("default");
    }

    /**
     * Get all transfer jobs (for Web UI & Phone listing).
     * GET /api/transfer/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<TransferJob>> getAllTransfers() {
        return ResponseEntity.ok(transferRepository.findAllByOrderByCreatedAtDesc());
    }

    /**
     * Initialize a new chunked transfer. Accepts both Query Params and Request Body.
     * POST /api/transfer/init
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initTransfer(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) Long totalSize,
            @RequestParam(required = false) String targetDir,
            @RequestParam(required = false) String targetPath,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal DevicePrincipal principal) {
        try {
            String resolvedFileName = fileName;
            Long resolvedSize = totalSize;
            String resolvedPath = targetPath != null ? targetPath : targetDir;

            if (body != null) {
                if (resolvedFileName == null && body.containsKey("fileName")) {
                    resolvedFileName = (String) body.get("fileName");
                }
                if (resolvedSize == null && body.containsKey("totalSize")) {
                    resolvedSize = Long.parseLong(body.get("totalSize").toString());
                }
                if (resolvedPath == null) {
                    if (body.containsKey("targetPath")) resolvedPath = (String) body.get("targetPath");
                    else if (body.containsKey("targetDir")) resolvedPath = (String) body.get("targetDir");
                }
            }

            if (resolvedFileName == null) resolvedFileName = "upload_" + System.currentTimeMillis();
            if (resolvedSize == null) resolvedSize = 0L;
            if (resolvedPath == null) resolvedPath = "default";

            String deviceId = resolveDeviceId(principal);
            Map<String, Object> result = transferService.initTransfer(
                deviceId, resolvedFileName, resolvedPath, resolvedSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Transfer init failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Upload a single chunk. Accepts chunk index from header or query param.
     * POST /api/transfer/chunk/{jobId}
     */
    @PostMapping("/chunk/{jobId}")
    public ResponseEntity<Map<String, Object>> receiveChunk(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Chunk-Index", required = false) Integer headerChunkIndex,
            @RequestParam(value = "chunkIndex", required = false) Integer paramChunkIndex,
            @RequestParam("chunk") MultipartFile chunkData) {
        try {
            int chunkIndex = headerChunkIndex != null ? headerChunkIndex : (paramChunkIndex != null ? paramChunkIndex : 0);
            Map<String, Object> result = transferService.receiveChunk(jobId, chunkIndex, chunkData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Chunk upload failed for job {}: {}", jobId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get transfer status.
     * GET /api/transfer/status/{jobId}
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(transferService.getTransferStatus(jobId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Complete transfer - assemble all chunks.
     * POST /api/transfer/complete/{jobId}
     */
    @PostMapping("/complete/{jobId}")
    public ResponseEntity<Map<String, Object>> completeTransfer(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(transferService.completeTransfer(jobId));
        } catch (Exception e) {
            log.error("Transfer completion failed for job {}: {}", jobId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cancel a transfer.
     * DELETE /api/transfer/{jobId}
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Map<String, String>> cancelTransfer(@PathVariable String jobId) {
        try {
            transferService.cancelTransfer(jobId);
            return ResponseEntity.ok(Map.of("status", "cancelled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Direct Cross-Pane Drag & Drop Transfer: PC to Phone
     * POST /api/transfer/pc-to-phone?sourcePath=...&targetDir=...
     */
    @PostMapping("/pc-to-phone")
    public ResponseEntity<TransferJob> transferPcToPhone(
            @RequestParam String sourcePath,
            @RequestParam(required = false) String targetDir) {
        try {
            Path file = Paths.get(sourcePath).normalize().toAbsolutePath();
            if (!Files.isRegularFile(file)) {
                return ResponseEntity.badRequest().build();
            }

            String jobId = UUID.randomUUID().toString();
            long size = Files.size(file);
            String fileName = file.getFileName().toString();
            String destination = (targetDir != null && !targetDir.isBlank()) ? targetDir : "/storage/emulated/0/Download";

            TransferJob job = TransferJob.builder()
                .jobId(jobId)
                .deviceId(resolveDeviceId(null))
                .fileName(fileName)
                .targetPath(destination)
                .totalSize(size)
                .totalChunks(1)
                .lastChunkReceived(0)
                .status(TransferJob.TransferStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();
            transferRepository.save(job);

            // Execute streaming in background thread
            new Thread(() -> {
                try {
                    phoneStorageService.saveLocalFileToPhoneStorage(job.getDeviceId(), destination, file);
                    job.setStatus(TransferJob.TransferStatus.COMPLETED);
                    transferRepository.save(job);
                } catch (Exception e) {
                    log.error("Background PC to Phone transfer failed", e);
                    job.setStatus(TransferJob.TransferStatus.FAILED);
                    transferRepository.save(job);
                }
            }).start();

            return ResponseEntity.ok(job);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Direct Cross-Pane Drag & Drop Transfer: Phone to PC
     * POST /api/transfer/phone-to-pc?sourcePath=...&targetDir=...
     */
    @PostMapping("/phone-to-pc")
    public ResponseEntity<TransferJob> transferPhoneToPc(
            @RequestParam String sourcePath,
            @RequestParam(required = false) String targetDir) {
        try {
            String deviceId = resolveDeviceId(null);
            String fileName = sourcePath.contains("/") ? sourcePath.substring(sourcePath.lastIndexOf('/') + 1) : "file";
            Path destDir = targetDir != null && !targetDir.isBlank() ? Paths.get(targetDir) : Paths.get(System.getProperty("user.home"), "Downloads");
            Files.createDirectories(destDir);
            Path destFile = destDir.resolve(fileName);

            String jobId = UUID.randomUUID().toString();
            TransferJob job = TransferJob.builder()
                .jobId(jobId)
                .deviceId(deviceId)
                .fileName(fileName)
                .targetPath(destFile.toString().replace('\\', '/'))
                .totalSize(0)
                .totalChunks(1)
                .lastChunkReceived(0)
                .status(TransferJob.TransferStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();
            transferRepository.save(job);

            // Stream file from phone to PC disk
            new Thread(() -> {
                try {
                    HttpResponse<InputStream> resp = phoneStorageService.fetchPhoneFileResponse(deviceId, sourcePath, false);
                    if (resp != null && resp.statusCode() == 200) {
                        Files.copy(resp.body(), destFile, StandardCopyOption.REPLACE_EXISTING);
                        job.setTotalSize(Files.size(destFile));
                        job.setStatus(TransferJob.TransferStatus.COMPLETED);
                    } else {
                        job.setStatus(TransferJob.TransferStatus.FAILED);
                    }
                    transferRepository.save(job);
                } catch (Exception e) {
                    log.error("Phone to PC background transfer failed", e);
                    job.setStatus(TransferJob.TransferStatus.FAILED);
                    transferRepository.save(job);
                }
            }).start();

            return ResponseEntity.ok(job);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Clear all completed and cancelled transfer jobs from history.
     * DELETE /api/transfer/history
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearTransferHistory() {
        int count = transferService.clearHistory();
        return ResponseEntity.ok(Map.of("status", "cleared", "deletedCount", count));
    }
}
