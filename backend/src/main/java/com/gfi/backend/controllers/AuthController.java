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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.auth.LoginRequest;
import com.gfi.backend.models.dtos.auth.RegisterRequest;
import com.gfi.backend.models.dtos.auth.RegisterResponse;
import com.gfi.backend.models.dtos.auth.TokenResponse;
import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.services.ITokenService;
import com.gfi.backend.services.interfaces.AuthPermissionService;
import com.gfi.backend.utils.SecurityContextUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Xác thực - Authentication")
public class AuthController extends ApiBaseController {
        private final AuthenticationManager authenticationManager;
        private final ITokenService tokenService;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthPermissionService authPermissionService;

        public AuthController(AuthenticationManager authenticationManager, ITokenService tokenService,
                        UserRepository userRepository, RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder, AuthPermissionService authPermissionService) {
                this.authenticationManager = authenticationManager;
                this.tokenService = tokenService;
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.passwordEncoder = passwordEncoder;
                this.authPermissionService = authPermissionService;
        }

        @PostMapping("/login")
        @Operation(summary = "Login", description = "Authenticate user and return JWT tokens.")
        public ResponseEntity<ApiResult<TokenResponse>> login(
                        @org.springframework.web.bind.annotation.RequestBody LoginRequest loginRequest) {
                try {
                        User user = userRepository.findByUsernameWithStaffAndRole(loginRequest.getUsername())
                                        .orElse(null);
                        if (user != null && Integer.valueOf(0).equals(user.getStatus())) {
                                return ResponseEntity
                                                .status(HttpStatus.UNAUTHORIZED)
                                                .body(ApiResult.fail(
                                                                CommonErrorCode.USER_INACTIVE.getCode(),
                                                                CommonErrorCode.USER_INACTIVE.getMessage()));
                        }

                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        loginRequest.getUsername(),
                                                        loginRequest.getPassword()));

                        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                        TokenResponse tokens = tokenService.generateTokens(userDetails);

                        user = userRepository.findByUsernameWithStaffAndRole(userDetails.getUsername())
                                        .orElseThrow(() -> new BadCredentialsException(
                                                        CommonErrorCode.INVALID_CREDENTIALS.getMessage()));

                        tokens.setUser(toUserInfo(user));

                        // Fetch and set permissions if user has a role
                        if (user.getRole() != null) {
                                tokens.setPermissions(authPermissionService
                                                .getPermissionsByRoleId(user.getRole().getId(), user.getId()));
                        }

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

        @PostMapping("/register")
        @Operation(summary = "Register new user", description = "Create new user account with FE-hashed password and BCrypt encoding on backend.")
        public ResponseEntity<ApiResult<RegisterResponse>> register(
                        @RequestBody RegisterRequest registerRequest) {
                try {
                        // Check if username already exists
                        if (userRepository.existsByUsername(registerRequest.getUsername())) {
                                return ResponseEntity
                                                .status(HttpStatus.CONFLICT)
                                        .body(ApiResult.fail(1001, "Tên đăng nhập đã tồn tại"));
                        }

                        // Create new user entity
                        User newUser = new User();
                        newUser.setUsername(registerRequest.getUsername());
                        
                        // FE sends: BCrypt hash from client
                        // BE does: BCrypt(client_hash) and store in passwordHash column
                        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());
                        newUser.setPasswordHash(encodedPassword);
                        
                        newUser.setStatus(1); // Active status
                        
                        // Set role (default: get first role or from request)
                        if (registerRequest.getRoleId() != null) {
                                var role = roleRepository.findById(registerRequest.getRoleId());
                                if (role.isPresent()) {
                                        newUser.setRole(role.get());
                                } else {
                                        return ResponseEntity
                                                        .status(HttpStatus.BAD_REQUEST)
                                                .body(ApiResult.fail(1002, "Role không tồn tại"));
                                }
                        } else {
                                // Default to first available role
                                var defaultRole = roleRepository.findAll().stream().findFirst();
                                if (defaultRole.isPresent()) {
                                        newUser.setRole(defaultRole.get());
                                } else {
                                        return ResponseEntity
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(ApiResult.fail(1003, "Không có role mặc định"));
                                }
                        }
                        
                        // Save to database
                        User savedUser = userRepository.save(newUser);
                        
                        // Build response
                        RegisterResponse response = RegisterResponse.builder()
                                        .id(savedUser.getId())
                                        .username(savedUser.getUsername())
                                        .fullName(savedUser.getFullName())
                                        .message("Đăng ký tài khoản thành công")
                                        .build();
                        
                        return ResponseEntity
                                        .status(HttpStatus.CREATED)
                                        .body(ApiResult.success(response, "Đăng ký thành công"));
                        
                } catch (Exception ex) {
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApiResult.fail(1000, "Lỗi đăng ký: " + ex.getMessage()));
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
                                .unit(user.getStaff() == null || user.getStaff().getUnit() == null ? null :
                                        TokenResponse.UnitInfo.builder()
                                                .id(user.getStaff().getUnit().getId())
                                                .code(user.getStaff().getUnit().getCode())
                                                .name(user.getStaff().getUnit().getName())
                                                .build())
                                .build();
        }

        @PostMapping("/logout")
        @Operation(summary = "Logout", description = "Clear auth cookie on the client.")
        public ResponseEntity<ApiResult<String>> logout() {
                // Clear user scopes from ThreadLocal
                SecurityContextUtils.clearUserScopes();

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
