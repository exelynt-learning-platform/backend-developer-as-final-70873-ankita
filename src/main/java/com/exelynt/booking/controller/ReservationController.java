package com.exelynt.booking.controller;

import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationUpdateRequest;
import com.exelynt.booking.model.Reservation;
import com.exelynt.booking.model.Status;
import com.exelynt.booking.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    public ResponseEntity<Reservation> createReservation(@Valid @RequestBody ReservationRequest request, Authentication auth) {
        String currentUserEmail = auth.getName();
        return ResponseEntity.ok(reservationService.createReservation(request, currentUserEmail));
    }

    @GetMapping
    public ResponseEntity<Page<Reservation>> getReservations(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication auth) {

        return ResponseEntity.ok(
                reservationService.getFilteredReservations(status, minPrice, maxPrice, page, size, sortBy, sortDirection, auth));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reservationService.getReservationById(id, auth));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reservation> updateReservation(
            @PathVariable Long id, @Valid @RequestBody ReservationUpdateRequest request, Authentication auth) {
        return ResponseEntity.ok(reservationService.updateReservation(id, request, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id, Authentication auth) {
        reservationService.deleteReservation(id, auth);
        return ResponseEntity.noContent().build();
    }
}