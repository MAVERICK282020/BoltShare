package com.localsync.service;

import com.localsync.model.TransferJob;
import com.localsync.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferRepository transferRepository;
    private final FileManagerService fileManagerService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${localsync.transfer.chunk-dir:./transfer-chunks}")
    private String chunkBaseDir;

    private static final long CHUNK_SIZE = 10 * 1024 * 1024L; // 10 MB

    /**
     * Initialize a new chunked transfer job.
     */
    @Transactional
    public synchronized Map<String, Object> initTransfer(String deviceId, String fileName,
                                             String targetPath, long totalSize) throws IOException {
        // If targetPath points to Phone Storage or is default, map to device's PhoneStorage directory
        if (targetPath == null || targetPath.isBlank() || targetPath.equalsIgnoreCase("default") || targetPath.startsWith("Phone Storage")) {
            String sub = (targetPath != null && targetPath.contains("/")) ? targetPath.substring(targetPath.indexOf("/") + 1).trim() : "Downloads";
            targetPath = System.getProperty("user.home") + "/LocalSyncPhoneStorage/" + deviceId + "/" + sub;
            Files.createDirectories(Path.of(targetPath));
        }

        // Validate target path
        fileManagerService.validateAndResolvePath(targetPath);

        String jobId = UUID.randomUUID().toString();
        int totalChunks = (int) Math.ceil((double) totalSize / CHUNK_SIZE);
        String tempDir = chunkBaseDir + "/" + jobId;
        Files.createDirectories(Path.of(tempDir));

        TransferJob job = TransferJob.builder()
            .jobId(jobId)
            .deviceId(deviceId)
            .fileName(fileName)
            .targetPath(targetPath)
            .totalSize(totalSize)
            .totalChunks(totalChunks)
            .lastChunkReceived(-1)
            .tempDir(tempDir)
            .status(TransferJob.TransferStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();

        transferRepository.save(job);

        log.info("Transfer initialized: {} ({} chunks, {} bytes)", jobId, totalChunks, totalSize);
        return Map.of(
            "jobId", jobId,
            "chunkSize", CHUNK_SIZE,
            "totalChunks", totalChunks
        );
    }

    /**
     * Receive and store a single chunk.
     */
    @Transactional
    public Map<String, Object> receiveChunk(String jobId, int chunkIndex, MultipartFile chunkData) throws IOException {
        TransferJob job = transferRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Transfer job not found: " + jobId));

        if (job.getStatus() == TransferJob.TransferStatus.CANCELLED ||
            job.getStatus() == TransferJob.TransferStatus.FAILED) {
            throw new RuntimeException("Transfer job is not active: " + job.getStatus());
        }

        // Write chunk to temp dir
        Path chunkFile = Path.of(job.getTempDir(), String.format("chunk_%05d", chunkIndex));
        try (OutputStream out = Files.newOutputStream(chunkFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            chunkData.getInputStream().transferTo(out);
        }

        // Update job state
        job.setLastChunkReceived(chunkIndex);
        job.setStatus(TransferJob.TransferStatus.IN_PROGRESS);
        transferRepository.save(job);

        // Broadcast progress via WebSocket
        double progress = ((double)(chunkIndex + 1) / job.getTotalChunks()) * 100;
        messagingTemplate.convertAndSend("/topic/transfer/" + jobId, Map.of(
            "jobId", jobId,
            "chunkIndex", chunkIndex,
            "totalChunks", job.getTotalChunks(),
            "progress", Math.min(progress, 100.0),
            "bytesReceived", (long)(chunkIndex + 1) * CHUNK_SIZE
        ));

        log.debug("Chunk {}/{} received for job {}", chunkIndex + 1, job.getTotalChunks(), jobId);
        return Map.of("chunkIndex", chunkIndex, "progress", progress);
    }

    /**
     * Get transfer status for resume support.
     * Returns the last chunk received so client can resume from next chunk.
     */
    public Map<String, Object> getTransferStatus(String jobId) {
        TransferJob job = transferRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Transfer job not found: " + jobId));
        return Map.of(
            "jobId", jobId,
            "status", job.getStatus().name(),
            "lastChunkReceived", job.getLastChunkReceived(),
            "totalChunks", job.getTotalChunks(),
            "resumeFromChunk", job.getLastChunkReceived() + 1
        );
    }

    /**
     * Assemble all chunks into the final file.
     */
    @Transactional
    public Map<String, Object> completeTransfer(String jobId) throws IOException {
        TransferJob job = transferRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Transfer job not found: " + jobId));

        Path targetDir = fileManagerService.validateAndResolvePath(job.getTargetPath());
        Path finalFile = targetDir.resolve(job.getFileName());

        // Assemble chunks in order
        log.info("Assembling {} chunks for job {}", job.getTotalChunks(), jobId);
        try (OutputStream out = Files.newOutputStream(finalFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < job.getTotalChunks(); i++) {
                Path chunkFile = Path.of(job.getTempDir(), String.format("chunk_%05d", i));
                if (!Files.exists(chunkFile)) {
                    throw new IOException("Missing chunk " + i + " for job " + jobId);
                }
                Files.copy(chunkFile, out);
            }
        }

        // Cleanup temp chunks
        FileUtils.deleteDirectory(new File(job.getTempDir()));

        // Update job status
        job.setStatus(TransferJob.TransferStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        transferRepository.save(job);

        // Final WebSocket notification
        messagingTemplate.convertAndSend("/topic/transfer/" + jobId, Map.of(
            "jobId", jobId,
            "status", "COMPLETED",
            "progress", 100.0,
            "filePath", finalFile.toString()
        ));

        log.info("Transfer completed: {} -> {}", jobId, finalFile);
        return Map.of("status", "COMPLETED", "filePath", finalFile.toString());
    }

    /**
     * Cancel a transfer and cleanup temp files.
     */
    @Transactional
    public void cancelTransfer(String jobId) throws IOException {
        TransferJob job = transferRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Transfer job not found: " + jobId));
        job.setStatus(TransferJob.TransferStatus.CANCELLED);
        transferRepository.save(job);
        FileUtils.deleteDirectory(new File(job.getTempDir()));
        log.info("Transfer cancelled: {}", jobId);
    }

    /**
     * Clear all completed, failed, and cancelled transfers from history.
     */
    @Transactional
    public int clearHistory() {
        var list = transferRepository.findAll().stream()
            .filter(t -> t.getStatus() == TransferJob.TransferStatus.COMPLETED 
                      || t.getStatus() == TransferJob.TransferStatus.CANCELLED 
                      || t.getStatus() == TransferJob.TransferStatus.FAILED)
            .toList();
        transferRepository.deleteAll(list);
        log.info("Cleared {} completed/cancelled transfer history items", list.size());
        return list.size();
    }
}
