package com.example.watermoniter.service;

import com.example.watermoniter.model.AlertEvent;
import com.example.watermoniter.model.WaterData;
import com.example.watermoniter.repository.AlertEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertService {

    private final AlertEventRepository alertEventRepository;
    private final String notificationChannels;
    private final NotificationService notificationService;

    public AlertService(AlertEventRepository alertEventRepository,
                        NotificationService notificationService,
                        @Value("${alerts.channels:email,sms,whatsapp}") String notificationChannels) {
        this.alertEventRepository = alertEventRepository;
        this.notificationService = notificationService;
        this.notificationChannels = notificationChannels;
    }

    public void evaluateAndCreateAlert(WaterData data, Map<String, Object> alertSummary) {
        String severity = String.valueOf(alertSummary.get("severity"));
        if ("low".equalsIgnoreCase(severity)) {
            return;
        }

        AlertEvent saved = alertEventRepository.save(new AlertEvent(
                severity,
                String.valueOf(alertSummary.get("status")),
                notificationChannels,
                LocalDateTime.now(),
                String.valueOf(alertSummary.get("message"))
        ));

        List<String> delivered = notificationService.notify(saved);
        saved.setChannels(delivered.isEmpty() ? "logged-only" : String.join(",", delivered));
        alertEventRepository.save(saved);
    }

    public List<AlertEvent> recentAlerts() {
        return alertEventRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public Map<String, Object> alertStatusBoard() {
        List<AlertEvent> alerts = recentAlerts();
        Map<String, Object> board = new LinkedHashMap<>();
        board.put("totalAlerts", alerts.size());
        board.put("channels", notificationChannels);
        board.put("latestSeverity", alerts.isEmpty() ? "none" : alerts.get(0).getSeverity());
        return board;
    }
}
