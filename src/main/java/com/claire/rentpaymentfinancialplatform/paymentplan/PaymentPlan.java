package com.claire.rentpaymentfinancialplatform.paymentplan;

import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payment_plans")
public class PaymentPlan {

    @Id
    private UUID id;

    @NotBlank
    @Column(name = "renter_id", nullable = false, length = 80)
    private String renterId;

    @NotBlank
    @Column(name = "billing_obligation_id", nullable = false, unique = true, length = 100)
    private String billingObligationId;

    @NotNull
    @DecimalMin("0.01")
    @Column(name = "rent_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal rentAmount;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "initial_collection_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialCollectionAmount;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "repayment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal repaymentAmount;

    @NotNull
    @Column(name = "rent_due_date", nullable = false)
    private LocalDate rentDueDate;

    @NotNull
    @Column(name = "repayment_due_date", nullable = false)
    private LocalDate repaymentDueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentPlanStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentPlan() {
    }

    public PaymentPlan(
            UUID id,
            String renterId,
            String billingObligationId,
            BigDecimal rentAmount,
            BigDecimal initialCollectionAmount,
            BigDecimal repaymentAmount,
            LocalDate rentDueDate,
            LocalDate repaymentDueDate,
            PaymentPlanStatus status
    ) {
        this.id = id;
        this.renterId = renterId;
        this.billingObligationId = billingObligationId;
        this.rentAmount = rentAmount;
        this.initialCollectionAmount = initialCollectionAmount;
        this.repaymentAmount = repaymentAmount;
        this.rentDueDate = rentDueDate;
        this.repaymentDueDate = repaymentDueDate;
        this.status = status;
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

    public String getRenterId() {
        return renterId;
    }

    public String getBillingObligationId() {
        return billingObligationId;
    }

    public BigDecimal getRentAmount() {
        return rentAmount;
    }

    public BigDecimal getInitialCollectionAmount() {
        return initialCollectionAmount;
    }

    public BigDecimal getRepaymentAmount() {
        return repaymentAmount;
    }

    public LocalDate getRentDueDate() {
        return rentDueDate;
    }

    public LocalDate getRepaymentDueDate() {
        return repaymentDueDate;
    }

    public PaymentPlanStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
