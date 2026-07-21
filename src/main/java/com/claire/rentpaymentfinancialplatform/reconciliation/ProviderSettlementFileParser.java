package com.claire.rentpaymentfinancialplatform.reconciliation;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class ProviderSettlementFileParser {

    private static final String HEADER = "provider,providerTransactionReference,grossAmount,feeAmount,netAmount,currency,settlementDate,providerBatchReference";

    List<ProviderSettlementFileRow> parse(String sourceFile) {
        try {
            List<String> lines = Files.readAllLines(Path.of(sourceFile));
            if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
                throw new IllegalArgumentException("Settlement file header is invalid.");
            }
            List<ProviderSettlementFileRow> rows = new ArrayList<>();
            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);
                if (line.isBlank()) {
                    continue;
                }
                rows.add(parseLine(line));
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Settlement file cannot be read: " + sourceFile, exception);
        }
    }

    private static ProviderSettlementFileRow parseLine(String line) {
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
                line
        );
    }
}
