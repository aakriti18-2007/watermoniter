package com.example.watermoniter.service;

import com.example.watermoniter.model.AlertEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final boolean emailEnabled;
    private final boolean twilioEnabled;
    private final String mailFrom;
    private final String mailTo;
    private final String twilioSid;
    private final String twilioToken;
    private final String twilioFromSms;
    private final String twilioToSms;
    private final String twilioFromWhatsapp;
    private final String twilioToWhatsapp;

    public NotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${notifications.email.enabled:false}") boolean emailEnabled,
            @Value("${notifications.email.from:}") String mailFrom,
            @Value("${notifications.email.to:}") String mailTo,
            @Value("${notifications.twilio.enabled:false}") boolean twilioEnabled,
            @Value("${notifications.twilio.account-sid:}") String twilioSid,
            @Value("${notifications.twilio.auth-token:}") String twilioToken,
            @Value("${notifications.twilio.from-sms:}") String twilioFromSms,
            @Value("${notifications.twilio.to-sms:}") String twilioToSms,
            @Value("${notifications.twilio.from-whatsapp:}") String twilioFromWhatsapp,
            @Value("${notifications.twilio.to-whatsapp:}") String twilioToWhatsapp
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.emailEnabled = emailEnabled;
        this.mailFrom = mailFrom;
        this.mailTo = mailTo;
        this.twilioEnabled = twilioEnabled;
        this.twilioSid = twilioSid;
        this.twilioToken = twilioToken;
        this.twilioFromSms = twilioFromSms;
        this.twilioToSms = twilioToSms;
        this.twilioFromWhatsapp = twilioFromWhatsapp;
        this.twilioToWhatsapp = twilioToWhatsapp;
        this.restClient = RestClient.builder().build();
    }

    public List<String> notify(AlertEvent event) {
        List<String> delivered = new ArrayList<>();

        if (emailEnabled && !mailFrom.isBlank() && !mailTo.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(mailTo);
                message.setSubject("Water Alert: " + event.getStatus());
                message.setText(event.getMessage());
                mailSender.send(message);
                delivered.add("email");
            } catch (Exception ignored) {
            }
        }

        if (twilioEnabled && !twilioSid.isBlank() && !twilioToken.isBlank()) {
            if (!twilioFromSms.isBlank() && !twilioToSms.isBlank()) {
                if (sendTwilioMessage(twilioFromSms, twilioToSms, event.getMessage())) {
                    delivered.add("sms");
                }
            }

            if (!twilioFromWhatsapp.isBlank() && !twilioToWhatsapp.isBlank()) {
                if (sendTwilioMessage("whatsapp:" + twilioFromWhatsapp, "whatsapp:" + twilioToWhatsapp, event.getMessage())) {
                    delivered.add("whatsapp");
                }
            }
        }

        return delivered;
    }

    private boolean sendTwilioMessage(String from, String to, String body) {
        try {
            String auth = Base64.getEncoder().encodeToString((twilioSid + ":" + twilioToken).getBytes());
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", from);
            form.add("To", to);
            form.add("Body", body);

            restClient.post()
                    .uri("https://api.twilio.com/2010-04-01/Accounts/" + twilioSid + "/Messages.json")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
