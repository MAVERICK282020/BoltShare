package com.localsync.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @Column(name = "device_id")
    private String deviceId;           // UUID

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "device_type")
    private String deviceType;         // "android", "ios", "web"

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "paired_at")
    private LocalDateTime pairedAt;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "is_trusted")
    private boolean trusted;

    @Column(name = "fingerprint")
    private String fingerprint;        // Device fingerprint for extra validation

    @Column(name = "device_token", unique = true)
    private String deviceToken;        // Long-lived refresh token (1 year) stored on phone

    @Column(name = "last_seen_ip")
    private String lastSeenIp;         // Last IP seen from

    @Column(name = "device_token_expiry")
    private LocalDateTime deviceTokenExpiry; // When long-lived token expires

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Permission> permissions;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Session> sessions;
}
