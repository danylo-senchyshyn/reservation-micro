package com.bp.reservations.api;

import com.bp.reservations.api.dto.CreateReservationRequest;
import com.bp.reservations.api.dto.ReservationResponse;
import com.bp.reservations.entity.ReservationStatus;
import com.bp.reservations.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The type Reservation controller.
 */
@Tag(name = "Reservation Management", description = "Operations related to booking reservations")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Create reservation response.
     *
     * @param request the request
     * @return the reservation response
     */
// CREATE
    @Operation(summary = "Create a new reservation")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(
            @RequestBody @Valid CreateReservationRequest request
    ) {
        return reservationService.createReservation(request);
    }

    /**
     * Gets by id.
     *
     * @param id the id
     * @return the by id
     */
// READ
    @Operation(summary = "Get reservation by ID")
    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable Long id) {
        return reservationService.getById(id);
    }

    /**
     * Gets all.
     *
     * @return the all
     */
    @Operation(summary = "Get all reservations")
    @GetMapping
    public List<ReservationResponse> getAll() {
        return reservationService.getAll();
    }

    /**
     * Gets by user.
     *
     * @param userId the user id
     * @return the by user
     */
    @Operation(summary = "Get reservations by User ID")
    @GetMapping("/user/{userId}")
    public List<ReservationResponse> getByUser(@PathVariable Long userId) {
        return reservationService.getByUserId(userId);
    }

    /**
     * Update status reservation response.
     *
     * @param id     the id
     * @param status the status
     * @return the reservation response
     */
// UPDATE
    @Operation(summary = "Update reservation status by ID")
    @PatchMapping("/{id}/status")
    public ReservationResponse updateStatus(
            @PathVariable Long id,
            @RequestBody ReservationStatus status
    ) {
        return reservationService.updateStatus(id, status);
    }

    /**
     * Cancel.
     *
     * @param id the id
     */
// DELETE
    @Operation(summary = "Cancel a reservation by ID")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        reservationService.cancel(id);
    }

    /**
     * Cancel all.
     */
    @Operation(summary = "Cancel all reservations (for admin/testing)")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelAll() {
        reservationService.cancelAll();
    }
}