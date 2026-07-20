package com.claire.rentpaymentfinancialplatform.webhook;

import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderWebhookEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "provider_webhook_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_provider_webhook_events_provider_event",
                columnNames = {"provider", "provider_event_id"}
        )
)
public class ProviderWebhookEvent {

    @Id
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String provider;

    @NotBlank
    @Column(name = "provider_event_id", nullable = false, length = 160)
    private String providerEventId;

    @NotBlank
    @Column(name = "provider_transaction_id", nullable = false, length = 160)
    private String providerTransactionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "normalized_status", nullable = false, length = 30)
    private ProviderTransactionStatus normalizedStatus;

    @NotBlank
    @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
    private String rawPayload;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ProviderWebhookEventStatus processingStatus;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected ProviderWebhookEvent() {
    }

    public ProviderWebhookEvent(
            UUID id,
            String provider,
            String providerEventId,
            String providerTransactionId,
            ProviderTransactionStatus normalizedStatus,
            String rawPayload,
            Instant occurredAt
    ) {
        this.id = id;
        this.provider = provider;
        this.providerEventId = providerEventId;
        this.providerTransactionId = providerTransactionId;
        this.normalizedStatus = normalizedStatus;
        this.rawPayload = rawPayload;
        this.occurredAt = occurredAt;
        this.processingStatus = ProviderWebhookEventStatus.RECEIVED;
    }

    @PrePersist
    void prePersist() {
        receivedAt = Instant.now();
    }

    public void applied() {
        processingStatus = ProviderWebhookEventStatus.APPLIED;
        processedAt = Instant.now();
    }

    public void unmatched(String failureReason) {
        processingStatus = ProviderWebhookEventStatus.UNMATCHED;
        this.failureReason = failureReason;
        processedAt = Instant.now();
    }

    public void ignored(String failureReason) {
        processingStatus = ProviderWebhookEventStatus.IGNORED;
        this.failureReason = failureReason;
        processedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public ProviderTransactionStatus getNormalizedStatus() {
        return normalizedStatus;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public ProviderWebhookEventStatus getProcessingStatus() {
        return processingStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
