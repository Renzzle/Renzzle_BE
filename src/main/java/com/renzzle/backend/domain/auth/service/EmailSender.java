package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    // Sender address is separate from spring.mail.username:
    // OCI Email Delivery uses an OCID-formatted SMTP username, not an email address.
    @Value("${app.mail.from}")
    private String senderEmail;

    @Value("${app.mail.from-name}")
    private String senderName;

    @Async
    public void sendAuthEmail(String address, String code) {
        sendEmail(address, code, "[Renzzle] Email Verification", "email/verification");
    }

    @Async
    public void sendPasswordResetEmail(String address, String code) {
        sendEmail(address, code, "[Renzzle] Password Reset", "email/password-reset");
    }

    private void sendEmail(String address, String code, String subject, String template) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            message.setFrom(new InternetAddress(senderEmail, senderName, "UTF-8"));
            message.setRecipients(Message.RecipientType.TO, address);
            message.setSubject(subject);

            Context context = new Context();
            context.setVariable("verificationCode", code);
            String htmlContent = templateEngine.process(template, context);

            message.setText(htmlContent, "UTF-8", "html");
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new CustomException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            // @Async swallows exceptions from the caller's perspective; log so delivery failures are visible.
            log.error("Failed to send email to {}: {}", address, e.getMessage(), e);
            throw e;
        }
    }

}
