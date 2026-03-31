package com.example.watermoniter.service;

import com.example.watermoniter.model.WaterData;
import com.example.watermoniter.repository.WaterDataRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WaterService {

    private static final int LOW_LEVEL_THRESHOLD = 30;
    private static final int CRITICAL_LEVEL_THRESHOLD = 15;
    private static final double MIN_SAFE_PH = 6.5;
    private static final double MAX_SAFE_PH = 8.5;
    private static final double HIGH_TEMP_THRESHOLD = 32.0;

    private final WaterDataRepository waterDataRepository;
    private final AlertService alertService;

    public WaterService(WaterDataRepository waterDataRepository, AlertService alertService) {
        this.waterDataRepository = waterDataRepository;
        this.alertService = alertService;
    }

    @PostConstruct
    public void seedIfEmpty() {
        if (waterDataRepository.count() > 0) {
            return;
        }

        waterDataRepository.save(normalize(new WaterData(78, 7.1, 25.0, LocalDateTime.now().minusMinutes(10))));
        waterDataRepository.save(normalize(new WaterData(74, 7.0, 25.6, LocalDateTime.now().minusMinutes(6))));
        waterDataRepository.save(normalize(new WaterData(69, 6.9, 26.3, LocalDateTime.now().minusMinutes(3))));
    }

    public WaterData getCurrentData() {
        WaterData latest = waterDataRepository.findTopByOrderByRecordedAtDesc();
        if (latest != null) {
            return latest;
        }

        WaterData fallback = normalize(new WaterData(75, 7.0, 25.0, LocalDateTime.now()));
        return waterDataRepository.save(fallback);
    }

    public List<WaterData> getHistory() {
        return waterDataRepository.findTop20ByOrderByRecordedAtAsc();
    }

    public WaterData updateData(WaterData data) {
        WaterData normalized = normalize(data);
        WaterData saved = waterDataRepository.save(normalized);
        alertService.evaluateAndCreateAlert(saved, buildAlertSummary(saved, getHistory()));
        return saved;
    }

    public Map<String, Object> getAlertSummary() {
        WaterData currentData = getCurrentData();
        return buildAlertSummary(currentData, getHistory());
    }

    public Map<String, Object> getInsights() {
        WaterData currentData = getCurrentData();
        List<WaterData> history = getHistory();

        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("qualityScore", calculateQualityScore(currentData));
        insights.put("consumptionTrend", calculateTrend(history));
        insights.put("predictedLevelInNextHour", predictNextLevel(history, currentData));
        insights.put("recommendedAction", buildRecommendation(currentData, history));
        insights.put("lastUpdated", currentData.getRecordedAt());
        return insights;
    }

    private WaterData normalize(WaterData data) {
        LocalDateTime recordedAt = data.getRecordedAt() == null ? LocalDateTime.now() : data.getRecordedAt();

        WaterData normalized = new WaterData();
        normalized.setLevel(clamp(data.getLevel(), 0, 100));
        normalized.setPh(round(clampDouble(data.getPh(), 0, 14)));
        normalized.setTemp(round(clampDouble(data.getTemp(), 0, 100)));
        normalized.setRecordedAt(recordedAt);
        normalized.setSource(data.getSource() == null || data.getSource().isBlank() ? "portal" : data.getSource());
        normalized.setDeviceName(data.getDeviceName() == null || data.getDeviceName().isBlank() ? "Manual Portal Input" : data.getDeviceName());
        return normalized;
    }

    private Map<String, Object> buildAlertSummary(WaterData currentData, List<WaterData> history) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", buildStatus(currentData));
        summary.put("severity", buildSeverity(currentData));
        summary.put("message", buildAlertMessage(currentData));
        summary.put("trend", calculateTrend(history));
        return summary;
    }

    private String buildStatus(WaterData data) {
        if (data.getLevel() <= CRITICAL_LEVEL_THRESHOLD) {
            return "CRITICAL";
        }
        if (data.getLevel() <= LOW_LEVEL_THRESHOLD || !isSafePh(data) || data.getTemp() >= HIGH_TEMP_THRESHOLD) {
            return "WARNING";
        }
        return "STABLE";
    }

    private String buildSeverity(WaterData data) {
        return switch (buildStatus(data)) {
            case "CRITICAL" -> "high";
            case "WARNING" -> "medium";
            default -> "low";
        };
    }

    private String buildAlertMessage(WaterData data) {
        List<String> issues = new ArrayList<>();

        if (data.getLevel() <= CRITICAL_LEVEL_THRESHOLD) {
            issues.add("Water reserve is critically low");
        } else if (data.getLevel() <= LOW_LEVEL_THRESHOLD) {
            issues.add("Water level is below the safe operating target");
        }

        if (!isSafePh(data)) {
            issues.add("pH is outside the healthy range");
        }

        if (data.getTemp() >= HIGH_TEMP_THRESHOLD) {
            issues.add("Temperature is rising above the recommended band");
        }

        if (issues.isEmpty()) {
            return "System is operating within the expected range.";
        }

        return String.join(". ", issues) + ".";
    }

    private String calculateTrend(List<WaterData> history) {
        if (history.size() < 2) {
            return "steady";
        }

        WaterData latest = history.get(history.size() - 1);
        WaterData previous = history.get(history.size() - 2);
        int delta = latest.getLevel() - previous.getLevel();

        if (delta >= 5) {
            return "rising";
        }
        if (delta <= -5) {
            return "falling";
        }
        return "steady";
    }

    private int predictNextLevel(List<WaterData> history, WaterData currentData) {
        if (history.size() < 2) {
            return currentData.getLevel();
        }

        WaterData latest = history.get(history.size() - 1);
        WaterData previous = history.get(history.size() - 2);
        int projected = latest.getLevel() + (latest.getLevel() - previous.getLevel());
        return clamp(projected, 0, 100);
    }

    private int calculateQualityScore(WaterData data) {
        double score = 100;

        if (data.getLevel() < LOW_LEVEL_THRESHOLD) {
            score -= 30;
        }
        if (data.getLevel() < CRITICAL_LEVEL_THRESHOLD) {
            score -= 20;
        }
        if (!isSafePh(data)) {
            score -= Math.min(25, Math.abs(7.0 - data.getPh()) * 10);
        }
        if (data.getTemp() >= HIGH_TEMP_THRESHOLD) {
            score -= Math.min(20, (data.getTemp() - HIGH_TEMP_THRESHOLD) * 3);
        }

        return (int) Math.max(0, Math.round(score));
    }

    private String buildRecommendation(WaterData data, List<WaterData> history) {
        if (data.getLevel() <= CRITICAL_LEVEL_THRESHOLD) {
            return "Refill the tank immediately and inspect inlet flow.";
        }
        if (!isSafePh(data)) {
            return "Run a water quality check and rebalance treatment chemicals.";
        }
        if (data.getTemp() >= HIGH_TEMP_THRESHOLD) {
            return "Inspect the tank environment and cooling exposure.";
        }
        if ("falling".equals(calculateTrend(history))) {
            return "Monitor consumption closely for the next hour.";
        }
        return "No urgent action needed. Continue routine monitoring.";
    }

    private boolean isSafePh(WaterData data) {
        return data.getPh() >= MIN_SAFE_PH && data.getPh() <= MAX_SAFE_PH;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
