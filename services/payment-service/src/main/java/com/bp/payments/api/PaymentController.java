package com.bp.payments.api;

import com.bp.payments.api.dto.CreatePaymentRequest;
import com.bp.payments.api.dto.PaymentResponse;
import com.bp.payments.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@RequestBody @Valid CreatePaymentRequest request) {
        return paymentService.create(request);
    }

    @GetMapping("/reservation/{reservationId}")
    public List<PaymentResponse> getByReservation(@PathVariable Long reservationId) {
        return paymentService.getByReservationId(reservationId);
    }

    @PatchMapping("/{id}/confirm")
    public PaymentResponse confirm(@PathVariable Long id) {
        return paymentService.confirm(id);
    }

    @PatchMapping("/{id}/fail")
    public PaymentResponse fail(
            @PathVariable Long id,
            @RequestParam String reason
    ) {
        return paymentService.fail(id, reason);
    }
}