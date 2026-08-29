package com.exelynt.booking.service;

import com.exelynt.booking.model.Status;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationSpecificationsTest {

    private Authentication authAs(String email, boolean admin) {
        String role = admin ? "ROLE_ADMIN" : "ROLE_USER";
        return new UsernamePasswordAuthenticationToken(email, "n/a", List.of(new SimpleGrantedAuthority(role)));
    }

    @Test
    void nonAdmin_specIsNeverNull_regardlessOfWhichFiltersAreSupplied() {
        Authentication user = authAs("owner@test.com", false);

        assertThat(ReservationSpecifications.withFilters(null, null, null, user)).isNotNull();
        assertThat(ReservationSpecifications.withFilters(Status.PENDING, null, null, user)).isNotNull();
        assertThat(ReservationSpecifications.withFilters(null, BigDecimal.ONE, BigDecimal.TEN, user)).isNotNull();
        assertThat(ReservationSpecifications.withFilters(Status.CONFIRMED, BigDecimal.ONE, BigDecimal.TEN, user)).isNotNull();
    }

    @Test
    void admin_specIsNeverNull_evenWithNoFilters() {
        Authentication admin = authAs("admin@test.com", true);
        Specification<com.exelynt.booking.model.Reservation> spec =
                ReservationSpecifications.withFilters(null, null, null, admin);

        assertThat(spec).isNotNull();
    }
}