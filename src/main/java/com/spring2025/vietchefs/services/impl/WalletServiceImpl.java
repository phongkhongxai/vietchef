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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
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
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public void createWallet(Long userId, String walletType) {
        if (walletRepository.existsByUserId(userId)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Wallet already exists for this user!");
        }
        if (walletType == null || (!walletType.equalsIgnoreCase("customer") && !walletType.equalsIgnoreCase("chef"))) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Wallet type must be either 'customer' or 'chef'");
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
        if (newType == null || (!newType.equalsIgnoreCase("customer") && !newType.equalsIgnoreCase("chef"))) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Wallet type must be either 'customer' or 'chef'");
        }
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));

        wallet.setWalletType(newType);
        walletRepository.save(wallet);
    }

    @Override
    public WalletDto updateEmailPaypalForWallet(Long userId, String email) {
        if (email == null || !isValidEmail(email)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));

        wallet.setPaypalAccountEmail(email);
        walletRepository.save(wallet);
        return modelMapper.map(wallet, WalletDto.class);
    }

    @Override
    public WalletDto setPasswordForWallet(Long userId, String password) {
        if (password == null || !password.matches("\\d{4}")) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Password must be exactly 4 digits.");
        }
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));
        wallet.setPassword(passwordEncoder.encode(password));
        walletRepository.save(wallet);
        return modelMapper.map(wallet, WalletDto.class);
    }




    @Override
    public WalletPlusResponse getWalletByUserIdAll(Long userId,int pageNo, int pageSize, String sortBy, String sortDir) {
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

    @Override
    public WalletDto getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));
        return modelMapper.map(wallet, WalletDto.class);
    }

    @Override
    public boolean checkWalletHasPassword(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));
        return wallet.getPassword() != null;
    }

    @Override
    public boolean accessWallet(Long userId, String inputPassword) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + userId));

        if (wallet.getPassword() == null) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Wallet has no password set yet.");
        }
        return passwordEncoder.matches(inputPassword, wallet.getPassword());
    }

    @Override
    public String forgotWalletPassword(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for username: " + username));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user id: " + user.getId()));

        String newRawPassword = generateRandom4DigitCode();
        String hashedPassword = passwordEncoder.encode(newRawPassword);
        wallet.setPassword(hashedPassword);
        walletRepository.save(wallet);
        emailVerificationService.sendWalletPassword(user, newRawPassword);
        return "New password sent you email.";
    }

    private boolean isValidEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(regex);
    }
    private String generateRandom4DigitCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000);
        return String.valueOf(code);
    }
}
