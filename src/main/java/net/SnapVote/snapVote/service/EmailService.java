package net.SnapVote.snapVote.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@Slf4j
public class EmailService {

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMail(String[] to, String subject, String body) {

        if (to == null || to.length == 0) {
            log.warn("No recipients found for email: {}", subject);
            return;
        }

        for (String recipient : to) {
            sendEmailInternal(recipient, subject, "<p>" + body + "</p>");
        }

        log.info("Reminder email sent to {} recipients.", to.length);
    }

    @PostConstruct
    public void init() {

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("BREVO_API_KEY is NOT configured. Email sending will fail.");
        } else {
            log.info("Brevo API key loaded successfully.");
        }

        log.info("Sender email configured as: {}", senderEmail);
    }

    public void sendEmailForOtp(String to, String subject, String htmlBody) {
        sendEmailInternal(to, subject, htmlBody);
    }

    private void sendEmailInternal(String to, String subject, String htmlContent) {

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.error("Brevo API key is missing. Cannot send email.");
            return;
        }

        try {

            Map<String, Object> sender = Map.of(
                    "name", "SnapVote",
                    "email", senderEmail);

            Map<String, Object> recipient = Map.of(
                    "email", to);

            Map<String, Object> body = new HashMap<>();
            body.put("sender", sender);
            body.put("to", List.of(recipient));
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            log.info("Brevo response status: {}", response.getStatusCode());
            log.info("Brevo response body: {}", response.getBody());

        } catch (Exception ex) {

            log.error("Failed to send email via Brevo", ex);

        }
    }
}