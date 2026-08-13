package com.ejobs.portal.controller;

import com.ejobs.portal.dto.auth.AuthResponse;
import com.ejobs.portal.dto.auth.ForgotPasswordRequest;
import com.ejobs.portal.dto.auth.ForgotPasswordResponse;
import com.ejobs.portal.dto.auth.LoginRequest;
import com.ejobs.portal.dto.auth.RegisterRequest;
import com.ejobs.portal.dto.auth.ResetPasswordRequest;
import com.ejobs.portal.service.AuthService;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration and login (FR-AUTH-01 to FR-AUTH-04)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new account",
            description = "FR-AUTH-01. Creates a JOB_SEEKER or EMPLOYER account and returns "
                    + "a signed JWT so the caller is logged in immediately."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = "FR-AUTH-03. Exchanges email and password for a signed JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request a password reset",
            description = "Always returns 200 with the same message, whether or not the "
                    + "email is registered - the response must not reveal which addresses "
                    + "hold accounts. Under the 'local' profile the raw token is echoed in "
                    + "resetToken so the flow can be tested without a mailbox; elsewhere "
                    + "that field is null and the token is only logged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request accepted"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Redeem a password reset token",
            description = "Tokens are single-use and expire 30 minutes after issue. "
                    + "Unknown, already-used and expired tokens all return the same 400."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Validation failed, or the "
                    + "token is invalid, already used, or expired")
    })
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "Your password has been updated. You can now sign in."));
    }
}
