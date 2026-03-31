package com.example.watermoniter.controller;

import com.example.watermoniter.service.AlertService;
import com.example.watermoniter.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ops")
public class OperationsController {

    private final AlertService alertService;
    private final ReportService reportService;

    public OperationsController(AlertService alertService, ReportService reportService) {
        this.alertService = alertService;
        this.reportService = reportService;
    }

    @GetMapping("/alerts")
    public Object alerts() {
        return alertService.recentAlerts();
    }

    @GetMapping("/alerts/status")
    public Map<String, Object> alertStatus() {
        return alertService.alertStatusBoard();
    }

    @GetMapping("/reports/summary")
    public Map<String, Object> summaryReport() {
        return reportService.summaryReport();
    }

    @GetMapping("/reports/export.csv")
    public ResponseEntity<String> exportCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=water-report.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(reportService.exportCsv());
    }
}
