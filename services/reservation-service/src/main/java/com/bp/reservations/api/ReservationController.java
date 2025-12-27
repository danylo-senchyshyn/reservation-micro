package com.bp.reservations.api;

import com.bp.reservations.api.dto.CreateReservationRequest;
import com.bp.reservations.api.dto.ReservationResponse;
import com.bp.reservations.entity.ReservationStatus;
import com.bp.reservations.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(
            @RequestBody @Valid CreateReservationRequest request
    ) {
        return reservationService.createReservation(request);
    }

    // READ
    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable Long id) {
        return reservationService.getById(id);
    }

    @GetMapping
    public List<ReservationResponse> getAll() {
        return reservationService.getAll();
    }

    @GetMapping("/user/{userId}")
    public List<ReservationResponse> getByUser(@PathVariable Long userId) {
        return reservationService.getByUserId(userId);
    }

    // UPDATE
    @PatchMapping("/{id}/status")
    public ReservationResponse updateStatus(
            @PathVariable Long id,
            @RequestBody ReservationStatus status
    ) {
        return reservationService.updateStatus(id, status);
    }

    // DELETE / CANCEL
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        reservationService.cancel(id);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelAll() {
        reservationService.cancelAll();
    }
}