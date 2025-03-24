package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.dao.RefreshTokenRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private Clock clock;
    @Mock
    private RefreshTokenRedisRepository refreshTokenRedisRepository;
    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    public void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2025-03-20T10:00:00Z"));
    }

}
