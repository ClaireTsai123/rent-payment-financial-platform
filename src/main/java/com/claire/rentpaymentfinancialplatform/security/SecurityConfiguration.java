package com.claire.rentpaymentfinancialplatform.security;

import com.claire.rentpaymentfinancialplatform.shared.api.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.core.convert.converter.Converter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    @Profile({"local", "dev", "test"})
    SecurityFilterChain localSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            DevBearerTokenAuthenticationFilter devBearerTokenAuthenticationFilter
    ) throws Exception {
        configureStatelessApiSecurity(http, objectMapper)
                .addFilterBefore(devBearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Profile("!local & !dev & !test")
    SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> jwtAuthenticationConverter
    ) throws Exception {
        configureStatelessApiSecurity(http, objectMapper)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> jwtAuthenticationConverter() {
        return new JwtApplicationUserAuthenticationConverter();
    }

    private static HttpSecurity configureStatelessApiSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Access is denied."))
                )
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/actuator/health"),
                                AntPathRequestMatcher.antMatcher("/api/v1/provider-webhooks/mock-provider")
                        ).permitAll()
                        .anyRequest().authenticated()
                );
    }

    private static void writeError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            String code,
            String message
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(code, message, Instant.now()));
    }
}
