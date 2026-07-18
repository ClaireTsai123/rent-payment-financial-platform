package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_transactions")
public class ProviderTransaction {

    @Id
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_movement_id", nullable = false)
    private MoneyMovement moneyMovement;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_attempt_id", nullable = false)
    private PaymentAttempt paymentAttempt;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String provider;

    @NotBlank
    @Column(name = "provider_transaction_id", nullable = false, length = 160)
    private String providerTransactionId;

    @Column(name = "provider_idempotency_key", length = 160)
    private String providerIdempotencyKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "normalized_status", nullable = false, length = 30)
    private ProviderTransactionStatus normalizedStatus;

    @Column(name = "raw_status", length = 120)
    private String rawStatus;

    @Column(name = "settlement_reference", length = 160)
    private String settlementReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProviderTransaction() {
    }

    public ProviderTransaction(
            UUID id,
            MoneyMovement moneyMovement,
            PaymentAttempt paymentAttempt,
            String provider,
            String providerTransactionId,
            String providerIdempotencyKey,
            ProviderTransactionStatus normalizedStatus,
            String rawStatus
    ) {
        this.id = id;
        this.moneyMovement = moneyMovement;
        this.paymentAttempt = paymentAttempt;
        this.provider = provider;
        this.providerTransactionId = providerTransactionId;
        this.providerIdempotencyKey = providerIdempotencyKey;
        this.normalizedStatus = normalizedStatus;
        this.rawStatus = rawStatus;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public MoneyMovement getMoneyMovement() {
        return moneyMovement;
    }

    public PaymentAttempt getPaymentAttempt() {
        return paymentAttempt;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getProviderIdempotencyKey() {
        return providerIdempotencyKey;
    }

    public ProviderTransactionStatus getNormalizedStatus() {
        return normalizedStatus;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    public String getSettlementReference() {
        return settlementReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
