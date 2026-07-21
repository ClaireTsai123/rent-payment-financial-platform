package com.claire.rentpaymentfinancialplatform.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
class ProviderSettlementFileParser {

    private static final String HEADER = "provider,providerTransactionReference,grossAmount,feeAmount,netAmount,currency,settlementDate,providerBatchReference";

    void validateHeader(String header) {
        if (!HEADER.equals(header)) {
            throw new IllegalArgumentException("Settlement file header is invalid.");
        }
    }

    ProviderSettlementFileRow parseLine(String line) {
        if (line.isBlank()) {
            return ProviderSettlementFileRow.blank(line);
        }
        String[] values = line.split(",", -1);
        if (values.length != 8) {
            throw new IllegalArgumentException("Settlement file row has invalid column count: " + line);
        }
        return new ProviderSettlementFileRow(
                values[0].trim(),
                values[1].trim(),
                new BigDecimal(values[2].trim()),
                new BigDecimal(values[3].trim()),
                new BigDecimal(values[4].trim()),
                values[5].trim(),
                LocalDate.parse(values[6].trim()),
                values[7].trim(),
                line,
                false
        );
    }
}
