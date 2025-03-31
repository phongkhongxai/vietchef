package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Booking;

public interface PaymentCycleService {
    void createPaymentCycles(Booking booking);
    void updatePaymentCycles(Booking booking);

}
