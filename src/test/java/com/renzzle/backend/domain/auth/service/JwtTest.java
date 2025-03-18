package com.renzzle.backend.domain.auth.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.renzzle.backend.domain.auth.service.JwtTest.JWT_TEST_PROPERTY;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JwtProvider.class)
@TestPropertySource(properties = JWT_TEST_PROPERTY)
public class JwtTest {

    public static final String JWT_TEST_PROPERTY = "spring.jwt.secret=ad3sf2sf98a7sd9f87a0ds98f70a98sd7f098asd70f98";

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    public void randomJwtTokenTest() throws InterruptedException {
        String email = "user@gmail.com";

        String token1 = jwtProvider.createAuthVerityToken(email);
        Thread.sleep(1000);
        String token2 = jwtProvider.createAuthVerityToken(email);

        Assertions.assertNotEquals(token1, token2);
    }

    @Test
    // test about accessToken, refreshToken
    public void userAuthenticationTokenTest() {
        long userId = 1;
        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        long id1 = jwtProvider.getUserId(accessToken);
        long id2 = jwtProvider.getUserId(refreshToken);
        Assertions.assertEquals(id1, id2);
    }

    @Test
    public void authVerityTokenTest() {
        String email = "user@gmail.com";

        String verityToken = jwtProvider.createAuthVerityToken(email);
        String parsedEmail = jwtProvider.getEmail(verityToken);

        Assertions.assertEquals(email, parsedEmail);
    }

}
