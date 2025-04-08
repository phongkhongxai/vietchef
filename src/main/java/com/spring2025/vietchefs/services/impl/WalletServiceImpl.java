package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.entity.Wallet;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.WalletDto;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.repositories.WalletRepository;
import com.spring2025.vietchefs.services.WalletService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public void createWallet(Long userId, String walletType) {
        if (walletRepository.existsByUserId(userId)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Wallet already exists for this user!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        //wallet.setPaypalAccountEmail(user.getEmail()); // Liên kết email PayPal
        wallet.setWalletType(walletType); // CUSTOMER hoặc CHEF

        walletRepository.save(wallet);
    }

    @Override
    public void updateWalletType(Long userId, String newType) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));

        wallet.setWalletType(newType);
        walletRepository.save(wallet);
    }

    @Override
    public WalletDto getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));

        return modelMapper.map(wallet, WalletDto.class);
    }
}
