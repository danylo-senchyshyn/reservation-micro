package com.bp.payments.api;

import com.bp.payments.api.dto.FailPaymentRequest;
import com.bp.payments.api.dto.PaymentResponse;
import com.bp.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The type Payment controller.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Gets by id.
     *
     * @param id the id
     * @return the by id
     */
    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    /**
     * Gets by reservation id.
     *
     * @param reservationId the reservation id
     * @return the by reservation id
     */
    @GetMapping("/by-reservation/{reservationId}")
    public List<PaymentResponse> getByReservationId(
            @PathVariable Long reservationId
    ) {
        return paymentService.getByReservationId(reservationId);
    }

    /**
     * Confirm response entity.
     *
     * @param id the id
     * @return the response entity
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.confirm(id));
    }

    /**
     * Fail response entity.
     *
     * @param id      the id
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/{id}/fail")
    public ResponseEntity<PaymentResponse> fail(
            @PathVariable Long id,
            @RequestBody FailPaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.fail(id, request.reason()));
    }

    /**
     * Delete all response entity.
     *
     * @return the response entity
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        paymentService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}