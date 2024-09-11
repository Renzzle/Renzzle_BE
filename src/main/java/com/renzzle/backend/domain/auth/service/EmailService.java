package com.renzzle.backend.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("spring.mail.username")
    private String senderEmail;

    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            int digit = random.nextInt(10);
            code.append(digit);
        }

        return code.toString();
    }

    public String sendAuthEmail(String address) {
        MimeMessage message = javaMailSender.createMimeMessage();
        String code = generateRandomCode();

        try {
            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, address);
            message.setSubject("[Renzzle] Email Verification");

            Context context = new Context();
            context.setVariable("verificationCode", code);
            String htmlContent = templateEngine.process("verification_email", context);

            message.setText(htmlContent, "UTF-8", "html");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        javaMailSender.send(message);

        return code;
    }

}
