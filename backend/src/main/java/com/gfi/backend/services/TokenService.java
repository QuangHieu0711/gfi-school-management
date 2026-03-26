package com.gfi.backend.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.gfi.backend.models.dtos.auth.TokenResponse;
import com.gfi.backend.models.security.JwtTokenProvider;

@Service
public class TokenService implements ITokenService {

    private final JwtTokenProvider tokenProvider;

    public TokenService(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public TokenResponse generateTokens(UserDetails userDetails) {
        String accessToken = tokenProvider.generateAccessToken(userDetails);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.extractExpiration(accessToken).getTime())
                .build();
    }
}
