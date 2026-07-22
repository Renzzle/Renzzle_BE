package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String senderEmail;

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
            message.setFrom(senderEmail);
            message.setRecipients(Message.RecipientType.TO, address);
            message.setSubject(subject);

            Context context = new Context();
            context.setVariable("verificationCode", code);
            String htmlContent = templateEngine.process(template, context);

            message.setText(htmlContent, "UTF-8", "html");
        } catch (MessagingException e) {
            throw new CustomException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR);
        }

        javaMailSender.send(message);
    }

}
