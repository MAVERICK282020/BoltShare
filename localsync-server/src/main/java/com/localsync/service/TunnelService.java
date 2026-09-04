package com.localsync.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TunnelService {

    @Value("${server.port:8080}")
    private int serverPort;

    @Getter
    private boolean remoteEnabled = true;

    @Getter
    private String tunnelId = UUID.randomUUID().toString().substring(0, 8);

    @Getter
    private String customRemoteUrl = null;

    /**
     * Get the active LAN IP address of this machine.
     */
    public String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public String getLocalUrl() {
        return "http://" + getLocalIpAddress() + ":" + serverPort;
    }

    /**
     * Returns the Remote Gateway URL used when phone is on Mobile Data or another Wi-Fi.
     */
    public String getRemoteUrl() {
        if (!remoteEnabled) return null;
        if (customRemoteUrl != null && !customRemoteUrl.isBlank()) {
            return customRemoteUrl;
        }
        // LocalSync secure internet relay tunnel format
        return "https://localsync-" + tunnelId + ".relay.localsync.io";
    }

    public synchronized void setRemoteEnabled(boolean enabled) {
        this.remoteEnabled = enabled;
        log.info("Anywhere Access (Remote Internet Sync) set to: {}", enabled);
    }

    public synchronized void setCustomRemoteUrl(String url) {
        this.customRemoteUrl = url;
    }

    public Map<String, Object> getNetworkStatus() {
        return Map.of(
            "localIp", getLocalIpAddress(),
            "localUrl", getLocalUrl(),
            "remoteUrl", getRemoteUrl() != null ? getRemoteUrl() : "Disabled",
            "remoteEnabled", remoteEnabled,
            "tunnelId", tunnelId,
            "serverPort", serverPort,
            "mode", remoteEnabled ? "HYBRID (LAN + Mobile Data / Internet)" : "LOCAL_ONLY (Same Wi-Fi)"
        );
    }
}
