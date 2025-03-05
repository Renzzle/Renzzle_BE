package com.renzzle.backend.auth;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
public class JwtTest {

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
