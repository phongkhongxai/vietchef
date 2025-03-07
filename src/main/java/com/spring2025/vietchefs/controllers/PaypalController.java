package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.Payment;
import com.spring2025.vietchefs.services.BookingService;
import com.spring2025.vietchefs.services.impl.PaypalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/paypal")
public class PaypalController {

    @Autowired
    private PaypalService paypalService;

    // Tạo thanh toán
    @PostMapping("/create")
    public Mono<ResponseEntity<String>> createPayment(
            @RequestParam BigDecimal amount,
            @RequestParam String currency,
            @RequestParam Long bookingId) {

        String returnUrl = "http://localhost:8080/api/v1/paypal/success";
        String cancelUrl = "http://localhost:8080/api/v1/paypal/cancel";
        return paypalService.createPayment(amount, currency, bookingId, returnUrl, cancelUrl)
                .map(orderId -> ResponseEntity.ok(orderId))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Error: " + e.getMessage())));
    }

    // Hoàn tất thanh toán
    @GetMapping("/success")
    public Mono<ResponseEntity<String>> capturePayment(@RequestParam("token") String orderId) {
        return paypalService.capturePayment(orderId)
                .then(Mono.just(ResponseEntity.ok("Thanh toán thành công")))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Error: " + e.getMessage())));
    }

    @GetMapping("/cancel")
    public Mono<ResponseEntity<String>> cancelPayment(@RequestParam("token") String orderId) {
        return paypalService.cancelPayment(orderId)
                .map(message -> ResponseEntity.ok(message))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    // Hoàn tiền

    @PostMapping("/refund")
    public Mono<ResponseEntity<String>> refundPayment(
            @RequestParam String transactionId,
            @RequestParam BigDecimal amount,
            @RequestParam String currency) {
        return paypalService.refundPayment(transactionId, amount, currency)
                .then(Mono.just(ResponseEntity.ok("Hoàn tiền thành công")))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Error: " + e.getMessage())));
    }

    // Chi trả
    @PostMapping("/payout")
    public Mono<ResponseEntity<String>> createPayout(
            @RequestParam String receiverEmail,
            @RequestParam BigDecimal amount,
            @RequestParam String currency,
            @RequestParam String note) {
        return paypalService.createPayout(receiverEmail, amount, currency, note)
                .then(Mono.just(ResponseEntity.ok("Chi trả thành công")))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Error: " + e.getMessage())));
    }
}
