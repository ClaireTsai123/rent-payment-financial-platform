package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MoneyMovementStateTransitionService {

    private final MoneyMovementRepository moneyMovementRepository;
    private final MoneyMovementStateHistoryRepository stateHistoryRepository;

    public MoneyMovementStateTransitionService(
            MoneyMovementRepository moneyMovementRepository,
            MoneyMovementStateHistoryRepository stateHistoryRepository
    ) {
        this.moneyMovementRepository = moneyMovementRepository;
        this.stateHistoryRepository = stateHistoryRepository;
    }

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
        stateHistoryRepository.saveAndFlush(new MoneyMovementStateHistory(
                UUID.randomUUID(),
                moneyMovement,
                currentState,
                nextState,
                reason
        ));
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
