package com.spring2025.vietchefs.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaypalService{
    @Value("${paypal.client.id}")
    private String CLIENT_ID;

    @Value("${paypal.client.secret}")
    private String CLIENT_SECRET;

    @Value("${paypal.mode}")
    private String MODE;

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WebClient webClient; // Inject WebClient từ Spring

    @Autowired
    private ObjectMapper objectMapper; // Inject Jackson ObjectMapper
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;
    @Autowired
    private ChefTransactionRepository chefTransactionRepository;

    private final String BASE_URL = "https://api-m.sandbox.paypal.com";
    private final String PAYPAL_CHECKOUT_URL = "https://www.sandbox.paypal.com/checkoutnow?token="; // URL redirect
    // Lấy Access Token từ PayPal
    private Mono<String> getAccessToken() {
        return webClient.post()
                .uri(BASE_URL + "/v1/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " +
                        java.util.Base64.getEncoder().encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=client_credentials")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(response);
                        return "Bearer " + jsonNode.get("access_token").asText();
                    } catch (Exception e) {
                        throw new VchefApiException(HttpStatus.BAD_REQUEST,"Failed to parse access token:"+e);
                    }
                });
    }

    // 1. Tạo thanh toán (v2/checkout/orders)
    public Mono<String> createPayment(BigDecimal amount, String currency, Long bookingId, String returnUrl, String cancelUrl) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be greater than zero"));
        }
        BigDecimal formattedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return Mono.error(new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found by id: " + bookingId));
        }
        Booking booking = bookingOpt.get();

        if (!"PENDING".equals(booking.getStatus())) {
            return Mono.error(new VchefApiException(HttpStatus.BAD_REQUEST, "Booking must be in PENDING status to initiate payment"));
        }
        return getAccessToken()
                .flatMap(token -> {
                    String jsonBody = "{"
                            + "\"intent\":\"CAPTURE\","
                            + "\"purchase_units\":[{"
                            + "\"amount\":{\"currency_code\":\"" + currency + "\",\"value\":\"" + formattedAmount.toString() + "\"},"
                            + "\"description\":\"Thanh toán cho booking #" + booking.getId() + "\""
                            + "}],"
                            + "\"payment_source\":{\"paypal\":{"
                            + "\"experience_context\":{"
                            + "\"return_url\":\"" + returnUrl + "\","
                            + "\"cancel_url\":\"" + cancelUrl + "\""
                            + "}}}"
                            + "}";

                    return webClient.post()
                            .uri(BASE_URL + "/v2/checkout/orders")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(jsonBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(response -> {
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(response);
                                    String orderId = jsonNode.get("id").asText();

                                    // Lưu vào DB
                                    Payment paymentEntity = new Payment();
                                    paymentEntity.setTransactionId(orderId);
                                    paymentEntity.setPaymentMethod("PayPal");
                                    paymentEntity.setAmount(amount);
                                    paymentEntity.setCurrency(currency);
                                    paymentEntity.setStatus("PENDING");
                                    paymentEntity.setPaymentType("PAYMENT");
                                    paymentEntity.setIsDeleted(false);
                                    paymentRepository.save(paymentEntity);

                                    return PAYPAL_CHECKOUT_URL + orderId;
                                } catch (Exception e) {
                                    booking.setStatus("PENDING");
                                    bookingRepository.save(booking);
                                    throw new VchefApiException(HttpStatus.BAD_REQUEST,"Failed to parse order ID: "+ e);
                                }
                            })
                            .onErrorResume(e -> {
                                booking.setStatus("PENDING");
                                bookingRepository.save(booking);
                                return Mono.error(e);
                            });
                });
    }

    // 2. Hoàn tất thanh toán (Capture order)
    public Mono<Void> capturePayment(String orderId) {
        return getAccessToken()
                .flatMap(token -> webClient.post()
                        .uri(BASE_URL + "/v2/checkout/orders/" + orderId + "/capture")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{}")
                        .retrieve()
                        .bodyToMono(String.class)
                        .map(response -> {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(response);
                                String captureId = jsonNode.get("purchase_units").get(0)
                                        .get("payments").get("captures").get(0).get("id").asText();

                                // Cập nhật DB
                                Optional<Payment> paymentEntity = paymentRepository.findByTransactionId(orderId);
                                if (paymentEntity.isPresent()) {
                                    Payment payment = paymentEntity.get();
                                    payment.setTransactionId(captureId);
                                    payment.setStatus("COMPLETED");
                                    paymentRepository.save(payment);

                                   /* Booking booking = payment.getBooking();
                                    if (booking != null) {
                                        booking.setStatus("PAID");
                                        bookingRepository.save(booking);
                                    }*/
                                }
                                return Mono.empty();
                            } catch (Exception e) {
                                throw new VchefApiException(HttpStatus.BAD_REQUEST,"Failed to parse capture ID: "+e);
                            }
                        })
                ).then();
    }


    public Mono<String> depositToWallet(Long walletId, BigDecimal amount, String currency, String returnUrl, String cancelUrl) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be greater than zero"));
        }

        // Lấy ví theo ID
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found with id: " + walletId));

        return getAccessToken()
                .flatMap(token -> {
                    String jsonBody = "{"
                            + "\"intent\":\"CAPTURE\","
                            + "\"purchase_units\":[{"
                            + "\"amount\":{\"currency_code\":\"" + currency + "\",\"value\":\"" + amount.setScale(2, RoundingMode.HALF_UP) + "\"},"
                            + "\"description\":\"Nạp tiền vào ví VietChefs - Wallet ID: " + walletId + "\""
                            + "}],"
                            + "\"application_context\":{"
                            + "\"return_url\":\"" + returnUrl + "\","
                            + "\"cancel_url\":\"" + cancelUrl + "\""
                            + "}"
                            + "}";
                    System.out.println("Request body gửi đến PayPal: " + jsonBody);
                    return webClient.post()
                            .uri(BASE_URL + "/v2/checkout/orders")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(jsonBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(response -> {
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(response);
                                    String orderId = jsonNode.get("id").asText();

                                    // Lưu thông tin Payment vào DB
                                    Payment payment = new Payment();
                                    payment.setTransactionId(orderId);
                                    payment.setPaymentMethod("PayPal");
                                    payment.setAmount(amount);
                                    payment.setCurrency(currency);
                                    payment.setStatus("PENDING");
                                    payment.setWallet(wallet);
                                    payment.setPaymentType("DEPOSIT");
                                    paymentRepository.save(payment);

                                    return PAYPAL_CHECKOUT_URL + orderId;
                                } catch (Exception e) {
                                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "Failed to create PayPal order: " + e.getMessage());
                                }
                            });
                });
    }

    /**
     * Xử lý hoàn tất thanh toán
     */
    public Mono<Void> completeDeposit(String orderId) {
        return getAccessToken()
                .flatMap(token -> webClient.post()
                        .uri(BASE_URL + "/v2/checkout/orders/" + orderId + "/capture")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{}")
                        .retrieve()
                        .bodyToMono(String.class)
                        .map(response -> {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(response);
                                String captureId = jsonNode.get("purchase_units").get(0)
                                        .get("payments").get("captures").get(0).get("id").asText();
                                BigDecimal amount = new BigDecimal(jsonNode.get("purchase_units").get(0)
                                        .get("payments").get("captures").get(0).get("amount").get("value").asText());



                                // Lưu Payment đã hoàn thành
                                Payment payment = paymentRepository.findByTransactionId(orderId)
                                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Payment not found"));
                                // Cập nhật ví
                                Wallet wallet = walletRepository.findById(payment.getWallet().getId())
                                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
                                payment.setTransactionId(captureId);
                                payment.setStatus("COMPLETED");
                                paymentRepository.save(payment);
                                wallet.setBalance(wallet.getBalance().add(amount));
                                walletRepository.save(wallet);

                                // Lưu Transaction
                                if (wallet.getWalletType().equalsIgnoreCase("CUSTOMER")) {
                                    customerTransactionRepository.save(CustomerTransaction.builder()
                                            .wallet(wallet)
                                            .transactionType("DEPOSIT")
                                            .amount(amount)
                                            .description("Successful deposit via Paypal")
                                            .status("COMPLETED")
                                            .isDeleted(false)
                                            .build());
                                } else {
                                    chefTransactionRepository.save(ChefTransaction.builder()
                                            .wallet(wallet)
                                            .transactionType("DEPOSIT")
                                            .amount(amount)
                                            .description("Successful deposit via Paypal")
                                            .status("COMPLETED")
                                            .isDeleted(false)
                                            .build());
                                }
                                return Mono.empty();
                            } catch (Exception e) {
                                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Lỗi khi xử lý thanh toán: " + e.getMessage());
                            }
                        })
                ).then();
    }
    public Mono<String> cancelPayment(String orderId) {
        return Mono.fromCallable(() -> {
            Optional<Payment> paymentEntity = paymentRepository.findByTransactionId(orderId);
            if (paymentEntity.isPresent()) {
                Payment payment = paymentEntity.get();
                payment.setStatus("FAILED");
                paymentRepository.save(payment);

                return "Thanh toán đã bị hủy";
            } else {
                throw new VchefApiException(HttpStatus.NOT_FOUND,"Payment not found for orderId: " + orderId);
            }
        }).onErrorResume(e -> Mono.error(new VchefApiException(HttpStatus.BAD_REQUEST,"Lỗi khi hủy thanh toán: " + e.getMessage())));
    }

    // 3. Hoàn tiền
    public Mono<Void> refundPayment(String transactionId, BigDecimal amount, String currency) {
        return getAccessToken()
                .flatMap(token -> {
                    String jsonBody = "{"
                            + "\"amount\":{\"currency_code\":\"" + currency + "\",\"value\":\"" + amount + "\"}"
                            + "}";

                    return webClient.post()
                            .uri(BASE_URL + "/v2/payments/captures/" + transactionId + "/refund")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(jsonBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(response -> {
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(response);
                                    String refundId = jsonNode.get("id").asText();

                                    // Lưu refund vào DB
                                    Optional<Payment> paymentEntity = paymentRepository.findByTransactionId(transactionId);
                                    if (paymentEntity.isPresent()){
                                        Payment refundEntity = new Payment();

                                        refundEntity.setTransactionId(refundId);
                                        refundEntity.setPaymentMethod("PayPal");
                                        refundEntity.setAmount(amount);
                                        refundEntity.setCurrency(currency);
                                        refundEntity.setStatus("REFUNDED");
                                        refundEntity.setPaymentType("REFUND");
                                        refundEntity.setRefundReference(transactionId);
                                        refundEntity.setIsDeleted(false);
                                        paymentRepository.save(refundEntity);

                                        Payment mainPayment = paymentEntity.get();
                                        // Cập nhật giao dịch gốc
                                        mainPayment.setStatus("REFUNDED");
                                        paymentRepository.save(mainPayment);
                                    }

                                    return Mono.empty(); // Hoàn tất mà không cần giá trị trả về
                                } catch (Exception e) {
                                    throw new VchefApiException(HttpStatus.BAD_REQUEST,"Failed to parse refund ID:"+e);
                                }
                            });
                }).then();
    }

    // 4. Chi trả
    public Mono<String> createPayout(Long walletId, BigDecimal amount, String currency, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be greater than zero"));
        }

        // Lấy ví theo ID
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found with id: " + walletId));

        // Kiểm tra số dư
        if (wallet.getBalance().compareTo(amount) < 0) {
            return Mono.error(new VchefApiException(HttpStatus.BAD_REQUEST, "Số dư trong ví không đủ"));
        }
        Optional<Payment> payment = paymentRepository.findTopByWalletAndPaymentTypeOrderByCreatedAtDesc(wallet, "PAYOUT");
        if (payment.isPresent() && payment.get().getCreatedAt() != null) {
            // Kiểm tra xem thời gian tạo giao dịch có trong vòng 24 giờ qua không
            LocalDateTime createdAt = payment.get().getCreatedAt();
            if (createdAt.isAfter(LocalDateTime.now().minusDays(1))) {
                return Mono.error(new VchefApiException(HttpStatus.BAD_REQUEST, "Bạn không thể thực hiện rút tiền trong vòng 24 giờ sau lần rút gần nhất."));
            }
        }
        if (wallet.getPaypalAccountEmail()==null){
            return Mono.error(new VchefApiException(HttpStatus.BAD_REQUEST, "Email Paypal đang null."));

        }
        String receiverEmail = wallet.getPaypalAccountEmail();

        return getAccessToken()
                .flatMap(token -> {
                    String jsonBody = "{"
                            + "\"sender_batch_header\":{"
                            + "\"sender_batch_id\":\"PAYOUT_" + System.currentTimeMillis() + "\","
                            + "\"email_subject\":\"Bạn đã nhận được một khoản chi trả từ VietChefs\""
                            + "},"
                            + "\"items\":[{"
                            + "\"recipient_type\":\"EMAIL\","
                            + "\"receiver\":\"" + receiverEmail + "\","
                            + "\"amount\":{\"value\":\"" + amount + "\",\"currency\":\"" + currency + "\"},"
                            + "\"note\":\"" + note + "\","
                            + "\"sender_item_id\":\"ITEM_" + System.currentTimeMillis() + "\""
                            + "}]"
                            + "}";

                    return webClient.post()
                            .uri(BASE_URL + "/v1/payments/payouts")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(jsonBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .flatMap(response -> {
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(response);

                                    if (jsonNode.has("name") && jsonNode.has("message")) {
                                        String errorName = jsonNode.get("name").asText();
                                        String errorMessage = jsonNode.get("message").asText();

                                        if ("RECEIVER_UNREGISTERED".equals(errorName)) {
                                            return Mono.error(new VchefApiException(HttpStatus.BAD_REQUEST, "Email chưa đăng ký tài khoản PayPal."));
                                        }

                                        // Các lỗi khác
                                        return Mono.error(new VchefApiException(HttpStatus.BAD_REQUEST, "PayPal payout error: " + errorMessage));
                                    }

                                    // ✅ Xử lý thành công
                                    String payoutId = jsonNode.get("batch_header").get("payout_batch_id").asText();

                                    Payment payoutEntity = new Payment();
                                    payoutEntity.setTransactionId(payoutId);
                                    payoutEntity.setPaymentMethod("PayPal");
                                    payoutEntity.setAmount(amount);
                                    payoutEntity.setCurrency(currency);
                                    payoutEntity.setStatus("COMPLETED");
                                    payoutEntity.setPaymentType("PAYOUT");
                                    payoutEntity.setIsDeleted(false);
                                    payoutEntity.setWallet(wallet);
                                    paymentRepository.save(payoutEntity);
                                    wallet.setBalance(wallet.getBalance().subtract(amount));
                                    walletRepository.save(wallet);

                                    // Lưu Transaction
                                    if (wallet.getWalletType().equalsIgnoreCase("CUSTOMER")) {
                                        customerTransactionRepository.save(CustomerTransaction.builder()
                                                .wallet(wallet)
                                                .transactionType("WITHDRAWL")
                                                .amount(amount)
                                                .description("Successful withdrawal")
                                                .status("COMPLETED")
                                                .isDeleted(false)
                                                .build());
                                    } else {
                                        chefTransactionRepository.save(ChefTransaction.builder()
                                                .wallet(wallet)
                                                .transactionType("WITHDRAWL")
                                                .amount(amount)
                                                .description("Successful withdrawal")
                                                .status("COMPLETED")
                                                .isDeleted(false)
                                                .build());
                                    }

                                    return Mono.just("Payout created successfully with Payout ID: " + payoutId);
                                } catch (Exception e) {
                                    return Mono.error(new VchefApiException(HttpStatus.BAD_REQUEST, "Lỗi xử lý response từ PayPal: " + e.getMessage()));
                                }
                            });

                });
    }

}
