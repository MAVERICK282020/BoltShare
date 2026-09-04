package com.localsync.repository;

import com.localsync.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    Optional<Session> findBySessionIdAndActiveTrue(String sessionId);

    List<Session> findByDevice_DeviceIdAndActiveTrue(String deviceId);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.active = false, s.revokedAt = :now WHERE s.active = true")
    void revokeAllSessions(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.active = false, s.revokedAt = :now WHERE s.device.deviceId = :deviceId AND s.active = true")
    void revokeSessionsByDevice(String deviceId, LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.active = false, s.revokedAt = :now WHERE s.expiresAt IS NOT NULL AND s.expiresAt < :now AND s.active = true")
    void expireOldSessions(LocalDateTime now);
}
