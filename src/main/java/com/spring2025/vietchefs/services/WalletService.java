package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.WalletDto;
import com.spring2025.vietchefs.models.payload.responseModel.WalletPlusResponse;

public interface WalletService {
    void createWallet(Long userId, String walletType);
    void updateWalletType(Long userId, String newType);
    WalletPlusResponse getWalletByUserId(Long userId,int pageNo, int pageSize, String sortBy, String sortDir);
}
