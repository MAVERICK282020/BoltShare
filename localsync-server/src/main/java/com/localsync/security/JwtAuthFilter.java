package com.localsync.security;

import com.localsync.model.Device;
import com.localsync.repository.DeviceRepository;
import com.localsync.repository.SessionRepository;
import com.localsync.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final DeviceRepository deviceRepository;
    private final SessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = null;
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (request.getParameter("token") != null && !request.getParameter("token").isBlank()) {
            token = request.getParameter("token").trim();
        }

        if (token == null || token.isBlank() || "null".equalsIgnoreCase(token) || "undefined".equalsIgnoreCase(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.isTokenValid(token)) {
                String deviceId = jwtService.extractDeviceId(token);
                String sessionId = jwtService.extractJti(token);
                String deviceName = jwtService.extractDeviceName(token);

                // Check device is still trusted
                Device device = deviceRepository.findByDeviceIdAndTrustedTrue(deviceId).orElse(null);
                if (device != null) {
                    // Check session is still active (not kill-switched)
                    boolean sessionActive = sessionRepository.findBySessionIdAndActiveTrue(sessionId).isPresent();
                    if (sessionActive) {
                        // Build authentication principal
                        DevicePrincipal principal = new DevicePrincipal(deviceId, deviceName, device);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
                        );
                        auth.setDetails(request.getRemoteAddr());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        log.warn("Session revoked for sessionId: {}", sessionId);
                    }
                } else {
                    log.warn("Device not trusted or found for deviceId: {}", deviceId);
                }
            }
        } catch (Exception e) {
            log.warn("JWT filter error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
