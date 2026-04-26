package com.sosvideos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosvideos.common.ApiResponse;
import com.sosvideos.user.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Bloqueia acesso a endpoints protegidos quando o usuário ainda não trocou
 * a senha temporária do primeiro login.
 *
 * <p>Enquanto {@code firstLoginPendingPasswordChange = true}, apenas os
 * endpoints listados em {@link #ALLOWED_PATHS} são liberados.
 * Qualquer outra requisição autenticada recebe HTTP 403 com mensagem orientando
 * o usuário a trocar a senha.</p>
 */
@Component
@RequiredArgsConstructor
public class FirstLoginEnforcementFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    /** Paths liberados mesmo com senha temporária ainda ativa. */
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/change-password",
            "/api/auth/me",
            "/api/auth/login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof UserPrincipal principal) {

            // Aplica somente a SUBSCRIBER — ADMIN e SUPPORT são usuários internos
            // e não devem ser bloqueados de operar com senha temporária.
            boolean isSubscriber = principal.getUser().getRole() == UserRole.SUBSCRIBER;
            boolean mustChangePassword = isSubscriber
                    && Boolean.TRUE.equals(principal.getUser().getFirstLoginPendingPasswordChange());

            if (mustChangePassword && !isAllowedPath(request.getRequestURI())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                String body = objectMapper.writeValueAsString(
                        ApiResponse.error("Você precisa trocar sua senha antes de continuar. "
                                + "Acesse /api/auth/change-password."));
                response.getWriter().write(body);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isAllowedPath(String uri) {
        return ALLOWED_PATHS.stream().anyMatch(uri::startsWith);
    }
}
