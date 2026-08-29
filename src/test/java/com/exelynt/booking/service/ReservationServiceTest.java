package com.exelynt.booking.service;

import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationUpdateRequest;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.model.*;
import com.exelynt.booking.repository.ReservationRepository;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReservationService, focused on the two areas the code review
 * flagged: non-admin users must never be able to see or modify someone else's
 * reservation, and updates must be partial (never null out untouched fields).
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ResourceRepository resourceRepository;

    private ReservationService reservationService;

    private User owner;
    private User otherUser;
    private Resource resource;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, userRepository, resourceRepository);

        owner = new User(1L, "owner@test.com", "hashed", Role.ROLE_USER);
        otherUser = new User(2L, "other@test.com", "hashed", Role.ROLE_USER);
        resource = new Resource(10L, "Room A", "desc", true);

        reservation = new Reservation();
        reservation.setId(100L);
        reservation.setUser(owner);
        reservation.setResource(resource);
        reservation.setPrice(new BigDecimal("50.00"));
        reservation.setStatus(Status.PENDING);
    }

    private Authentication authAs(String email, boolean admin) {
        String role = admin ? "ROLE_ADMIN" : "ROLE_USER";
        return new UsernamePasswordAuthenticationToken(email, "n/a", List.of(new SimpleGrantedAuthority(role)));
    }

    @Test
    void createReservation_defaultsStatusToPending_whenNotProvided() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(10L);
        request.setPrice(new BigDecimal("25.00"));

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.createReservation(request, "owner@test.com");

        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
        assertThat(result.getUser()).isEqualTo(owner);
        assertThat(result.getResource()).isEqualTo(resource);
    }

    @Test
    void createReservation_throwsNotFound_whenResourceMissing() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(999L);
        request.setPrice(BigDecimal.TEN);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(request, "owner@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReservation_appliesOnlySuppliedFields() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationUpdateRequest update = new ReservationUpdateRequest();
        update.setStatus(Status.CONFIRMED); // only status supplied; price/startTime/endTime left null

        Reservation result = reservationService.updateReservation(100L, update, authAs("owner@test.com", false));

        assertThat(result.getStatus()).isEqualTo(Status.CONFIRMED);
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("50.00")); // untouched
    }

    @Test
    void updateReservation_allowsOwner() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationUpdateRequest update = new ReservationUpdateRequest();
        update.setPrice(new BigDecimal("75.00"));

        Reservation result = reservationService.updateReservation(100L, update, authAs("owner@test.com", false));

        assertThat(result.getPrice()).isEqualTo(new BigDecimal("75.00"));
    }

    @Test
    void updateReservation_allowsAdmin_evenIfNotOwner() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationUpdateRequest update = new ReservationUpdateRequest();
        update.setStatus(Status.CANCELLED);

        Reservation result = reservationService.updateReservation(100L, update, authAs("admin@test.com", true));

        assertThat(result.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void updateReservation_rejectsNonOwnerNonAdmin() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ReservationUpdateRequest update = new ReservationUpdateRequest();
        update.setStatus(Status.CANCELLED);

        assertThatThrownBy(() ->
                reservationService.updateReservation(100L, update, authAs("other@test.com", false)))
                .isInstanceOf(AccessDeniedException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateReservation_throwsNotFound_whenReservationMissing() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        ReservationUpdateRequest update = new ReservationUpdateRequest();
        assertThatThrownBy(() -> reservationService.updateReservation(999L, update, authAs("owner@test.com", false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteReservation_allowsOwner() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        reservationService.deleteReservation(100L, authAs("owner@test.com", false));

        verify(reservationRepository).delete(reservation);
    }

    @Test
    void deleteReservation_rejectsNonOwnerNonAdmin() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.deleteReservation(100L, authAs("other@test.com", false)))
                .isInstanceOf(AccessDeniedException.class);

        verify(reservationRepository, never()).delete(any(Reservation.class));
    }

    @Test
    void getReservationById_rejectsNonOwnerNonAdmin() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationById(100L, authAs("other@test.com", false)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getFilteredReservations_delegatesToRepositoryWithSpecificationAndPageable() {
        Page<Reservation> page = new PageImpl<>(List.of(reservation));
        when(reservationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Reservation> result = reservationService.getFilteredReservations(
                Status.PENDING, null, null, 0, 10, "price", "asc", authAs("owner@test.com", false));

        assertThat(result.getContent()).containsExactly(reservation);
        verify(reservationRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "price"))));
    }
}