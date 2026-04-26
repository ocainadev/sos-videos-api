package com.sosvideos.admin.service;

import com.sosvideos.admin.dto.CreateSubscriberRequest;
import com.sosvideos.admin.dto.PageResponse;
import com.sosvideos.admin.dto.SubscriberSummaryResponse;
import com.sosvideos.admin.dto.SubscriptionAdjustRequest;
import com.sosvideos.admin.dto.UserStatusRequest;
import com.sosvideos.exception.BusinessException;
import com.sosvideos.exception.ResourceNotFoundException;
import com.sosvideos.subscription.entity.Subscription;
import com.sosvideos.subscription.enums.SubscriptionStatus;
import com.sosvideos.subscription.repository.SubscriptionRepository;
import com.sosvideos.terms.repository.TermAcceptanceRepository;
import com.sosvideos.user.entity.User;
import com.sosvideos.user.enums.UserRole;
import com.sosvideos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TermAcceptanceRepository termAcceptanceRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Subscribers ───────────────────────────────────────────────────────────

    @Transactional
    public SubscriberSummaryResponse createSubscriber(CreateSubscriberRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Já existe um usuário com este e-mail.");
        }
        if (request.getCpf() != null && !request.getCpf().isBlank()
                && userRepository.existsByCpf(request.getCpf())) {
            throw new BusinessException("Já existe um usuário com este CPF.");
        }

        String initialPassword = (request.getCpf() != null && !request.getCpf().isBlank())
                ? request.getCpf().replaceAll("\\D", "")
                : request.getEmail();

        UserRole role = UserRole.SUBSCRIBER;
        if ("SUPPORT".equalsIgnoreCase(request.getRole())) {
            role = UserRole.SUPPORT;
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .cpf(request.getCpf())
                .passwordHash(passwordEncoder.encode(initialPassword))
                .role(role)
                .active(true)
                .firstLoginPendingPasswordChange(true)
                .build();

        user = userRepository.save(user);

        // Only create a default PENDING subscription for subscribers (not support users)
        if (role == UserRole.SUBSCRIBER) {
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .status(SubscriptionStatus.PENDING)
                    .build();
            subscriptionRepository.save(subscription);
        }

        log.info("User created manually by admin: id={} email={} role={}", user.getId(), user.getEmail(), role);
        return toSummary(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriberSummaryResponse> listSubscribers(String search,
                                                                    boolean includeInactive,
                                                                    Pageable pageable) {
        String q = (search == null || search.isBlank()) ? null : search;
        Page<User> page = userRepository.findSubscribers(UserRole.SUBSCRIBER, q, includeInactive, pageable);
        return PageResponse.from(page.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public SubscriberSummaryResponse getSubscriber(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getRole() == UserRole.SUBSCRIBER)
                .orElseThrow(() -> new ResourceNotFoundException("Assinante", userId));
        return toSummary(user);
    }

    @Transactional
    public void toggleUserStatus(Long userId, UserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        user.setActive(request.getActive());
        userRepository.save(user);
        log.info("User id={} active set to {} by admin", userId, request.getActive());
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────

    @Transactional
    public void adjustSubscription(Long subscriptionId, SubscriptionAdjustRequest request) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", subscriptionId));

        String previous = sub.getStatus().name();

        if (request.getStartDate() != null) {
            sub.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            sub.setEndDate(request.getEndDate());
        }

        // Regra de negócio: ACTIVE só é permitido quando a data de início efetiva
        // (a que ficará gravada) for hoje ou anterior — sem ativação antecipada.
        if (request.getStatus() == SubscriptionStatus.ACTIVE) {
            LocalDate effectiveStart = sub.getStartDate(); // já atualizado acima se veio no request
            if (effectiveStart != null && effectiveStart.isAfter(LocalDate.now())) {
                throw new BusinessException(
                        "Não é possível ativar uma assinatura com data de início no futuro (" + effectiveStart + ").");
            }
        }

        sub.setStatus(request.getStatus());
        subscriptionRepository.save(sub);
        // Sanitiza o reason para evitar log injection (remove quebras de linha)
        String safeReason = request.getReason() == null ? "—"
                : request.getReason().replaceAll("[\r\n\t]", " ").strip();
        log.info("Subscription id={} manually adjusted: {} -> {} | endDate={} | reason={}",
                subscriptionId, previous, request.getStatus(),
                sub.getEndDate(), safeReason);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SubscriberSummaryResponse toSummary(User user) {
        Optional<Subscription> sub = subscriptionRepository
                .findTopByUserIdOrderByCreatedAtDesc(user.getId());

        boolean hasAcceptedTerm = termAcceptanceRepository
                .findActiveTermAcceptanceByUserId(user.getId()).isPresent();

        return new SubscriberSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                Boolean.TRUE.equals(user.getActive()),
                user.getCreatedAt(),
                sub.map(s -> s.getId()).orElse(null),
                sub.map(s -> s.getStatus().name()).orElse(null),
                sub.map(Subscription::getStartDate).orElse(null),
                sub.map(Subscription::getEndDate).orElse(null),
                sub.map(Subscription::getLastPaymentDate).orElse(null),
                sub.map(Subscription::getKiwifySubscriptionId).orElse(null),
                sub.map(Subscription::getKiwifyCustomerId).orElse(null),
                hasAcceptedTerm
        );
    }
}
