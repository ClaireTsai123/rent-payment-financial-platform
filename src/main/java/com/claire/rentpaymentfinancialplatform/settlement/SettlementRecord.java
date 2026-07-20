package com.claire.rentpaymentfinancialplatform.settlement;

import com.claire.rentpaymentfinancialplatform.shared.domain.SettlementStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "settlement_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_settlement_records_money_movement", columnNames = "money_movement_id"),
                @UniqueConstraint(name = "uk_settlement_records_provider_transaction", columnNames = "provider_transaction_id")
        }
)
public class SettlementRecord {

    @Id
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_movement_id", nullable = false)
    private MoneyMovement moneyMovement;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_transaction_id", nullable = false)
    private ProviderTransaction providerTransaction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementStatus status;

    @NotNull
    @Column(name = "expected_gross_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedGrossAmount;

    @NotNull
    @Column(name = "expected_fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedFeeAmount;

    @NotNull
    @Column(name = "expected_net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedNetAmount;

    @Column(name = "actual_gross_amount", precision = 19, scale = 2)
    private BigDecimal actualGrossAmount;

    @Column(name = "actual_fee_amount", precision = 19, scale = 2)
    private BigDecimal actualFeeAmount;

    @Column(name = "actual_net_amount", precision = 19, scale = 2)
    private BigDecimal actualNetAmount;

    @NotBlank
    @Column(nullable = false, length = 3)
    private String currency;

    @NotNull
    @Column(name = "expected_settlement_date", nullable = false)
    private LocalDate expectedSettlementDate;

    @Column(name = "actual_settlement_date")
    private LocalDate actualSettlementDate;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String provider;

    @NotBlank
    @Column(name = "provider_transaction_reference", nullable = false, length = 160)
    private String providerTransactionReference;

    @Column(name = "provider_batch_reference", length = 160)
    private String providerBatchReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SettlementRecord() {
    }

    public SettlementRecord(
            UUID id,
            MoneyMovement moneyMovement,
            ProviderTransaction providerTransaction,
            SettlementStatus status,
            BigDecimal expectedGrossAmount,
            BigDecimal expectedFeeAmount,
            BigDecimal expectedNetAmount,
            String currency,
            LocalDate expectedSettlementDate,
            String provider,
            String providerTransactionReference
    ) {
        this.id = id;
        this.moneyMovement = moneyMovement;
        this.providerTransaction = providerTransaction;
        this.status = status;
        this.expectedGrossAmount = expectedGrossAmount;
        this.expectedFeeAmount = expectedFeeAmount;
        this.expectedNetAmount = expectedNetAmount;
        this.currency = currency;
        this.expectedSettlementDate = expectedSettlementDate;
        this.provider = provider;
        this.providerTransactionReference = providerTransactionReference;
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

    public UUID getMoneyMovementId() {
        return moneyMovement.getId();
    }

    public ProviderTransaction getProviderTransaction() {
        return providerTransaction;
    }

    public UUID getProviderTransactionId() {
        return providerTransaction.getId();
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public BigDecimal getExpectedGrossAmount() {
        return expectedGrossAmount;
    }

    public BigDecimal getExpectedFeeAmount() {
        return expectedFeeAmount;
    }

    public BigDecimal getExpectedNetAmount() {
        return expectedNetAmount;
    }

    public BigDecimal getActualGrossAmount() {
        return actualGrossAmount;
    }

    public BigDecimal getActualFeeAmount() {
        return actualFeeAmount;
    }

    public BigDecimal getActualNetAmount() {
        return actualNetAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getExpectedSettlementDate() {
        return expectedSettlementDate;
    }

    public LocalDate getActualSettlementDate() {
        return actualSettlementDate;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderTransactionReference() {
        return providerTransactionReference;
    }

    public String getProviderBatchReference() {
        return providerBatchReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
