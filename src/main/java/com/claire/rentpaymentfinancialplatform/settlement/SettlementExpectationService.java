package com.claire.rentpaymentfinancialplatform.settlement;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.SettlementStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransaction;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SettlementExpectationService {

    private static final BigDecimal ZERO_FEE = new BigDecimal("0.00");

    private final ProviderTransactionRepository providerTransactionRepository;
    private final SettlementRecordRepository settlementRecordRepository;

    public SettlementExpectationService(
            ProviderTransactionRepository providerTransactionRepository,
            SettlementRecordRepository settlementRecordRepository
    ) {
        this.providerTransactionRepository = providerTransactionRepository;
        this.settlementRecordRepository = settlementRecordRepository;
    }

    public void recordExpectedSettlementIfSucceeded(MoneyMovement moneyMovement) {
        if (moneyMovement.getState() != MoneyMovementState.SUCCEEDED) {
            return;
        }
        if (settlementRecordRepository.findByMoneyMovementId(moneyMovement.getId()).isPresent()) {
            return;
        }

        ProviderTransaction providerTransaction = providerTransactionRepository
                .findLatestByMoneyMovementId(moneyMovement.getId())
                .orElse(null);
        if (providerTransaction == null) {
            return;
        }

        settlementRecordRepository.saveAndFlush(new SettlementRecord(
                UUID.randomUUID(),
                moneyMovement,
                providerTransaction,
                SettlementStatus.EXPECTED,
                moneyMovement.getAmount(),
                ZERO_FEE,
                moneyMovement.getAmount().subtract(ZERO_FEE),
                moneyMovement.getCurrency(),
                LocalDate.now().plusDays(1),
                providerTransaction.getProvider(),
                providerTransaction.getProviderTransactionId()
        ));
    }
}
