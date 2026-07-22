package com.claire.rentpaymentfinancialplatform.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DevBearerTokenAuthenticationFilterTests {

    @Test
    void filterIsNotRegisteredForProductionProfile() {
        new ApplicationContextRunner()
                .withUserConfiguration(DevBearerTokenAuthenticationFilter.class)
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(DevBearerTokenAuthenticationFilter.class));
    }

    @Test
    void filterIsRegisteredForLocalProfile() {
        new ApplicationContextRunner()
                .withUserConfiguration(DevBearerTokenAuthenticationFilter.class)
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).hasSingleBean(DevBearerTokenAuthenticationFilter.class));
    }
}
