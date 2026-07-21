package com.claire.rentpaymentfinancialplatform.reconciliation;

import org.springframework.core.io.Resource;

interface SettlementFileSource {

    Resource resourceFor(String sourceFile);
}
