package net.SnapVote.snapVote.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:stduy21@gmail.com}")
    private String senderEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    // Used for sending bulk reminder emails (plain text)
    public void sendMail(String[] to, String sub, String body) {
        if (to == null || to.length == 0) {
            log.info("No recipients found for email: {}", sub);
            return;
        }
        for (String recipient : to) {
            sendEmailInternal(recipient, sub, "<p>" + body + "</p>");
        }
        log.info("Reminder email dispatched to {} recipients.", to.length);
    }

    // Used for OTP emails (HTML)
    public void sendEmailForOtp(String to, String subject, String htmlBody) {
        sendEmailInternal(to, subject, htmlBody);
    }

    private void sendEmailInternal(String to, String subject, String htmlContent) {
        try {
            if (brevoApiKey == null || brevoApiKey.isBlank()) {
                log.error("Brevo API key is not configured! Set the BREVO_API_KEY environment variable.");
                return;
            }

            Map<String, Object> sender = new HashMap<>();
            sender.put("name", "SnapVote");
            sender.put("email", senderEmail);

            Map<String, Object> recipient = new HashMap<>();
            recipient.put("email", to);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sender", sender);
            requestBody.put("to", List.of(recipient));
            requestBody.put("subject", subject);
            requestBody.put("htmlContent", htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Email sent successfully via Brevo to: " + to);
            } else {
                log.error("Brevo API returned non-OK response: {} - {}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            System.out.println("❌ Failed to send OTP email: " + e.getMessage());
            log.error("Brevo email send failed", e);
        }
    }
}
