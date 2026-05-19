package com.sosvideos.kiwify.repository;

import com.sosvideos.kiwify.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByProviderAndExternalId(String provider, String externalId);
}
