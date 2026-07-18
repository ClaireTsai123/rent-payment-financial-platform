package com.claire.rentpaymentfinancialplatform.idempotency;

import com.claire.rentpaymentfinancialplatform.shared.domain.IdempotencyStatus;
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
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idempotency_records_key_operation",
                columnNames = {"idempotency_key", "operation"}
        )
)
public class IdempotencyRecord {

    @Id
    private UUID id;

    @NotBlank
    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String operation;

    @NotBlank
    @Column(name = "request_fingerprint", nullable = false, length = 128)
    private String requestFingerprint;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IdempotencyStatus status;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "response_payload", columnDefinition = "text")
    private String responsePayload;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(
            UUID id,
            String idempotencyKey,
            String operation,
            String requestFingerprint,
            IdempotencyStatus status,
            UUID resourceId,
            Instant expiresAt
    ) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.operation = operation;
        this.requestFingerprint = requestFingerprint;
        this.status = status;
        this.resourceId = resourceId;
        this.expiresAt = expiresAt;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getOperation() {
        return operation;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void complete(UUID resourceId, String responsePayload) {
        this.resourceId = resourceId;
        this.responsePayload = responsePayload;
        this.status = IdempotencyStatus.COMPLETED;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
