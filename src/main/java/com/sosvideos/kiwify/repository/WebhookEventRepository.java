package com.sosvideos.kiwify.repository;

import com.sosvideos.kiwify.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByProviderAndExternalId(String provider, String externalId);

    Optional<WebhookEvent> findByProviderAndExternalId(String provider, String externalId);
}
