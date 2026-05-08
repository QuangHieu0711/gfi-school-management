package com.gfi.backend.services;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service gửi email qua SMTP (Gmail).
 * Sử dụng MimeMessageHelper để hỗ trợ HTML email + display name.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.mail.from-name:Hệ thống GFI}")
    private String fromName;

    /**
     * Gửi email HTML với display name.
     *
     * @param to      email người nhận
     * @param subject tiêu đề email
     * @param htmlBody nội dung email (HTML)
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(new InternetAddress(mailUsername, fromName, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
            log.info("Đã gửi email tới: {}", to);
        } catch (Exception e) {
            log.error("Gửi email thất bại tới: {}", to, e);
            throw new RuntimeException("Không thể gửi email", e);
        }
    }
}
