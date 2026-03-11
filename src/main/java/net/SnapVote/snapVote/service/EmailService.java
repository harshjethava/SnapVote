package net.SnapVote.snapVote.service;

import lombok.extern.slf4j.Slf4j;
import net.SnapVote.snapVote.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendMail(String[] to, String sub, String body) {
        try {
            if (to == null || to.length == 0) {
                log.info("No recipients found for email: " + sub);
                return;
            }
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);
            mail.setSubject(sub);
            mail.setText(body);
            mail.setFrom("stduy21@gmail.com");
            javaMailSender.send(mail);
            log.info("Email sent successfully to " + to.length + " recipients.");
        } catch (Exception e) {
            log.error("Exception in mail", e);
        }
    }

    public void sendEmailForOtp(String to, String subject, String body) {
        try{
            jakarta.mail.internet.MimeMessage message = javaMailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true indicates HTML format
            helper.setFrom("stduy21@gmail.com");

            javaMailSender.send(message);
            System.out.println("✅ OTP mail sent successfully to: " + to);
        }catch(Exception e)
        {
            System.out.println("❌ Failed to send OTP email: " + e.getMessage());
        }

    }


}
