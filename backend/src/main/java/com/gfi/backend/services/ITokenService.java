package com.gfi.backend.services;

import org.springframework.security.core.userdetails.UserDetails;

import com.gfi.backend.models.dtos.auth.TokenResponse;

public interface ITokenService {
    TokenResponse generateTokens(UserDetails userDetails);
}
