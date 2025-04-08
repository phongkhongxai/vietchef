package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.services.impl.PaypalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    @Autowired
    private PaypalService paypalService;

    // Tạo thanh toán
    @PostMapping("/create")
    public Mono<ResponseEntity<String>> createPayment(
            @RequestParam BigDecimal amount,
            @RequestParam String currency,
            @RequestParam Long bookingId) {

        String returnUrl = "http://35.240.147.10/api/v1/payment/success";
        String cancelUrl = "http://35.240.147.10/api/v1/payment/cancel";
        return paypalService.createPayment(amount, currency, bookingId, returnUrl, cancelUrl)
                .map(orderId -> ResponseEntity.ok(orderId))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Error: " + e.getMessage())));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @PostMapping("/deposit")
    public Mono<String> depositToWallet(@RequestParam Long walletId,
                                        @RequestParam BigDecimal amount) {
        String returnUrl = "http://35.240.147.10/api/v1/payment/success";
        String cancelUrl = "http://35.240.147.10/api/v1/payment/cancel";
        String currency = "USD";
        return paypalService.depositToWallet(walletId, amount, currency, returnUrl, cancelUrl);
    }

    // Hoàn tất thanh toán
    @GetMapping("/success")
    public Mono<ResponseEntity<String>> capturePayment(@RequestParam("token") String orderId) {
        return paypalService.completeDeposit(orderId)
                .then(Mono.just(ResponseEntity.ok("Thanh toán thành công")))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Error: " + e.getMessage())));
    }

    @GetMapping("/cancel")
    public Mono<ResponseEntity<String>> cancelPayment(@RequestParam("token") String orderId) {
        return paypalService.cancelPayment(orderId)
                .map(ResponseEntity::ok)
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
