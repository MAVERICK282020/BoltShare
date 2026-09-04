package com.localsync.repository;

import com.localsync.model.TransferJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<TransferJob, String> {
    List<TransferJob> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    List<TransferJob> findAllByOrderByCreatedAtDesc();
    List<TransferJob> findByStatus(TransferJob.TransferStatus status);
}
