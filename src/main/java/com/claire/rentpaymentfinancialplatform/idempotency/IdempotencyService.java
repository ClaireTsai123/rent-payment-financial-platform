package com.claire.rentpaymentfinancialplatform.idempotency;

import com.claire.rentpaymentfinancialplatform.shared.domain.IdempotencyStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IdempotencyService {

    private static final Duration DEFAULT_EXPIRATION = Duration.ofHours(24);

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;
    private final ObjectMapper fingerprintObjectMapper;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final Clock clock;

    public IdempotencyService(
            IdempotencyRecordRepository idempotencyRecordRepository,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
        this.fingerprintObjectMapper = objectMapper.copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        this.clock = Clock.systemUTC();
    }

    public IdempotencyRecord startOrReplay(String idempotencyKey, IdempotencyOperation operation, Object request) {
        String operationName = operation.name();
        String requestFingerprint = fingerprint(request);

        return requiresNewTransactionTemplate.execute(status ->
                idempotencyRecordRepository.findByIdempotencyKeyAndOperation(idempotencyKey, operationName)
                        .map(existing -> validateExisting(existing, requestFingerprint))
                        .orElseGet(() -> createOrReadAfterInsertRace(idempotencyKey, operationName, requestFingerprint))
        );
    }

    public <T> T readStoredResponse(IdempotencyRecord record, Class<T> responseType) {
        try {
            return objectMapper.readValue(record.getResponsePayload(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read stored idempotent response.", exception);
        }
    }

    public void complete(IdempotencyRecord record, UUID resourceId, Object response) {
        try {
            record.complete(resourceId, objectMapper.writeValueAsString(response));
            idempotencyRecordRepository.saveAndFlush(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to store idempotent response.", exception);
        }
    }

    public String fingerprintForTesting(Object request) {
        return fingerprint(request);
    }

    private IdempotencyRecord createOrReadAfterInsertRace(
            String idempotencyKey,
            String operationName,
            String requestFingerprint
    ) {
        try {
            return requiresNewTransactionTemplate.execute(status ->
                    idempotencyRecordRepository.saveAndFlush(new IdempotencyRecord(
                            UUID.randomUUID(),
                            idempotencyKey,
                            operationName,
                            requestFingerprint,
                            IdempotencyStatus.IN_PROGRESS,
                            null,
                            Instant.now(clock).plus(DEFAULT_EXPIRATION)
                    ))
            );
        } catch (DataIntegrityViolationException exception) {
            return requiresNewTransactionTemplate.execute(status ->
                    idempotencyRecordRepository.findByIdempotencyKeyAndOperation(idempotencyKey, operationName)
                            .map(existing -> validateExisting(existing, requestFingerprint))
                            .orElseThrow(() -> exception)
            );
        }
    }

    private IdempotencyRecord validateExisting(IdempotencyRecord existing, String requestFingerprint) {
        if (!existing.getExpiresAt().isAfter(Instant.now(clock))) {
            throw new IdempotencyExpiredException(existing.getIdempotencyKey(), existing.getOperation());
        }
        if (!existing.getRequestFingerprint().equals(requestFingerprint)) {
            throw new IdempotencyConflictException("Idempotency key was reused with a different request.");
        }
        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            return existing;
        }
        throw new IdempotencyInProgressException(existing.getIdempotencyKey(), existing.getOperation());
    }

    private String fingerprint(Object request) {
        try {
            JsonNode normalizedRequest = fingerprintObjectMapper.valueToTree(request);
            String canonicalRequest = fingerprintObjectMapper.writeValueAsString(normalizedRequest);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Request cannot be fingerprinted.", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
