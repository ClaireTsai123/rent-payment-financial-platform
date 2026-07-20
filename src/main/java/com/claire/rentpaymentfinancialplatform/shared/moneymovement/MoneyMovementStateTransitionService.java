package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEventService;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementExpectationService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoneyMovementStateTransitionService {

    private final MoneyMovementRepository moneyMovementRepository;
    private final MoneyMovementStateHistoryRepository stateHistoryRepository;
    private final OutboxEventService outboxEventService;
    private final SettlementExpectationService settlementExpectationService;

    public MoneyMovementStateTransitionService(
            MoneyMovementRepository moneyMovementRepository,
            MoneyMovementStateHistoryRepository stateHistoryRepository,
            OutboxEventService outboxEventService,
            SettlementExpectationService settlementExpectationService
    ) {
        this.moneyMovementRepository = moneyMovementRepository;
        this.stateHistoryRepository = stateHistoryRepository;
        this.outboxEventService = outboxEventService;
        this.settlementExpectationService = settlementExpectationService;
    }

    @Transactional
    public MoneyMovementStateTransitionResult transition(MoneyMovement moneyMovement, MoneyMovementState nextState, String reason) {
        MoneyMovementState currentState = moneyMovement.getState();
        if (currentState == nextState) {
            return MoneyMovementStateTransitionResult.noOpTransition();
        }
        if (!isValidTransition(currentState, nextState)) {
            return MoneyMovementStateTransitionResult.rejectedTransition(
                    "Invalid money movement transition from " + currentState + " to " + nextState + "."
            );
        }

        moneyMovement.transitionTo(nextState);
        moneyMovementRepository.saveAndFlush(moneyMovement);
        MoneyMovementStateHistory history = stateHistoryRepository.saveAndFlush(new MoneyMovementStateHistory(
                UUID.randomUUID(),
                moneyMovement,
                currentState,
                nextState,
                reason
        ));
        outboxEventService.recordMoneyMovementStateChanged(moneyMovement, history);
        settlementExpectationService.recordExpectedSettlementIfSucceeded(moneyMovement);
        return MoneyMovementStateTransitionResult.appliedTransition();
    }

    private static boolean isValidTransition(MoneyMovementState currentState, MoneyMovementState nextState) {
        return switch (currentState) {
            case CREATED -> nextState == MoneyMovementState.SUBMITTED
                    || nextState == MoneyMovementState.PROCESSING
                    || nextState == MoneyMovementState.SUCCEEDED
                    || nextState == MoneyMovementState.FAILED;
            case SUBMITTED -> nextState == MoneyMovementState.PROCESSING
                    || nextState == MoneyMovementState.SUCCEEDED
                    || nextState == MoneyMovementState.FAILED;
            case PROCESSING -> nextState == MoneyMovementState.SUCCEEDED
                    || nextState == MoneyMovementState.FAILED
                    || nextState == MoneyMovementState.RETURNED
                    || nextState == MoneyMovementState.REVERSED;
            case SUCCEEDED -> nextState == MoneyMovementState.RETURNED
                    || nextState == MoneyMovementState.REVERSED;
            case FAILED, RETURNED, REVERSED -> false;
        };
    }
}
