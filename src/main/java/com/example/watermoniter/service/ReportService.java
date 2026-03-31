package com.example.watermoniter.service;

import com.example.watermoniter.model.WaterData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WaterService waterService;
    private final AlertService alertService;
    private final SensorDeviceService sensorDeviceService;

    public ReportService(WaterService waterService, AlertService alertService, SensorDeviceService sensorDeviceService) {
        this.waterService = waterService;
        this.alertService = alertService;
        this.sensorDeviceService = sensorDeviceService;
    }

    public Map<String, Object> summaryReport() {
        WaterData current = waterService.getCurrentData();
        Map<String, Object> insights = waterService.getInsights();
        Map<String, Object> alert = waterService.getAlertSummary();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", LocalDateTime.now());
        report.put("currentLevel", current.getLevel());
        report.put("currentPh", current.getPh());
        report.put("currentTemp", current.getTemp());
        report.put("alertStatus", alert.get("status"));
        report.put("qualityScore", insights.get("qualityScore"));
        report.put("predictedLevelInNextHour", insights.get("predictedLevelInNextHour"));
        report.put("registeredDevices", sensorDeviceService.listDevices().size());
        report.put("recentAlerts", alertService.recentAlerts().size());
        return report;
    }

    public String exportCsv() {
        List<WaterData> history = waterService.getHistory();
        StringBuilder csv = new StringBuilder();
        csv.append("recordedAt,level,ph,temp,source,deviceName\n");
        for (WaterData item : history) {
            csv.append(FORMATTER.format(item.getRecordedAt())).append(",")
                    .append(item.getLevel()).append(",")
                    .append(item.getPh()).append(",")
                    .append(item.getTemp()).append(",")
                    .append(safe(item.getSource())).append(",")
                    .append(safe(item.getDeviceName()))
                    .append("\n");
        }
        return csv.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }
}
