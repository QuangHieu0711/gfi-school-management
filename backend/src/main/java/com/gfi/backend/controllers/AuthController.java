package com.gfi.backend.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.gfi.backend.models.dtos.auth.LoginRequest;
import com.gfi.backend.models.dtos.auth.TokenResponse;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.models.security.UserPrincipal;
import com.gfi.backend.services.ITokenService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController extends ApiBaseController {
    private final AuthenticationManager authenticationManager;
    private final ITokenService tokenService;

    // Constructor injection
    public AuthController(AuthenticationManager authenticationManager, ITokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return JWT tokens.")
    public ResponseEntity<ApiResult<TokenResponse>> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Lấy role (ví dụ lấy role đầu tiên)
            String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("USER");

            // Lấy fullName từ UserPrincipal
            String fullName = "";
            if (userDetails instanceof UserPrincipal) {
                fullName = ((UserPrincipal) userDetails).getFullName();
            }

            TokenResponse tokens = tokenService.generateTokens(userDetails);
            tokens.setRole(role);
            tokens.setFullName(fullName);
            tokens.setUserId(((UserPrincipal) userDetails).getId());

            ResponseCookie cookie = ResponseCookie.from("authToken", tokens.getAccessToken())
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
                .body(ApiResult.fail("Tên đăng nhập hoặc mật khẩu không chính xác"));
        }
    }
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Clear auth cookie on the client.")
    public ResponseEntity<ApiResult<String>> logout() {
        // Tạo cookie hết hạn để xóa ở browser
        ResponseCookie deleteCookie = ResponseCookie.from("authToken", "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .sameSite("Lax")
            .maxAge(0) // Hết hạn ngay lập tức
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
            .body(ApiResult.success(null, "Đăng xuất thành công"));
    }
}
