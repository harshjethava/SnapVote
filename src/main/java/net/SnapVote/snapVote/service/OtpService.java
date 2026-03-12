package net.SnapVote.snapVote.service;

import net.SnapVote.snapVote.config.DynamicMongoService;
import net.SnapVote.snapVote.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    // Store OTP with timestamp
    private Map<String, OtpData> otpStore = new HashMap<>();

    @Autowired
    private EmailService emailService;

    @Autowired
    private DynamicMongoService mongoService;

    public String generateAndSendOtp(String username, String area) {

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        User user = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                User.class,
                "user"
        );

        if (user == null) {
            System.out.println("❌ User not found for OTP in area: " + area);
            return null;
        }

        String email = user.getEmail(); // Fetch email from DB

        if (email == null || email.trim().isEmpty()) {
            System.out.println("❌ No email found for user in area: " + area);
            return null;
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000); // 6-digit OTP
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        otpStore.put(username, new OtpData(otp, expiryTime));

        String htmlBody = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e5e7eb; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);\">" +
                "<div style=\"text-align: center; margin-bottom: 20px;\">" +
                "<h1 style=\"color: #7c3aed; margin: 0;\">SNAPVOTE</h1>" +
                "</div>" +
                "<div style=\"background-color: #f3f4f6; padding: 30px; border-radius: 8px; text-align: center;\">" +
                "<h2 style=\"color: #1f2937; margin-top: 0;\">Secure Login Verification</h2>" +
                "<p style=\"color: #4b5563; font-size: 16px;\">Hi " + user.getFullName() + ",</p>" +
                "<p style=\"color: #4b5563; font-size: 16px;\">You are attempting to log in to the <strong>" + user.getArea().toUpperCase() + "</strong> election area. Please use the following One-Time Password (OTP) to complete your secure login:</p>" +
                "<div style=\"margin: 30px 0;\">" +
                "<span style=\"font-size: 32px; font-weight: bold; color: #7c3aed; letter-spacing: 8px; padding: 10px 20px; background-color: #ede9fe; border-radius: 8px; display: inline-block;\">" + otp + "</span>" +
                "</div>" +
                "<p style=\"color: #ef4444; font-size: 14px; font-weight: bold;\">This OTP is valid for 5 minutes.</p>" +
                "</div>" +
                "<div style=\"margin-top: 20px; text-align: center; color: #9ca3af; font-size: 12px;\">" +
                "<p>If you did not request this OTP, please ignore this email or contact support.</p>" +
                "<p>&copy; " + LocalDateTime.now().getYear() + " SnapVote Election System</p>" +
                "</div>" +
                "</div>";

        String subject = "Your SnapVote Login OTP: " + otp;
        System.out.println("================================================");
        System.out.println("🔐 OTP FOR USER [" + username + "] IN AREA [" + area + "]: " + otp);
        System.out.println("================================================");
        emailService.sendEmailForOtp(email, subject, htmlBody);

        System.out.println("📧 OTP sent to: " + email);
        return otp;
    }

    public boolean validateOtp(String username, String otp) {
        if (!otpStore.containsKey(username)) return false;

        OtpData data = otpStore.get(username);

        if (data.getExpiry().isBefore(LocalDateTime.now())) {
            otpStore.remove(username); // Expired
            return false;
        }

        if (data.getOtp().equals(otp)) {
            otpStore.remove(username); // Valid and used once
            return true;
        }

        return false;
    }

    // Inner class to store OTP and expiry
    private static class OtpData {
        private String otp;
        private LocalDateTime expiry;

        public OtpData(String otp, LocalDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getExpiry() {
            return expiry;
        }
    }
}
