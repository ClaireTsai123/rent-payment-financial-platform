package com.claire.rentpaymentfinancialplatform.webhook;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProviderWebhookEventRepository extends JpaRepository<ProviderWebhookEvent, UUID>, JpaSpecificationExecutor<ProviderWebhookEvent> {

    Optional<ProviderWebhookEvent> findByProviderAndProviderEventId(String provider, String providerEventId);
}
