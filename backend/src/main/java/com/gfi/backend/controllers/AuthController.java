package com.gfi.backend.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.auth.LoginRequest;
import com.gfi.backend.models.dtos.auth.TokenResponse;
import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.services.ITokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController extends ApiBaseController {
    private final AuthenticationManager authenticationManager;
    private final ITokenService tokenService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, ITokenService tokenService,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return JWT tokens.")
    public ResponseEntity<ApiResult<TokenResponse>> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            TokenResponse tokens = tokenService.generateTokens(userDetails);

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new BadCredentialsException(CommonErrorCode.INVALID_CREDENTIALS.getMessage()));

            tokens.setUser(toUserInfo(user));

            ResponseCookie cookie = ResponseCookie.from("authToken", tokens.getToken().getAccessToken())
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(7 * 24 * 60 * 60)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(ApiResult.success(tokens, "Đăng nhập thành công"));

        } catch (BadCredentialsException ex) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResult.fail(
                            CommonErrorCode.INVALID_CREDENTIALS.getCode(),
                            CommonErrorCode.INVALID_CREDENTIALS.getMessage()));
        }
    }

    private TokenResponse.UserInfo toUserInfo(User user) {
        return TokenResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .role(TokenResponse.RoleInfo.builder()
                        .id(user.getRole() == null ? null : user.getRole().getId())
                        .code(user.getRole() == null ? null : user.getRole().getCode())
                        .name(user.getRole() == null ? null : user.getRole().getRoleName())
                        .build())
                .unit(TokenResponse.UnitInfo.builder()
                        .id(user.getUnit() == null ? null : user.getUnit().getId())
                        .code(user.getUnit() == null ? null : user.getUnit().getCode())
                        .name(user.getUnit() == null ? null : user.getUnit().getName())
                        .build())
                .build();
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Clear auth cookie on the client.")
    public ResponseEntity<ApiResult<String>> logout() {
        ResponseCookie deleteCookie = ResponseCookie.from("authToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResult.success(null, "Đăng xuất thành công"));
    }
}
