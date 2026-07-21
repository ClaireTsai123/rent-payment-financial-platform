package com.claire.rentpaymentfinancialplatform.reconciliation;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
class LocalSettlementFileSource implements SettlementFileSource {

    @Override
    public Resource resourceFor(String sourceFile) {
        return new FileSystemResource(sourceFile);
    }
}
