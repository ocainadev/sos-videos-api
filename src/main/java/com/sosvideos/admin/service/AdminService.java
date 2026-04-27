package com.sosvideos.admin.service;

import com.sosvideos.admin.dto.CreateSubscriberRequest;
import com.sosvideos.admin.dto.CsvImportResult;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

        // Senha inicial = primeiros 6 dígitos do CPF; sem CPF usa "123456"
        String cpfDigits = (request.getCpf() != null) ? request.getCpf().replaceAll("\\D", "") : "";
        String initialPassword = cpfDigits.length() >= 6 ? cpfDigits.substring(0, 6) : "123456";

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

        // Batch-load subscriptions and term acceptances for the current page — avoids N+1 queries
        Set<Long> userIds = page.stream().map(User::getId).collect(Collectors.toSet());
        Map<Long, Subscription> subByUserId = subscriptionRepository.findLatestByUserIds(userIds)
                .stream().collect(Collectors.toMap(s -> s.getUser().getId(), s -> s, (a, b) -> a));
        Set<Long> acceptedTermUserIds = termAcceptanceRepository
                .findUserIdsWithActiveTermAcceptance(userIds);

        return PageResponse.from(page.map(u ->
                toSummary(u, subByUserId.get(u.getId()), acceptedTermUserIds.contains(u.getId()))));
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

    // ── CSV Import ────────────────────────────────────────────────────────────

    @Transactional
    public CsvImportResult importFromCsv(MultipartFile file) {
        int created = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                long line = record.getRecordNumber() + 1;
                try {
                    String email = record.get("Customer Email");
                    if (email == null || email.isBlank()) { skipped++; continue; }

                    String name      = record.get("Customer Name");
                    String cpfRaw    = record.get("Customer CPF / CNPJ");
                    String statusRaw = record.get("Status");
                    String startedAt = record.get("Started At");
                    String endedAt   = record.get("Ended At");
                    String periodEnd = record.get("Current Period End");

                    String cpf = cpfRaw != null ? cpfRaw.replaceAll("[^0-9]", "") : "";
                    // CPF do Kiwify pode ser CNPJ (14 dígitos); guarda só se for CPF válido (11 dígitos)
                    String cpfToSave = cpf.length() == 11 ? cpf : null;

                    SubscriptionStatus status = mapCsvStatus(statusRaw);
                    LocalDate startDate = parseCsvDate(startedAt);
                    // Prefere Current Period End; cai para Ended At se vazio
                    LocalDate endDate = parseCsvDate(!periodEnd.isBlank() ? periodEnd : endedAt);

                    Optional<User> existing = userRepository.findByEmail(email);

                    if (existing.isEmpty()) {
                        String initialPassword = cpf.length() >= 6 ? cpf.substring(0, 6) : email;
                        User user = User.builder()
                                .name(name)
                                .email(email)
                                .cpf(cpfToSave)
                                .passwordHash(passwordEncoder.encode(initialPassword))
                                .role(UserRole.SUBSCRIBER)
                                .active(true)
                                .build();
                        user = userRepository.save(user);

                        subscriptionRepository.save(Subscription.builder()
                                .user(user)
                                .status(status)
                                .startDate(startDate)
                                .endDate(endDate)
                                .build());
                        created++;
                        log.info("CSV import: user created email={}", email);
                    } else {
                        User user = existing.get();
                        Optional<Subscription> existingSub = subscriptionRepository
                                .findTopByUserIdOrderByCreatedAtDesc(user.getId());

                        if (existingSub.isPresent()) {
                            Subscription sub = existingSub.get();
                            sub.setStatus(status);
                            sub.setStartDate(startDate);
                            sub.setEndDate(endDate);
                            subscriptionRepository.save(sub);
                        } else {
                            subscriptionRepository.save(Subscription.builder()
                                    .user(user)
                                    .status(status)
                                    .startDate(startDate)
                                    .endDate(endDate)
                                    .build());
                        }
                        updated++;
                        log.info("CSV import: subscription updated email={}", email);
                    }
                } catch (Exception e) {
                    errors.add("Linha " + line + ": " + e.getMessage());
                    log.warn("CSV import error at line {}: {}", line, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BusinessException("Erro ao processar o arquivo CSV: " + e.getMessage());
        }

        log.info("CSV import finished: created={} updated={} skipped={} errors={}", created, updated, skipped, errors.size());
        return new CsvImportResult(created, updated, skipped, errors);
    }

    private SubscriptionStatus mapCsvStatus(String status) {
        if (status == null) return SubscriptionStatus.PENDING;
        return switch (status.toLowerCase().trim()) {
            case "active"              -> SubscriptionStatus.ACTIVE;
            case "canceled",
                 "cancelled"          -> SubscriptionStatus.CANCELED;
            case "refunded"           -> SubscriptionStatus.REFUNDED;
            case "expired"            -> SubscriptionStatus.EXPIRED;
            default                   -> SubscriptionStatus.PENDING;
        };
    }

    private LocalDate parseCsvDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            // Kiwify format: "2026-04-27 16:20" — take date portion only
            return LocalDate.parse(dateStr.trim().substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Single-user lookup — used when fetching or creating an individual subscriber. */
    private SubscriberSummaryResponse toSummary(User user) {
        Optional<Subscription> sub = subscriptionRepository
                .findTopByUserIdOrderByCreatedAtDesc(user.getId());
        boolean hasAcceptedTerm = termAcceptanceRepository
                .findActiveTermAcceptanceByUserId(user.getId()).isPresent();
        return toSummary(user, sub.orElse(null), hasAcceptedTerm);
    }

    /** Batch-aware overload — accepts pre-fetched data to avoid N+1. */
    private SubscriberSummaryResponse toSummary(User user, Subscription sub, boolean hasAcceptedTerm) {
        return new SubscriberSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                Boolean.TRUE.equals(user.getActive()),
                user.getCreatedAt(),
                sub != null ? sub.getId() : null,
                sub != null ? sub.getStatus().name() : null,
                sub != null ? sub.getStartDate() : null,
                sub != null ? sub.getEndDate() : null,
                sub != null ? sub.getLastPaymentDate() : null,
                sub != null ? sub.getKiwifySubscriptionId() : null,
                sub != null ? sub.getKiwifyCustomerId() : null,
                hasAcceptedTerm
        );
    }
}
