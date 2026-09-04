package com.localsync.security;

import com.localsync.model.Device;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DevicePrincipal {
    private final String deviceId;
    private final String deviceName;
    private final Device device;
}
