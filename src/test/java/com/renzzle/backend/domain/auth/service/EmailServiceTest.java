package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.dao.EmailRedisRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private SpringTemplateEngine templateEngine;
    @Mock
    private EmailRedisRepository emailRepository;

    @InjectMocks
    private EmailService emailService;

}
