package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;

import java.util.List;

@Data
public class ChefPackageRequestDto {
    private Long chefId;
    private List<Long> packageIds;
}
