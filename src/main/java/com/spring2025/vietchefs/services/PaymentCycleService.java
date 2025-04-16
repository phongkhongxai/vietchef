package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.payload.responseModel.PaymentCycleResponseDto;

public interface PaymentCycleService {
    void createPaymentCycles(Booking booking);
    void updatePaymentCycles(Booking booking);
    PaymentCycleResponseDto cancelPaymentCycle(Long paymentCycleId);


}
