package com.bp.payments.api;

import com.bp.payments.api.dto.FailPaymentRequest;
import com.bp.payments.api.dto.PaymentResponse;
import com.bp.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    @GetMapping("/by-reservation/{reservationId}")
    public List<PaymentResponse> getByReservationId(
            @PathVariable Long reservationId
    ) {
        return paymentService.getByReservationId(reservationId);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.confirm(id));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<PaymentResponse> fail(
            @PathVariable Long id,
            @RequestBody FailPaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.fail(id, request.reason()));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        paymentService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}