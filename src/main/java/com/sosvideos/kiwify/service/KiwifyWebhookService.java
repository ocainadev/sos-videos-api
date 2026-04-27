package com.sosvideos.kiwify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosvideos.common.AppConstants;
import com.sosvideos.config.AppProperties;
import com.sosvideos.exception.BusinessException;
import com.sosvideos.kiwify.dto.KiwifyWebhookPayload;
import com.sosvideos.kiwify.dto.KiwifyWebhookPayload.CustomerData;
import com.sosvideos.kiwify.entity.WebhookEvent;
import com.sosvideos.kiwify.repository.WebhookEventRepository;
import com.sosvideos.subscription.entity.Subscription;
import com.sosvideos.subscription.enums.SubscriptionStatus;
import com.sosvideos.subscription.repository.SubscriptionRepository;
import com.sosvideos.user.entity.User;
import com.sosvideos.user.enums.UserRole;
import com.sosvideos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KiwifyWebhookService {

    private final WebhookEventRepository webhookEventRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    @Transactional
    public void process(String rawPayload) {
        KiwifyWebhookPayload payload = parse(rawPayload);
        String externalId = buildExternalId(payload);

        if (webhookEventRepository.existsByProviderAndExternalId(AppConstants.KIWIFY_PROVIDER, externalId)) {
            log.info("Duplicate Kiwify event ignored: {}", externalId);
            return;
        }

        WebhookEvent event = WebhookEvent.builder()
                .provider(AppConstants.KIWIFY_PROVIDER)
                .eventType(payload.getWebhookEventType())
                .externalId(externalId)
                .payloadJson(rawPayload)
                .processed(false)
                .build();
        webhookEventRepository.save(event);

        try {
            dispatch(payload);
            event.setProcessed(true);
            event.setProcessedAt(LocalDateTime.now());
            log.info("Kiwify event processed: {} [{}]", payload.getWebhookEventType(), externalId);
        } catch (Exception e) {
            log.error("Error processing Kiwify event {}: {}", externalId, e.getMessage(), e);
            event.setErrorMessage(e.getMessage());
        }

        webhookEventRepository.save(event);
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    private void dispatch(KiwifyWebhookPayload payload) {
        String type = payload.getWebhookEventType();
        switch (type) {
            case AppConstants.EVENT_ORDER_APPROVED,
                 AppConstants.EVENT_SUBSCRIPTION_ACTIVE -> handleApproved(payload);
            case AppConstants.EVENT_SUBSCRIPTION_RENEWED  -> handleRenewed(payload);
            case AppConstants.EVENT_SUBSCRIPTION_CANCELED -> {
                if ("refund_requested".equals(payload.getOrderStatus())
                        || "refunded".equals(payload.getOrderStatus())) {
                    handleRefunded(payload);
                } else {
                    handleCanceled(payload);
                }
            }
            case AppConstants.EVENT_ORDER_REFUNDED        -> handleRefunded(payload);
            case AppConstants.EVENT_SUBSCRIPTION_OVERDUE  -> handleOverdue(payload);
            default -> log.warn("Unknown Kiwify event type: {}", type);
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void handleApproved(KiwifyWebhookPayload payload) {
        CustomerData customerData = requireCustomer(payload);
        User user = findOrCreateUser(customerData);

        String kiwifySubId = resolveSubscriptionId(payload);

        // 1. Já existe assinatura com esse ID kiwify → ativa/estende
        Optional<Subscription> byKiwifyId = subscriptionRepository.findByKiwifySubscriptionId(kiwifySubId);
        if (byKiwifyId.isPresent()) {
            activateOrExtend(byKiwifyId.get());
            return;
        }

        // 2. Usuário criado manualmente tem assinatura PENDING sem kiwifySubscriptionId
        //    → aproveita ela em vez de criar uma segunda
        Optional<Subscription> pending = subscriptionRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), SubscriptionStatus.PENDING);
        if (pending.isPresent()) {
            Subscription sub = pending.get();
            LocalDate today = LocalDate.now();
            sub.setKiwifySubscriptionId(kiwifySubId);
            sub.setKiwifyCustomerId(customerData.getId());
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setStartDate(today);
            sub.setEndDate(today.plusDays(appProperties.getSubscriptionDurationDays()));
            sub.setLastPaymentDate(LocalDateTime.now());
            sub.setNextDueDate(today.plusDays(appProperties.getSubscriptionDurationDays()));
            subscriptionRepository.save(sub);
            log.info("Pending subscription activated via webhook: userId={} kiwifySubId={}", user.getId(), kiwifySubId);
            return;
        }

        // 3. Nenhuma assinatura prévia → cria nova
        createSubscription(user, customerData, kiwifySubId);
    }

    private void handleRenewed(KiwifyWebhookPayload payload) {
        String kiwifySubId = resolveSubscriptionId(payload);
        subscriptionRepository.findByKiwifySubscriptionId(kiwifySubId).ifPresentOrElse(sub -> {
            LocalDate base = (sub.getEndDate() != null && sub.getEndDate().isAfter(LocalDate.now()))
                    ? sub.getEndDate() : LocalDate.now();
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setEndDate(base.plusDays(appProperties.getSubscriptionDurationDays()));
            sub.setLastPaymentDate(LocalDateTime.now());
            sub.setNextDueDate(base.plusDays(appProperties.getSubscriptionDurationDays()));
            subscriptionRepository.save(sub);
            log.info("Subscription renewed for kiwifySubId={}, new endDate={}", kiwifySubId, sub.getEndDate());
        }, () -> log.warn("Renewed event received but subscription not found: {}", kiwifySubId));
    }

    private void handleCanceled(KiwifyWebhookPayload payload) {
        String kiwifySubId = resolveSubscriptionId(payload);
        subscriptionRepository.findByKiwifySubscriptionId(kiwifySubId).ifPresentOrElse(sub -> {
            sub.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(sub);
            // endDate is NOT changed — user retains access until the end of the paid period
            log.info("Subscription canceled for kiwifySubId={}, access until={}", kiwifySubId, sub.getEndDate());
        }, () -> log.warn("Canceled event received but subscription not found: {}", kiwifySubId));
    }

    private void handleRefunded(KiwifyWebhookPayload payload) {
        String kiwifySubId = resolveSubscriptionId(payload);
        subscriptionRepository.findByKiwifySubscriptionId(kiwifySubId).ifPresentOrElse(sub -> {
            sub.setStatus(SubscriptionStatus.REFUNDED);
            sub.setEndDate(LocalDate.now().minusDays(1)); // remove access immediately
            subscriptionRepository.save(sub);
            log.info("Subscription refunded, access removed for kiwifySubId={}", kiwifySubId);
        }, () -> log.warn("Refund event received but subscription not found: {}", kiwifySubId));
    }

    private void handleOverdue(KiwifyWebhookPayload payload) {
        String kiwifySubId = resolveSubscriptionId(payload);
        subscriptionRepository.findByKiwifySubscriptionId(kiwifySubId).ifPresentOrElse(sub -> {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            log.info("Subscription expired (overdue) for kiwifySubId={}", kiwifySubId);
        }, () -> log.warn("Overdue event received but subscription not found: {}", kiwifySubId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findOrCreateUser(CustomerData customerData) {
        return userRepository.findByEmail(customerData.getEmail())
                .orElseGet(() -> {
                    String cpfDigits = normalizeCpf(customerData.getCpf());
                    String initialPassword = cpfDigits.length() >= 6 ? cpfDigits.substring(0, 6) : cpfDigits;
                    User user = User.builder()
                            .name(customerData.getFullName())
                            .email(customerData.getEmail())
                            .cpf(cpfDigits)
                            .passwordHash(passwordEncoder.encode(initialPassword))
                            .role(UserRole.SUBSCRIBER)
                            .active(true)
                            .build();
                    User saved = userRepository.save(user);
                    log.info("New subscriber created: email={}", saved.getEmail());
                    return saved;
                });
    }

    private void createSubscription(User user, CustomerData customerData, String kiwifySubId) {
        LocalDate today = LocalDate.now();
        Subscription sub = Subscription.builder()
                .user(user)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(today)
                .endDate(today.plusDays(appProperties.getSubscriptionDurationDays()))
                .lastPaymentDate(LocalDateTime.now())
                .nextDueDate(today.plusDays(appProperties.getSubscriptionDurationDays()))
                .kiwifySubscriptionId(kiwifySubId)
                .kiwifyCustomerId(customerData.getId())
                .build();
        subscriptionRepository.save(sub);
        log.info("Subscription created: userId={}, endDate={}", user.getId(), sub.getEndDate());
    }

    private void activateOrExtend(Subscription sub) {
        if (sub.hasAccess()) {
            return; // already active, do nothing on duplicate approved event
        }
        LocalDate today = LocalDate.now();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(today);
        sub.setEndDate(today.plusDays(appProperties.getSubscriptionDurationDays()));
        sub.setLastPaymentDate(LocalDateTime.now());
        subscriptionRepository.save(sub);
    }

    private CustomerData requireCustomer(KiwifyWebhookPayload payload) {
        if (payload.getCustomer() == null) {
            throw new BusinessException("Webhook payload sem dados do cliente.");
        }
        if (payload.getCustomer().getEmail() == null || payload.getCustomer().getEmail().isBlank()) {
            throw new BusinessException("Webhook payload sem e-mail do cliente.");
        }
        if (payload.getCustomer().getCpf() == null || payload.getCustomer().getCpf().isBlank()) {
            throw new BusinessException("Webhook payload sem CPF do cliente.");
        }
        return payload.getCustomer();
    }

    private String resolveSubscriptionId(KiwifyWebhookPayload payload) {
        if (payload.getSubscriptionId() != null) {
            return payload.getSubscriptionId();
        }
        if (payload.getSubscription() != null && payload.getSubscription().getId() != null) {
            return payload.getSubscription().getId();
        }
        if (payload.getOrderId() != null) {
            return payload.getOrderId();
        }
        throw new BusinessException("Webhook payload sem ID de assinatura ou pedido.");
    }

    private String buildExternalId(KiwifyWebhookPayload payload) {
        String id;
        try {
            id = resolveSubscriptionId(payload);
        } catch (BusinessException e) {
            id = "unknown";
        }
        return payload.getWebhookEventType() + ":" + id;
    }

    private String normalizeCpf(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }

    private KiwifyWebhookPayload parse(String raw) {
        try {
            return objectMapper.readValue(raw, KiwifyWebhookPayload.class);
        } catch (Exception e) {
            throw new BusinessException("Payload de webhook inválido: " + e.getMessage());
        }
    }
}
