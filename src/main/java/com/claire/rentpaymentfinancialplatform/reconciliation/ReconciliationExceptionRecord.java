package com.claire.rentpaymentfinancialplatform.reconciliation;

import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "reconciliation_exceptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reconciliation_exceptions_run_provider_reference_type",
                columnNames = {"reconciliation_run_id", "provider", "provider_transaction_reference", "exception_type"}
        )
)
public class ReconciliationExceptionRecord {

    @Id
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliation_run_id", nullable = false)
    private ReconciliationRun reconciliationRun;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 40)
    private ReconciliationExceptionType exceptionType;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String provider;

    @NotBlank
    @Column(name = "provider_transaction_reference", nullable = false, length = 160)
    private String providerTransactionReference;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String message;

    @NotBlank
    @Column(name = "raw_record", nullable = false, columnDefinition = "text")
    private String rawRecord;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReconciliationExceptionRecord() {
    }

    public ReconciliationExceptionRecord(
            UUID id,
            ReconciliationRun reconciliationRun,
            ReconciliationExceptionType exceptionType,
            String provider,
            String providerTransactionReference,
            String message,
            String rawRecord
    ) {
        this.id = id;
        this.reconciliationRun = reconciliationRun;
        this.exceptionType = exceptionType;
        this.provider = provider;
        this.providerTransactionReference = providerTransactionReference;
        this.message = message;
        this.rawRecord = rawRecord;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ReconciliationRun getReconciliationRun() {
        return reconciliationRun;
    }

    public ReconciliationExceptionType getExceptionType() {
        return exceptionType;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderTransactionReference() {
        return providerTransactionReference;
    }

    public String getMessage() {
        return message;
    }

    public String getRawRecord() {
        return rawRecord;
    }
}
