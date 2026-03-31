package com.example.watermoniter.repository;

import com.example.watermoniter.model.SensorDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SensorDeviceRepository extends JpaRepository<SensorDevice, Long> {

    Optional<SensorDevice> findByDeviceKey(String deviceKey);
}
