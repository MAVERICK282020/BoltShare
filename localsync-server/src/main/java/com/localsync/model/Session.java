package com.localsync.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @Column(name = "session_id")
    private String sessionId;          // JWT ID (jti claim)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Device device;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;   // null = permanent

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
