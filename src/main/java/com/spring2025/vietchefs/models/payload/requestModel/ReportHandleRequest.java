package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportHandleRequest {
    private String status;
    private Integer deduction;
    private boolean lockChef;
    private boolean refundBooking;

}
