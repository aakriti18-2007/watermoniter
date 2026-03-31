package com.example.watermoniter.controller;

import com.example.watermoniter.model.WaterData;
import com.example.watermoniter.service.AiAgentService;
import com.example.watermoniter.service.WaterService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class WaterController {

    private final WaterService waterService;
    private final AiAgentService aiAgentService;

    public WaterController(WaterService waterService, AiAgentService aiAgentService) {
        this.waterService = waterService;
        this.aiAgentService = aiAgentService;
    }

    @PostMapping("/update")
    public WaterData updateData(@RequestBody WaterData data) {
        return waterService.updateData(data);
    }

    @GetMapping("/data")
    public WaterData getData() {
        return waterService.getCurrentData();
    }

    @GetMapping("/history")
    public List<WaterData> getHistory() {
        return waterService.getHistory();
    }

    @GetMapping("/alert")
    public Map<String, Object> getAlert() {
        return waterService.getAlertSummary();
    }

    @GetMapping("/insights")
    public Map<String, Object> getInsights() {
        return waterService.getInsights();
    }

    @GetMapping("/assistant/overview")
    public Map<String, Object> getAssistantOverview() {
        return aiAgentService.getCopilotOverview();
    }

    @PostMapping("/assistant/chat")
    public Map<String, Object> chatWithAssistant(@RequestBody Map<String, String> request) {
        return aiAgentService.respond(request.get("message"));
    }

    @GetMapping("/assistant/history")
    public List<?> getAssistantHistory() {
        return aiAgentService.getRecentMessages();
    }
}
