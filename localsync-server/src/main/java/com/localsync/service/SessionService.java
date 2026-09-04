package com.localsync.service;

import com.localsync.repository.DeviceRepository;
import com.localsync.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final DeviceRepository deviceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * KILL SWITCH: Revoke ALL active sessions immediately.
     * Broadcasts SESSION_REVOKED event via WebSocket.
     */
    @Transactional
    public void revokeAllSessions() {
        sessionRepository.revokeAllSessions(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/system", "SESSION_REVOKED");
        log.warn("🔴 KILL SWITCH ACTIVATED: All sessions revoked");
    }

    /**
     * Revoke all sessions for a specific device.
     */
    @Transactional
    public void revokeDeviceSessions(String deviceId) {
        sessionRepository.revokeSessionsByDevice(deviceId, LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/device/" + deviceId, "SESSION_REVOKED");
        log.info("Sessions revoked for device: {}", deviceId);
    }

    /**
     * Unpair (remove trust) from a device.
     */
    @Transactional
    public void unpairDevice(String deviceId) {
        revokeDeviceSessions(deviceId);
        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setTrusted(false);
            try {
                deviceRepository.delete(device);
            } catch (Exception e) {
                deviceRepository.save(device);
            }
        });
    }

    /**
     * Scheduled job: expire temporary sessions every minute.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireTemporarySessions() {
        sessionRepository.expireOldSessions(LocalDateTime.now());
    }
}
