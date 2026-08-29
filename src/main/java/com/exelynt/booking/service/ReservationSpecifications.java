package com.exelynt.booking.service;

import com.exelynt.booking.model.Reservation;
import com.exelynt.booking.model.Status;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;

/**
 * Builds the JPA {@link Specification} used to filter reservations.
 * Kept separate from {@link ReservationService} so the filtering rules
 * (in particular, that non-admin users can only ever see their own
 * reservations, regardless of which other filters are supplied) are
 * easy to read, and to unit test, in isolation.
 */
public final class ReservationSpecifications {

    private ReservationSpecifications() {}

    public static Specification<Reservation> withFilters(
            Status status, BigDecimal minPrice, BigDecimal maxPrice, Authentication auth) {

        boolean isAdmin = isAdmin(auth);

        return Specification
                .where(ownedByCurrentUserUnlessAdmin(auth, isAdmin))
                .and(hasStatus(status))
                .and(priceGreaterThanOrEqual(minPrice))
                .and(priceLessThanOrEqual(maxPrice));
    }

    private static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // Non-admin users are always restricted to their own reservations. This predicate is
    // ANDed with every other filter, so a non-admin can never widen a status/price query
    // to see another user's reservations.
    private static Specification<Reservation> ownedByCurrentUserUnlessAdmin(Authentication auth, boolean isAdmin) {
        if (isAdmin) {
            return null; // no restriction for admins
        }
        return (root, query, cb) -> cb.equal(root.get("user").get("email"), auth.getName());
    }

    private static Specification<Reservation> hasStatus(Status status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Reservation> priceGreaterThanOrEqual(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    private static Specification<Reservation> priceLessThanOrEqual(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
