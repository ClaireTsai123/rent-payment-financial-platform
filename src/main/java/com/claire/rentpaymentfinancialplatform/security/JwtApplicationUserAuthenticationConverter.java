package com.claire.rentpaymentfinancialplatform.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;

public class JwtApplicationUserAuthenticationConverter
        implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    private static final Set<String> SUPPORTED_ROLES = Set.of("RENTER", "SUPPORT", "FINOPS", "ADMIN");

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        String subject = firstNonBlank(jwt.getSubject(), jwt.getClaimAsString("sub"));
        String renterId = firstNonBlank(jwt.getClaimAsString("renter_id"), jwt.getClaimAsString("renterId"));
        List<SimpleGrantedAuthority> authorities = extractRoles(jwt).stream()
                .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                .filter(SUPPORTED_ROLES::contains)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        ApplicationUser user = new ApplicationUser(subject, renterId, authorities);
        return new ApplicationUserJwtAuthenticationToken(jwt, user, authorities);
    }

    private static Set<String> extractRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        addClaimValues(roles, jwt.getClaim("roles"));
        addClaimValues(roles, jwt.getClaim("authorities"));
        addScopeValues(roles, jwt.getClaim("scope"));
        addClaimValues(roles, jwt.getClaim("scp"));
        return roles;
    }

    private static void addClaimValues(Set<String> roles, Object claim) {
        if (claim instanceof Collection<?> values) {
            values.stream().flatMap(JwtApplicationUserAuthenticationConverter::splitRoleValue).forEach(roles::add);
        } else if (claim != null) {
            splitRoleValue(claim).forEach(roles::add);
        }
    }

    private static void addScopeValues(Set<String> roles, Object claim) {
        addClaimValues(roles, claim);
    }

    private static Stream<String> splitRoleValue(Object value) {
        return Stream.of(String.valueOf(value).split("[ ,]"))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toUpperCase);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return "unknown-subject";
    }

    private static final class ApplicationUserJwtAuthenticationToken extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

        private ApplicationUserJwtAuthenticationToken(
                Jwt jwt,
                ApplicationUser principal,
                Collection<? extends GrantedAuthority> authorities
        ) {
            super(jwt, principal, jwt, authorities);
            setAuthenticated(true);
        }

        @Override
        public ApplicationUser getPrincipal() {
            return (ApplicationUser) super.getPrincipal();
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Map<String, Object> getTokenAttributes() {
            return getToken().getClaims();
        }
    }
}
