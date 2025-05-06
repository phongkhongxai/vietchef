package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportHandleRequest {
    private String status;
    private Integer deduction;
    private boolean lockChef;
    private boolean refundBooking;

}
