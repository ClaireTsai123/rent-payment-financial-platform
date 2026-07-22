package com.claire.rentpaymentfinancialplatform.devsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LocalDemoDataSeederProfileTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(PaymentPlanRepository.class, () -> mock(PaymentPlanRepository.class))
            .withBean(MoneyMovementRepository.class, () -> mock(MoneyMovementRepository.class))
            .withUserConfiguration(LocalDemoDataSeeder.class);

    @Test
    void seederIsRegisteredForLocalProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).hasSingleBean(LocalDemoDataSeeder.class));
    }

    @Test
    void seederIsRegisteredForDevProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).hasSingleBean(LocalDemoDataSeeder.class));
    }

    @Test
    void seederIsNotRegisteredForProductionProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(LocalDemoDataSeeder.class));
    }
}
