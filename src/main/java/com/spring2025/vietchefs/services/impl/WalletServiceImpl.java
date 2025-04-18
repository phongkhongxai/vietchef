package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.ChefTransactionDto;
import com.spring2025.vietchefs.models.payload.dto.CustomerTransactionDto;
import com.spring2025.vietchefs.models.payload.dto.WalletDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefTransactionsResponse;
import com.spring2025.vietchefs.models.payload.responseModel.CustomerTransactionsResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.models.payload.responseModel.WalletPlusResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.WalletService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ChefTransactionRepository chefTransactionRepository;
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
    public WalletDto updateEmailPaypalForWallet(Long userId, String email) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));

        wallet.setPaypalAccountEmail(email);
        walletRepository.save(wallet);
        return modelMapper.map(wallet, WalletDto.class);
    }

    @Override
    public WalletPlusResponse getWalletByUserId(Long userId,int pageNo, int pageSize, String sortBy, String sortDir) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));

        WalletPlusResponse response = new WalletPlusResponse();
        response.setWallet(modelMapper.map(wallet, WalletDto.class));

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        if (wallet.getWalletType().equalsIgnoreCase("CUSTOMER")) {
            Page<CustomerTransaction> txPage = customerTransactionRepository.findByWalletAndIsDeletedFalse(wallet, pageable);
            List<CustomerTransactionDto> dtos = txPage.getContent().stream()
                    .map(tx -> modelMapper.map(tx, CustomerTransactionDto.class))
                    .collect(Collectors.toList());

            CustomerTransactionsResponse templatesResponse = new CustomerTransactionsResponse();
            templatesResponse.setContent(dtos);
            templatesResponse.setPageNo(txPage.getNumber());
            templatesResponse.setPageSize(txPage.getSize());
            templatesResponse.setTotalElements(txPage.getTotalElements());
            templatesResponse.setTotalPages(txPage.getTotalPages());
            templatesResponse.setLast(txPage.isLast());
            response.setCustomerTransactions(templatesResponse);
        } else if (wallet.getWalletType().equalsIgnoreCase("CHEF")) {
            Page<ChefTransaction> txPage = chefTransactionRepository.findByWalletAndIsDeletedFalse(wallet, pageable);
            List<ChefTransactionDto> dtos = txPage.getContent().stream()
                    .map(tx -> modelMapper.map(tx, ChefTransactionDto.class))
                    .collect(Collectors.toList());

            ChefTransactionsResponse templatesResponse = new ChefTransactionsResponse();
            templatesResponse.setContent(dtos);
            templatesResponse.setPageNo(txPage.getNumber());
            templatesResponse.setPageSize(txPage.getSize());
            templatesResponse.setTotalElements(txPage.getTotalElements());
            templatesResponse.setTotalPages(txPage.getTotalPages());
            templatesResponse.setLast(txPage.isLast());
            response.setChefTransactions(templatesResponse);
        }
        return response;
    }
}
