package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.entity.CustomerTransaction;
import com.spring2025.vietchefs.models.payload.dto.ChefTransactionDto;
import com.spring2025.vietchefs.models.payload.dto.CustomerTransactionDto;
import com.spring2025.vietchefs.models.payload.dto.WalletDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WalletPlusResponse {
    private WalletDto wallet;
    private CustomerTransactionsResponse customerTransactions;
    private ChefTransactionsResponse chefTransactions;
}
