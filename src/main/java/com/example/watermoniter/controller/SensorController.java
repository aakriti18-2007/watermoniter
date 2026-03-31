package com.example.watermoniter.controller;

import com.example.watermoniter.model.SensorDevice;
import com.example.watermoniter.model.WaterData;
import com.example.watermoniter.service.SensorDeviceService;
import com.example.watermoniter.service.WaterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorDeviceService sensorDeviceService;
    private final WaterService waterService;

    public SensorController(SensorDeviceService sensorDeviceService, WaterService waterService) {
        this.sensorDeviceService = sensorDeviceService;
        this.waterService = waterService;
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> listDevices() {
        return sensorDeviceService.listDevices().stream()
                .map(device -> {
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("id", device.getId());
                    response.put("deviceName", device.getDeviceName());
                    response.put("deviceKey", device.getDeviceKey());
                    response.put("location", device.getLocation());
                    response.put("status", device.getStatus());
                    response.put("lastSeenAt", device.getLastSeenAt());
                    return response;
                })
                .toList();
    }

    @PostMapping("/devices")
    public Map<String, Object> createDevice(@RequestBody Map<String, String> request) {
        SensorDevice device = sensorDeviceService.createDevice(
                request.getOrDefault("deviceName", "New Sensor Device"),
                request.getOrDefault("location", "Unknown location")
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", device.getId());
        response.put("deviceName", device.getDeviceName());
        response.put("deviceKey", device.getDeviceKey());
        response.put("location", device.getLocation());
        response.put("status", device.getStatus());
        return response;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> ingestReading(@RequestHeader("X-DEVICE-KEY") String deviceKey, @RequestBody WaterData data) {
        try {
            SensorDevice device = sensorDeviceService.authenticate(deviceKey);
            sensorDeviceService.touch(device);

            data.setSource("iot-device");
            data.setDeviceName(device.getDeviceName());
            return ResponseEntity.ok(waterService.updateData(data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
        }
    }
}
