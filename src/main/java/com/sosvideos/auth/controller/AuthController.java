package com.sosvideos.auth.controller;

import com.sosvideos.auth.dto.ChangePasswordRequest;
import com.sosvideos.auth.dto.LoginRequest;
import com.sosvideos.auth.dto.LoginResponse;
import com.sosvideos.auth.dto.MeResponse;
import com.sosvideos.auth.service.AuthService;
import com.sosvideos.common.ApiResponse;
import com.sosvideos.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticação e gerenciamento de conta")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login por e-mail e senha")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Troca de senha", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.changePassword(principal, request);
        return ResponseEntity.ok(ApiResponse.ok("Senha alterada com sucesso."));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Dados do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<MeResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(authService.getMe(principal)));
    }
}
