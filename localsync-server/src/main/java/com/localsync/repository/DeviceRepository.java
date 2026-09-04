package com.localsync.repository;

import com.localsync.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    Optional<Device> findByDeviceIdAndTrustedTrue(String deviceId);
    Optional<Device> findByDeviceTokenAndTrustedTrue(String deviceToken);
    boolean existsByDeviceId(String deviceId);
}
