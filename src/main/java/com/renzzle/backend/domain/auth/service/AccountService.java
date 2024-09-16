package com.renzzle.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final JwtProvider jwtProvider;

    public String createAuthVerityToken(String email) {
        return jwtProvider.createAuthVerityToken(email);
    }

}
