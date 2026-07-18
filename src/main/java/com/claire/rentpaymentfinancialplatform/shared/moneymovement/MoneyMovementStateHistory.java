package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "money_movement_state_history")
public class MoneyMovementStateHistory {

    @Id
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_movement_id", nullable = false)
    private MoneyMovement moneyMovement;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 30)
    private MoneyMovementState fromState;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 30)
    private MoneyMovementState toState;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected MoneyMovementStateHistory() {
    }

    public MoneyMovementStateHistory(
            UUID id,
            MoneyMovement moneyMovement,
            MoneyMovementState fromState,
            MoneyMovementState toState,
            String reason
    ) {
        this.id = id;
        this.moneyMovement = moneyMovement;
        this.fromState = fromState;
        this.toState = toState;
        this.reason = reason;
    }

    @PrePersist
    void prePersist() {
        changedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public MoneyMovement getMoneyMovement() {
        return moneyMovement;
    }

    public MoneyMovementState getFromState() {
        return fromState;
    }

    public MoneyMovementState getToState() {
        return toState;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
