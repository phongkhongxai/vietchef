package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.WalletRequest;
import com.spring2025.vietchefs.models.payload.dto.WalletRequestDto;

import java.math.BigDecimal;
import java.util.List;

public interface WalletRequestService {
    WalletRequestDto createWithdrawalRequest(WalletRequestDto dto);
    WalletRequestDto approveRequest(Long requestId);
    WalletRequestDto rejectRequest(Long requestId, String reason);
    List<WalletRequestDto> getRequestsByStatus(String status);
    List<WalletRequestDto> getAllRequests();
    List<WalletRequestDto> getAllRequestsOfUser(Long userId);
    WalletRequestDto getWalletRequestById(Long id);
    String deleteWalletRequestById(Long id);

}
