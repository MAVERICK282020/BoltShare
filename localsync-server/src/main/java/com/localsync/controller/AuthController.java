package com.localsync.controller;

import com.localsync.model.Device;
import com.localsync.repository.DeviceRepository;
import com.localsync.repository.SessionRepository;
import com.localsync.service.PairingService;
import com.localsync.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final PairingService pairingService;
    private final SessionService sessionService;
    private final DeviceRepository deviceRepository;
    private final com.localsync.service.TunnelService tunnelService;
    private final PhoneStorageController phoneStorageController;

    /**
     * Generate QR code for pairing.
     * GET /api/auth/generate-qr
     */
    @GetMapping("/generate-qr")
    public ResponseEntity<Map<String, String>> generateQr() {
        try {
            Map<String, String> result = pairingService.generateQrCode();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("QR generation failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Pair a new device (called by phone after scanning QR).
     * POST /api/auth/pair
     */
    @PostMapping("/pair")
    public ResponseEntity<Map<String, String>> pairDevice(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String token = (String) body.get("token");
            String hmac = (String) (body.get("sig") != null ? body.get("sig") : body.get("signature"));
            Object expObj = body.get("exp") != null ? body.get("exp") : body.get("expiresAt");
            if (token == null || hmac == null || expObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing token, sig, or exp"));
            }
            long expiry = Long.parseLong(expObj.toString());
            String deviceName = (String) body.getOrDefault("deviceName", "Unknown Device");
            String deviceType = (String) body.getOrDefault("deviceType", "android");
            String deviceIp = request.getRemoteAddr();

            Map<String, String> result = pairingService.pairDevice(token, hmac, expiry, deviceName, deviceType, deviceIp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Pairing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Auto-reconnect trusted device using stored deviceToken.
     * Zero QR scans required after first pairing!
     * POST /api/auth/reconnect
     */
    @PostMapping("/reconnect")
    public ResponseEntity<Map<String, String>> reconnectDevice(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String deviceToken = body.get("deviceToken");
            if (deviceToken == null || deviceToken.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "deviceToken is required"));
            }
            String clientIp = request.getRemoteAddr();
            Map<String, String> result = pairingService.reconnect(deviceToken, clientIp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Auto-reconnection failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * KILL SWITCH - revoke all active sessions.
     * POST /api/auth/revoke-all
     */
    @PostMapping("/revoke-all")
    public ResponseEntity<Map<String, String>> revokeAll() {
        sessionService.revokeAllSessions();
        return ResponseEntity.ok(Map.of("status", "All sessions revoked"));
    }

    /**
     * List all trusted devices.
     * GET /api/auth/devices
     */
    @GetMapping("/devices")
    public ResponseEntity<List<Device>> listDevices() {
        List<Device> devices = deviceRepository.findAll().stream()
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
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(devices);
    }

    /**
     * Unpair a specific device.
     * DELETE /api/auth/devices/{deviceId}
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<Map<String, String>> unpairDevice(@PathVariable String deviceId) {
        sessionService.unpairDevice(deviceId);
        try {
            deviceRepository.deleteById(deviceId);
        } catch (Exception ignored) {}
        phoneStorageController.clearCacheForDevice(deviceId);
        return ResponseEntity.ok(Map.of("status", "Device unpaired"));
    }

    /**
     * Purge all untrusted and stale test devices.
     * POST /api/auth/devices/cleanup
     */
    @PostMapping("/devices/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupDevices() {
        List<Device> all = deviceRepository.findAll();
        // Keep ONLY the latest trusted active mobile device if available
        Device activePhone = all.stream()
            .filter(Device::isTrusted)
            .sorted((a, b) -> {
                if (a.getLastSeen() != null && b.getLastSeen() != null) return b.getLastSeen().compareTo(a.getLastSeen());
                if (a.getPairedAt() != null && b.getPairedAt() != null) return b.getPairedAt().compareTo(a.getPairedAt());
                return 0;
            })
            .findFirst()
            .orElse(null);

        int deleted = 0;
        for (Device d : all) {
            if (activePhone != null && d.getDeviceId().equals(activePhone.getDeviceId())) {
                continue; // Keep the active phone
            }
            try {
                sessionService.unpairDevice(d.getDeviceId());
                deviceRepository.delete(d);
                phoneStorageController.clearCacheForDevice(d.getDeviceId());
                deleted++;
            } catch (Exception e) {
                log.warn("Failed to delete stale device: {}", d.getDeviceId());
            }
        }
        return ResponseEntity.ok(Map.of(
            "deleted", deleted, 
            "activeDevice", activePhone != null ? activePhone.getDeviceName() : "None",
            "activeDeviceId", activePhone != null ? activePhone.getDeviceId() : "None"
        ));
    }

    /**
     * Get dual network status (LAN Direct vs Remote Internet Gateway).
     * GET /api/auth/network-status
     */
    @GetMapping("/network-status")
    public ResponseEntity<Map<String, Object>> getNetworkStatus() {
        return ResponseEntity.ok(tunnelService.getNetworkStatus());
    }

    /**
     * Toggle Anywhere Access (Remote Internet Gateway) ON/OFF.
     * POST /api/auth/toggle-remote
     */
    @PostMapping("/toggle-remote")
    public ResponseEntity<Map<String, Object>> toggleRemote(@RequestBody(required = false) Map<String, Object> body) {
        boolean enable = true;
        if (body != null && body.containsKey("enabled")) {
            enable = Boolean.parseBoolean(body.get("enabled").toString());
        } else {
            enable = !tunnelService.isRemoteEnabled();
        }
        tunnelService.setRemoteEnabled(enable);
        return ResponseEntity.ok(tunnelService.getNetworkStatus());
    }

    /**
     * Set a custom tunnel or relay endpoint (e.g., Cloudflare Tunnel or ngrok).
     * POST /api/auth/custom-remote-url
     */
    @PostMapping("/custom-remote-url")
    public ResponseEntity<Map<String, Object>> setCustomRemoteUrl(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        tunnelService.setCustomRemoteUrl(url);
        return ResponseEntity.ok(tunnelService.getNetworkStatus());
    }

    /**
     * Health check endpoint.
     * GET /api/health
     */
    @GetMapping("/../../health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "LocalSync",
            "version", "1.0.0"
        ));
    }
}
