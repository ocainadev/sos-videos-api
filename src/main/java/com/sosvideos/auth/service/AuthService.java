package com.sosvideos.auth.service;

import com.sosvideos.auth.dto.ChangePasswordRequest;
import com.sosvideos.auth.dto.LoginRequest;
import com.sosvideos.auth.dto.LoginResponse;
import com.sosvideos.auth.dto.MeResponse;
import com.sosvideos.exception.BusinessException;
import com.sosvideos.security.JwtService;
import com.sosvideos.security.UserPrincipal;
import com.sosvideos.subscription.entity.Subscription;
import com.sosvideos.subscription.enums.SubscriptionStatus;
import com.sosvideos.subscription.repository.SubscriptionRepository;
import com.sosvideos.terms.repository.TermAcceptanceRepository;
import com.sosvideos.user.entity.User;
import com.sosvideos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TermAcceptanceRepository termAcceptanceRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = principal.getUser();

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        Optional<Subscription> sub = subscriptionRepository
                .findActiveByUserId(user.getId(), SubscriptionStatus.ACTIVE, LocalDate.now());

        boolean hasAcceptedTerm = termAcceptanceRepository
                .findActiveTermAcceptanceByUserId(user.getId()).isPresent();

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                Boolean.TRUE.equals(user.getFirstLoginPendingPasswordChange()),
                hasAcceptedTerm,
                sub.map(s -> s.getStatus().name()).orElse(null),
                sub.map(Subscription::getEndDate).orElse(null)
        );
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Senha atual incorreta.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("A nova senha não pode ser igual à senha atual.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLoginPendingPasswordChange(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        Optional<Subscription> sub = subscriptionRepository
                .findActiveByUserId(user.getId(), SubscriptionStatus.ACTIVE, LocalDate.now());

        boolean hasAcceptedTerm = termAcceptanceRepository
                .findActiveTermAcceptanceByUserId(user.getId()).isPresent();

        return new MeResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                MeResponse.maskCpf(user.getCpf()),
                user.getRole().name(),
                Boolean.TRUE.equals(user.getFirstLoginPendingPasswordChange()),
                hasAcceptedTerm,
                sub.map(s -> s.getStatus().name()).orElse(null),
                sub.map(Subscription::getEndDate).orElse(null)
        );
    }
}
