package com.exelynt.booking.service;

import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationUpdateRequest;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.model.Reservation;
import com.exelynt.booking.model.Resource;
import com.exelynt.booking.model.Status;
import com.exelynt.booking.model.User;
import com.exelynt.booking.repository.ReservationRepository;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              UserRepository userRepository,
                              ResourceRepository resourceRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
    }

    public Reservation createReservation(ReservationRequest request, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setPrice(request.getPrice());
        reservation.setStatus(request.getStatus() != null ? request.getStatus() : Status.PENDING);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());

        return reservationRepository.save(reservation);
    }

    public Page<Reservation> getFilteredReservations(
            Status status, BigDecimal minPrice, BigDecimal maxPrice,
            int page, int size, String sortBy, String sortDirection, Authentication auth) {

        Sort sort = buildSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);

        return reservationRepository.findAll(
                ReservationSpecifications.withFilters(status, minPrice, maxPrice, auth), pageable);
    }

    public Reservation getReservationById(Long id, Authentication auth) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnerOrAdmin(reservation, auth);
        return reservation;
    }

    public Reservation updateReservation(Long id, ReservationUpdateRequest request, Authentication auth) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnerOrAdmin(reservation, auth);

        // Partial update: only fields the caller actually supplied are applied,
        // so an update that omits a field never wipes out existing data.
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }
        if (request.getPrice() != null) {
            reservation.setPrice(request.getPrice());
        }
        if (request.getStartTime() != null) {
            reservation.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            reservation.setEndTime(request.getEndTime());
        }

        return reservationRepository.save(reservation);
    }

    public void deleteReservation(Long id, Authentication auth) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnerOrAdmin(reservation, auth);
        reservationRepository.delete(reservation);
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void assertOwnerOrAdmin(Reservation reservation, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = reservation.getUser().getEmail().equals(auth.getName());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to access this reservation");
        }
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        String property = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "id";
        // Default stays descending (most recent first), matching prior behavior when
        // no direction is specified. Callers can opt into ascending explicitly.
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
