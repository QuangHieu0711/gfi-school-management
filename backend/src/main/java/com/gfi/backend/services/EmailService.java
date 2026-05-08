package com.gfi.backend.services;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service gửi email qua SMTP (Gmail).
 * Sử dụng MimeMessageHelper để hỗ trợ HTML email + inline images (CID).
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
     * Gửi email HTML với inline logo (CID attachment).
     * Logo được nhúng trực tiếp vào email, không cần URL bên ngoài.
     *
     * @param to       email người nhận
     * @param subject  tiêu đề email
     * @param htmlBody nội dung email (HTML), dùng src="cid:logo" để hiển thị logo
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart mode (cần để đính kèm inline image)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(mailUsername, fromName, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            // Embed logo trực tiếp vào email bằng CID
            // Trong HTML dùng: <img src="cid:logo">
            ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
            if (logoResource.exists()) {
                helper.addInline("logo", logoResource, "image/png");
            }

            mailSender.send(message);
            log.info("Đã gửi email tới: {}", to);
        } catch (Exception e) {
            log.error("Gửi email thất bại tới: {}", to, e);
            throw new RuntimeException("Không thể gửi email", e);
        }
    }
}
