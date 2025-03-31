package com.spring2025.vietchefs.services;

public interface WalletService {
    void createWallet(Long userId, String walletType);
    void updateWalletType(Long userId, String newType);
}
