package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.CustomerTransactionDto;
import com.spring2025.vietchefs.models.payload.dto.DishDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerTransactionsResponse {
    private List<CustomerTransactionDto> content;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
