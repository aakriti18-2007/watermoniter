package com.example.watermoniter;

import com.example.watermoniter.model.AppUser;
import com.example.watermoniter.model.SensorDevice;
import com.example.watermoniter.model.WaterData;
import com.example.watermoniter.service.AlertService;
import com.example.watermoniter.service.AiAgentService;
import com.example.watermoniter.service.AppUserService;
import com.example.watermoniter.service.ReportService;
import com.example.watermoniter.service.SensorDeviceService;
import com.example.watermoniter.service.WaterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ApplicationTests {

    @Autowired
    private WaterService waterService;

    @Autowired
    private AiAgentService aiAgentService;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SensorDeviceService sensorDeviceService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private ReportService reportService;

    @Test
    void contextLoads() {
        assertNotNull(waterService);
        assertNotNull(aiAgentService);
        assertNotNull(appUserService);
        assertNotNull(passwordEncoder);
        assertNotNull(sensorDeviceService);
        assertNotNull(alertService);
        assertNotNull(reportService);
    }

    @Test
    void shouldReturnSeededData() {
        WaterData current = waterService.getCurrentData();

        assertNotNull(current);
        assertTrue(current.getLevel() >= 0);
        assertTrue(current.getPh() >= 0);
        assertTrue(current.getTemp() >= 0);
        assertNotNull(current.getRecordedAt());
        assertFalse(waterService.getHistory().isEmpty());
    }

    @Test
    void shouldStoreUpdateAndReturnInsights() {
        int previousHistorySize = waterService.getHistory().size();
        WaterData request = new WaterData(12, 9.3, 35.4, LocalDateTime.now());
        WaterData updated = waterService.updateData(request);
        Map<String, Object> alert = waterService.getAlertSummary();
        Map<String, Object> insights = waterService.getInsights();

        assertEquals(12, updated.getLevel());
        assertTrue(waterService.getHistory().size() >= previousHistorySize);
        assertEquals("CRITICAL", alert.get("status"));
        assertEquals("high", alert.get("severity"));
        assertNotNull(insights.get("qualityScore"));
        assertNotNull(insights.get("recommendedAction"));
        assertNotNull(insights.get("predictedLevelInNextHour"));
    }

    @Test
    void shouldReturnAssistantOverviewWithoutApiKey() {
        Map<String, Object> overview = aiAgentService.getCopilotOverview();
        Map<String, Object> response = aiAgentService.respond("Explain the current system health");

        assertEquals("AquaMind Copilot", overview.get("agentName"));
        assertNotNull(overview.get("provider"));
        assertNotNull(overview.get("summary"));
        assertNotNull(response.get("message"));
        assertFalse(aiAgentService.getRecentMessages().isEmpty());
    }

    @Test
    void shouldSeedSecureUsersWithEncodedPasswords() {
        AppUser admin = appUserService.findDomainUser("admin");
        AppUser operator = appUserService.findDomainUser("operator");
        AppUser viewer = appUserService.findDomainUser("viewer");

        assertEquals("ADMIN", admin.getRole());
        assertEquals("OPERATOR", operator.getRole());
        assertEquals("VIEWER", viewer.getRole());
        assertTrue(passwordEncoder.matches("admin123", admin.getPassword()));
        assertTrue(passwordEncoder.matches("operator123", operator.getPassword()));
        assertTrue(passwordEncoder.matches("viewer123", viewer.getPassword()));
    }

    @Test
    void shouldCreateUpdateAndDeleteAdminManagedUser() {
        AppUser created = appUserService.createUser("testadminpanel", "test123", "VIEWER", "Test Admin Panel");
        assertEquals("VIEWER", created.getRole());
        assertTrue(passwordEncoder.matches("test123", created.getPassword()));

        AppUser updated = appUserService.updateUser(created.getId(), "Updated Test User", "OPERATOR");
        assertEquals("Updated Test User", updated.getDisplayName());
        assertEquals("OPERATOR", updated.getRole());

        AppUser reset = appUserService.resetPassword(created.getId(), "newpass123");
        assertTrue(passwordEncoder.matches("newpass123", reset.getPassword()));

        appUserService.deleteUser(created.getId(), "admin");
        assertFalse(appUserService.listUsers().stream().anyMatch(user -> "testadminpanel".equals(user.getUsername())));
    }

    @Test
    void shouldCreateDeviceAndExportReport() {
        SensorDevice device = sensorDeviceService.createDevice("Field Sensor", "Reservoir Zone");
        assertNotNull(device.getDeviceKey());
        assertTrue(sensorDeviceService.listDevices().stream().anyMatch(item -> item.getDeviceName().equals("Field Sensor")));

        WaterData reading = new WaterData(10, 9.1, 35.0, LocalDateTime.now());
        reading.setSource("iot-device");
        reading.setDeviceName(device.getDeviceName());
        waterService.updateData(reading);

        assertFalse(alertService.recentAlerts().isEmpty());
        assertTrue(reportService.exportCsv().contains("recordedAt,level,ph,temp,source,deviceName"));
        assertNotNull(reportService.summaryReport().get("registeredDevices"));
    }
}
