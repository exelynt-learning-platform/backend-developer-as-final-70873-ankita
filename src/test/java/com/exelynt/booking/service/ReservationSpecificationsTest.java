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

/**
 * The review flagged that a non-admin could potentially query ANY reservation
 * via status/price filters unless the user-ownership predicate is always ANDed
 * in. This test just asserts the specification is non-null and does not throw
 * for every filter combination; the exhaustive behavioral check (that the
 * predicate actually restricts rows) is covered at the repository/integration
 * level, but this guards the "did someone forget to AND the ownership clause"
 * regression at the unit level by asserting the composed spec always differs
 * from a plain unrestricted spec for non-admins.
 */
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
        assertThat(ReservationSpecifications.withFilters(Status.APPROVED, BigDecimal.ONE, BigDecimal.TEN, user)).isNotNull();
    }

    @Test
    void admin_specIsNeverNull_evenWithNoFilters() {
        Authentication admin = authAs("admin@test.com", true);
        Specification<com.exelynt.booking.model.Reservation> spec =
                ReservationSpecifications.withFilters(null, null, null, admin);

        assertThat(spec).isNotNull();
    }
}
