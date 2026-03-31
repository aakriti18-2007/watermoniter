package com.example.watermoniter.service;

import com.example.watermoniter.model.SensorDevice;
import com.example.watermoniter.repository.SensorDeviceRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SensorDeviceService {

    private final SensorDeviceRepository sensorDeviceRepository;

    public SensorDeviceService(SensorDeviceRepository sensorDeviceRepository) {
        this.sensorDeviceRepository = sensorDeviceRepository;
    }

    @PostConstruct
    public void seedDevices() {
        if (sensorDeviceRepository.count() > 0) {
            return;
        }

        sensorDeviceRepository.save(new SensorDevice(
                "Main Tank Sensor",
                "DEVICE-MAIN-001",
                "Rooftop Tank",
                "ONLINE",
                LocalDateTime.now()
        ));
    }

    public List<SensorDevice> listDevices() {
        return sensorDeviceRepository.findAll();
    }

    public SensorDevice createDevice(String deviceName, String location) {
        return sensorDeviceRepository.save(new SensorDevice(
                deviceName,
                "DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                location,
                "ONLINE",
                LocalDateTime.now()
        ));
    }

    public SensorDevice authenticate(String deviceKey) {
        return sensorDeviceRepository.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid device key"));
    }

    public SensorDevice touch(SensorDevice device) {
        device.setLastSeenAt(LocalDateTime.now());
        device.setStatus("ONLINE");
        return sensorDeviceRepository.save(device);
    }
}
