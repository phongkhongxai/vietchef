package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.entity.Wallet;
import com.spring2025.vietchefs.models.entity.WalletRequest;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.WalletRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.NotificationRequest;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.repositories.WalletRepository;
import com.spring2025.vietchefs.repositories.WalletRequestRepository;
import com.spring2025.vietchefs.services.WalletRequestService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletRequestServiceImpl implements WalletRequestService {

    private final WalletRequestRepository walletRequestRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final PaypalService paypalService;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;

    @Override
    public WalletRequestDto createWithdrawalRequest(WalletRequestDto dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.valueOf(1.00)) < 0) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Amount must be at least $1.00 to process a payout.");
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"User not found."));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Wallet not found."));

        if (wallet.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance.");
        }
        if (wallet.getPaypalAccountEmail()==null){
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email Paypal cannot empty.");
        }
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(3);

        List<WalletRequest> recentRequests = walletRequestRepository.findRecentRequestsByUserId(user.getId(), cutoffTime);
        if (!recentRequests.isEmpty()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "You can only create one withdrawal request every 3 hours.");
        }
        if(dto.getNote().isEmpty()){
            dto.setNote("Withdrawal from wallet.");
        }
        WalletRequest request = WalletRequest.builder()
                .user(user)
                .requestType("WITHDRAWAL")
                .amount(dto.getAmount())
                .status("PENDING")
                .note(dto.getNote())
                .build();
        WalletRequest saved = walletRequestRepository.save(request);
        return modelMapper.map(saved, WalletRequestDto.class);
    }

    @Override
    public WalletRequestDto approveRequest(Long requestId) {
        WalletRequest request = walletRequestRepository.findById(requestId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Wallet request not found."));

        if (!"PENDING".equals(request.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"Only pending requests can be approved.");
        }

        Wallet wallet = walletRepository.findByUserId(request.getUser().getId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Wallet not found."));

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"Insufficient balance to approve request.");
        }
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        BigDecimal amount = request.getAmount();
        paypalService.createPayout(
                wallet.getId(),
                amount,
                "USD",
                "Withdrawal from VietChefs. Note: "+request.getNote()
        ).block();
        request.setStatus("APPROVED");
        WalletRequest updated = walletRequestRepository.save(request);
        NotificationRequest refundNotification = NotificationRequest.builder()
                .userId(wallet.getUser().getId())
                .title("Withdrawal Request Approved")
                .body("Your request to withdraw " + amount + " USD from your VietChefs Wallet has been successfully processed. Please check your PayPal account for confirmation.")
                .screen("WalletScreen")
                .build();
        notificationService.sendPushNotification(refundNotification);
        return modelMapper.map(updated, WalletRequestDto.class);
    }

    @Override
    public WalletRequestDto rejectRequest(Long requestId, String reason) {
        WalletRequest request = walletRequestRepository.findById(requestId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Wallet request not found."));

        if (!"PENDING".equals(request.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"Only pending requests can be approved.");
        }

        request.setStatus("REJECTED");
        String updatedNote = (request.getNote() == null ? "" : request.getNote() + " | ") + "REJECT: " + reason;
        request.setNote(updatedNote);
        WalletRequest updated = walletRequestRepository.save(request);
        NotificationRequest rejectNotification = NotificationRequest.builder()
                .userId(request.getUser().getId())
                .title("Withdrawal Request Rejected")
                .body("Your request to withdraw " + request.getAmount() + " USD from your VietChefs Wallet has been rejected. Reason: "+reason)
                .screen("WalletScreen")
                .build();

        return modelMapper.map(updated, WalletRequestDto.class);
    }

    @Override
    public List<WalletRequestDto> getRequestsByStatus(String status) {
        return walletRequestRepository.findAllByStatus(status)
                .stream()
                .map(request -> modelMapper.map(request, WalletRequestDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<WalletRequestDto> getAllRequests() {
        return walletRequestRepository.findAll()
                .stream()
                .map(request -> modelMapper.map(request, WalletRequestDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<WalletRequestDto> getAllRequestsOfUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"User not found."));
        return walletRequestRepository.findAllByUser(user)
                .stream()
                .map(request -> modelMapper.map(request, WalletRequestDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public WalletRequestDto getWalletRequestById(Long id) {
        WalletRequest request = walletRequestRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Wallet request not found."));
        return modelMapper.map(request, WalletRequestDto.class);

    }

    @Override
    public String deleteWalletRequestById(Long id) {
        WalletRequest request = walletRequestRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Wallet request not found."));
        walletRequestRepository.delete(request);
        return "Deleted successfully.";

    }
}
