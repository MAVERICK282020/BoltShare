package com.localsync.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferJob {

    @Id
    @Column(name = "job_id")
    private String jobId;              // UUID

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "target_path", nullable = false)
    private String targetPath;

    @Column(name = "total_size")
    private long totalSize;

    @Column(name = "total_chunks")
    private int totalChunks;

    @Column(name = "last_chunk_received")
    private int lastChunkReceived;     // 0-based, -1 = none received yet

    @Column(name = "temp_dir")
    private String tempDir;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransferStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum TransferStatus {
        PENDING, IN_PROGRESS, PAUSED, COMPLETED, FAILED, CANCELLED
    }
}
