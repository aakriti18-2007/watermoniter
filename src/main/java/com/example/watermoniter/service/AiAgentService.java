package com.example.watermoniter.service;

import com.example.watermoniter.model.AiChatMessage;
import com.example.watermoniter.model.WaterData;
import com.example.watermoniter.repository.AiChatMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiAgentService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final WaterService waterService;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String responsesUrl;

    public AiAgentService(
            WaterService waterService,
            AiChatMessageRepository aiChatMessageRepository,
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.model:gpt-5}") String model,
            @Value("${openai.responses.url:https://api.openai.com/v1/responses}") String responsesUrl
    ) {
        this.waterService = waterService;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.responsesUrl = responsesUrl;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Map<String, Object> getCopilotOverview() {
        WaterData current = waterService.getCurrentData();
        Map<String, Object> alert = waterService.getAlertSummary();
        Map<String, Object> insights = waterService.getInsights();

        String fallbackSummary = buildSummary(current, alert, insights);
        String summary = fallbackSummary;
        String provider = "Local intelligence";

        if (isOpenAiConfigured()) {
            provider = "OpenAI Responses API";
            String openAiSummary = generateWithOpenAi(
                    "You are AquaMind Copilot, an operations AI for a smart water monitoring portal. "
                            + "Produce a concise executive briefing in 2-3 sentences for an operator. "
                            + "Focus on system health, risks, and next action.",
                    buildContextPrompt(current, alert, insights, "Create a live operator briefing.")
            );

            if (openAiSummary != null && !openAiSummary.isBlank()) {
                summary = openAiSummary;
            } else {
                provider = "Local intelligence fallback";
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("agentName", "AquaMind Copilot");
        response.put("summary", summary);
        response.put("priority", alert.get("severity"));
        response.put("automationState", buildAutomationState(alert));
        response.put("nextBestAction", insights.get("recommendedAction"));
        response.put("provider", provider);
        response.put("suggestedPrompts", List.of(
                "Explain the current system health",
                "What happens in the next hour?",
                "Give me maintenance advice",
                "Is the water quality safe?"
        ));
        return response;
    }

    public Map<String, Object> respond(String message) {
        WaterData current = waterService.getCurrentData();
        Map<String, Object> alert = waterService.getAlertSummary();
        Map<String, Object> insights = waterService.getInsights();

        String answer = null;
        String provider = "Local intelligence";

        if (isOpenAiConfigured()) {
            provider = "OpenAI Responses API";
            answer = generateWithOpenAi(
                    "You are AquaMind Copilot, a futuristic but practical AI operator inside a water monitoring portal. "
                            + "Answer clearly, using the provided telemetry as source-of-truth. "
                            + "Do not invent sensor values. Keep answers concise and actionable.",
                    buildContextPrompt(current, alert, insights, message)
            );

            if (answer == null || answer.isBlank()) {
                provider = "Local intelligence fallback";
            }
        }

        if (answer == null || answer.isBlank()) {
            answer = respondLocally(message, current, alert, insights);
        }

        storeMessage("user", "Operator", message == null || message.isBlank() ? "Status request" : message);
        storeMessage("assistant", provider, answer);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("agentName", "AquaMind Copilot");
        response.put("message", answer);
        response.put("confidence", "Operational");
        response.put("provider", provider);
        return response;
    }

    public List<AiChatMessage> getRecentMessages() {
        return aiChatMessageRepository.findTop12ByOrderByCreatedAtAsc();
    }

    private void storeMessage(String role, String provider, String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        aiChatMessageRepository.save(new AiChatMessage(role, provider, LocalDateTime.now(), content));
    }

    private String respondLocally(String message, WaterData current, Map<String, Object> alert, Map<String, Object> insights) {
        String prompt = message == null ? "" : message.toLowerCase(Locale.ROOT).trim();

        if (prompt.contains("next hour") || prompt.contains("predict") || prompt.contains("forecast")) {
            return "Projected water level in the next hour is "
                    + insights.get("predictedLevelInNextHour")
                    + "%. Current trend is "
                    + insights.get("consumptionTrend")
                    + ", so refill planning should follow that direction.";
        }
        if (prompt.contains("quality") || prompt.contains("safe") || prompt.contains("ph")) {
            return "Water quality score is "
                    + insights.get("qualityScore")
                    + "/100. Current pH is "
                    + current.getPh()
                    + " and temperature is "
                    + current.getTemp()
                    + " C. "
                    + insights.get("recommendedAction");
        }
        if (prompt.contains("maintenance") || prompt.contains("service") || prompt.contains("advice")) {
            return "Maintenance guidance: " + insights.get("recommendedAction")
                    + " Latest alert state is " + alert.get("status")
                    + " and the tank was last updated on "
                    + current.getRecordedAt().format(FORMATTER) + ".";
        }

        return buildSummary(current, alert, insights);
    }

    private String generateWithOpenAi(String instructions, String userPrompt) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", model);
            request.put("instructions", instructions);
            request.put("input", List.of(Map.of(
                    "role", "user",
                    "content", userPrompt
            )));

            Map<?, ?> response = restClient.post()
                    .uri(responsesUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            return extractOutputText(response);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractOutputText(Map<?, ?> response) {
        if (response == null) {
            return null;
        }

        Object output = response.get("output");
        if (!(output instanceof List<?> outputList)) {
            return null;
        }

        List<String> texts = new ArrayList<>();

        for (Object item : outputList) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                continue;
            }

            Object content = itemMap.get("content");
            if (!(content instanceof List<?> contentList)) {
                continue;
            }

            for (Object contentItem : contentList) {
                if (!(contentItem instanceof Map<?, ?> contentMap)) {
                    continue;
                }

                if ("output_text".equals(contentMap.get("type")) && contentMap.get("text") instanceof String text) {
                    texts.add(text);
                }
            }
        }

        if (texts.isEmpty()) {
            return null;
        }

        return String.join("\n", texts).trim();
    }

    private boolean isOpenAiConfigured() {
        return !apiKey.isBlank();
    }

    private String buildContextPrompt(WaterData current, Map<String, Object> alert, Map<String, Object> insights, String question) {
        return """
                Live water system telemetry:
                - Level: %s%%
                - pH: %s
                - Temperature: %s C
                - Last updated: %s
                - Alert status: %s
                - Alert message: %s
                - Severity: %s
                - Consumption trend: %s
                - Quality score: %s/100
                - Predicted level in next hour: %s%%
                - Recommended action: %s

                Operator request:
                %s
                """.formatted(
                current.getLevel(),
                current.getPh(),
                current.getTemp(),
                current.getRecordedAt().format(FORMATTER),
                alert.get("status"),
                alert.get("message"),
                alert.get("severity"),
                insights.get("consumptionTrend"),
                insights.get("qualityScore"),
                insights.get("predictedLevelInNextHour"),
                insights.get("recommendedAction"),
                question == null || question.isBlank() ? "Provide a helpful status update." : question
        );
    }

    private String buildSummary(WaterData current, Map<String, Object> alert, Map<String, Object> insights) {
        return "Current level is " + current.getLevel()
                + "% with pH " + current.getPh()
                + " and temperature " + current.getTemp()
                + " C. Status is " + alert.get("status")
                + " with a " + alert.get("trend")
                + " trend. Health score is " + insights.get("qualityScore")
                + "/100 and the next recommended action is: " + insights.get("recommendedAction");
    }

    private String buildAutomationState(Map<String, Object> alert) {
        if ("high".equals(alert.get("severity"))) {
            return "Escalation ready";
        }
        if ("medium".equals(alert.get("severity"))) {
            return "Watching thresholds";
        }
        return "Autonomous watch active";
    }
}
