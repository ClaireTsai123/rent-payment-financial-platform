package com.claire.rentpaymentfinancialplatform.reconciliation;

import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "reconciliation_runs",
        uniqueConstraints = @UniqueConstraint(name = "uk_reconciliation_runs_source_file", columnNames = "source_file")
)
public class ReconciliationRun {

    @Id
    private UUID id;

    @NotBlank
    @Column(name = "source_file", nullable = false, length = 500)
    private String sourceFile;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReconciliationRunStatus status;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "matched_rows", nullable = false)
    private int matchedRows;

    @Column(name = "exception_rows", nullable = false)
    private int exceptionRows;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReconciliationRun() {
    }

    public ReconciliationRun(UUID id, String sourceFile) {
        this.id = id;
        this.sourceFile = sourceFile;
        this.status = ReconciliationRunStatus.STARTED;
        this.startedAt = Instant.now();
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

    public void complete(int totalRows, int matchedRows, int exceptionRows) {
        this.status = ReconciliationRunStatus.COMPLETED;
        this.totalRows = totalRows;
        this.matchedRows = matchedRows;
        this.exceptionRows = exceptionRows;
        this.completedAt = Instant.now();
        this.failureReason = null;
    }

    public void restart() {
        this.status = ReconciliationRunStatus.STARTED;
        this.startedAt = Instant.now();
        this.completedAt = null;
        this.failureReason = null;
    }

    public void fail(String failureReason) {
        this.status = ReconciliationRunStatus.FAILED;
        this.failureReason = failureReason;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public ReconciliationRunStatus getStatus() {
        return status;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getMatchedRows() {
        return matchedRows;
    }

    public int getExceptionRows() {
        return exceptionRows;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
