package com.claire.rentpaymentfinancialplatform.outbox;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.OutboxEventStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventService {

    private static final String MONEY_MOVEMENT_AGGREGATE = "MoneyMovement";
    private static final String STATE_CHANGED_EVENT = "money-movement.state-changed";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void recordMoneyMovementStateChanged(MoneyMovement moneyMovement, MoneyMovementStateHistory history) {
        outboxEventRepository.saveAndFlush(new OutboxEvent(
                UUID.randomUUID(),
                MONEY_MOVEMENT_AGGREGATE,
                moneyMovement.getId(),
                STATE_CHANGED_EVENT,
                payloadFor(moneyMovement, history),
                OutboxEventStatus.PENDING,
                Instant.now()
        ));
    }

    private String payloadFor(MoneyMovement moneyMovement, MoneyMovementStateHistory history) {
        try {
            return objectMapper.writeValueAsString(new MoneyMovementStateChangedPayload(
                    moneyMovement.getId(),
                    moneyMovement.getPaymentPlan().getId(),
                    moneyMovement.getType().name(),
                    stateName(history.getFromState()),
                    history.getToState().name(),
                    history.getReason(),
                    history.getChangedAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event payload.", exception);
        }
    }

    private static String stateName(MoneyMovementState state) {
        return state == null ? null : state.name();
    }

    private record MoneyMovementStateChangedPayload(
            UUID moneyMovementId,
            UUID paymentPlanId,
            String moneyMovementType,
            String fromState,
            String toState,
            String reason,
            Instant changedAt
    ) {
    }
}
