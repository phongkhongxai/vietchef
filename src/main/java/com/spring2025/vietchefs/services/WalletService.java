package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.WalletDto;

public interface WalletService {
    void createWallet(Long userId, String walletType);
    void updateWalletType(Long userId, String newType);
    WalletDto getWalletByUserId(Long userId);
}
