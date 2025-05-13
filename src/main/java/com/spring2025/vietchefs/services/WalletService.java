package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.WalletDto;
import com.spring2025.vietchefs.models.payload.responseModel.WalletPlusResponse;

public interface WalletService {
    void createWallet(Long userId, String walletType);
    void updateWalletType(Long userId, String newType);
    WalletDto updateEmailPaypalForWallet(Long userId, String email);
    WalletDto setPasswordForWallet(Long userId, String password);
    WalletPlusResponse getWalletByUserIdAll(Long userId,int pageNo, int pageSize, String sortBy, String sortDir);
    WalletDto getWalletByUserId(Long userId);
    boolean checkWalletHasPassword(Long userId);
    boolean accessWallet(Long userId, String inputPassword);
    String forgotWalletPassword(String username);
}
