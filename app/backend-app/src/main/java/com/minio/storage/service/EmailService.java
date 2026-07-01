package com.minio.storage.service;

import com.minio.storage.util.EmailTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @SuppressWarnings("unused")
    @Value("${spring.mail.username}")
    private String fromEmail;

    @SuppressWarnings("unused")
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public boolean sendVerificationEmail(String toEmail, String username, String verificationKey) {
        String link = frontendUrl + "/auth/verify-email?username="
                + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&key="
                + URLEncoder.encode(verificationKey, StandardCharsets.UTF_8);
        String body = EmailTemplates.getVerificationEmail(username, link);
        return sendMail(toEmail, "MinIO Storage - Verifikacija naloga", body);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean sendWelcomeEmail(String toEmail, String username) {
        String loginUrl = frontendUrl + "/auth/login";
        String body = EmailTemplates.getWelcomeEmail(username, loginUrl);
        return sendMail(toEmail, "Dobrodošli na MinIO Storage!", body);
    }

    public boolean sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        String link = frontendUrl + "/auth/reset-password?token=" + resetToken;
        String body = EmailTemplates.getPasswordResetEmail(username, link);
        return sendMail(toEmail, "MinIO Storage - Resetovanje lozinke", body);
    }

    private boolean sendMail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            javaMailSender.send(message);
            log.info("Email je uspješno poslat na adresu: {}", to);
            return true;
        } catch (Exception e) {
            log.error("Greška pri slanju email-a na {}: {}", to, e.getMessage());
            return false;
        }
    }

}