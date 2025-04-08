package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.entity.PaymentCycle;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.responseModel.PaymentCycleResponseDto;
import com.spring2025.vietchefs.repositories.BookingDetailRepository;
import com.spring2025.vietchefs.repositories.BookingRepository;
import com.spring2025.vietchefs.repositories.PaymentCycleRepository;
import com.spring2025.vietchefs.services.PaymentCycleService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentCycleServiceImpl implements PaymentCycleService {
    @Autowired
    private PaymentCycleRepository paymentCycleRepository;
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public void createPaymentCycles(Booking booking) {
        List<BookingDetail> details = bookingDetailRepository.findByBookingOrderBySessionDateAsc(booking);
        int numOfCycles = getNumOfCycles(booking);

        int detailsPerCycle = (int) Math.ceil((double) details.size() / numOfCycles);
        LocalDate firstPaymentDate = booking.getCreatedAt().toLocalDate().plusDays(1);

        for (int i = 0; i < numOfCycles; i++) {
            int startIdx = i * detailsPerCycle;
            int endIdx = Math.min(startIdx + detailsPerCycle, details.size());

            if (startIdx >= details.size()) break; // Nếu không còn BookingDetail nào thì dừng

            LocalDate startDate = details.get(startIdx).getSessionDate();
            LocalDate endDate = details.get(endIdx - 1).getSessionDate();

            // 🔹 Tính tổng tiền thực tế của các `BookingDetail` trong kỳ này
            BigDecimal adjustedCycleAmount = details.subList(startIdx, endIdx).stream()
                    .map(BookingDetail::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PaymentCycle paymentCycle = new PaymentCycle();
            paymentCycle.setBooking(booking);
            paymentCycle.setCycleOrder(i + 1);
            paymentCycle.setAmountDue(adjustedCycleAmount); //  Số tiền thực tế
            paymentCycle.setStartDate(startDate);
            paymentCycle.setEndDate(endDate);
            paymentCycle.setDueDate(startDate.minusDays(2)); // Hạn thanh toán trước 2 ngày
            paymentCycle.setStatus("PENDING");

            paymentCycleRepository.save(paymentCycle);
        }
    }

    @Override
    public void updatePaymentCycles(Booking booking) {
        List<BookingDetail> details = bookingDetailRepository.findByBookingOrderBySessionDateAsc(booking);
        List<PaymentCycle> paymentCycles = paymentCycleRepository.findByBookingOrderByCycleOrder(booking);

        int numOfCycles = paymentCycles.size();
        int detailsPerCycle = (int) Math.ceil((double) details.size() / numOfCycles);

        for (int i = 0; i < numOfCycles; i++) {
            int startIdx = i * detailsPerCycle;
            int endIdx = Math.min(startIdx + detailsPerCycle, details.size());

            if (startIdx >= details.size()) break;

            LocalDate newStartDate = details.get(startIdx).getSessionDate();
            LocalDate newEndDate = details.get(endIdx - 1).getSessionDate();
            BigDecimal newAmountDue = details.subList(startIdx, endIdx).stream()
                    .map(BookingDetail::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PaymentCycle paymentCycle = paymentCycles.get(i);

            if (!"PAID".equals(paymentCycle.getStatus())) {
                paymentCycle.setStartDate(newStartDate);
                paymentCycle.setEndDate(newEndDate);
                paymentCycle.setAmountDue(newAmountDue);
                paymentCycle.setDueDate(newStartDate.minusDays(2));
                paymentCycleRepository.save(paymentCycle);
            }
        }
    }

    @Override
    @Transactional
    public PaymentCycleResponseDto cancelPaymentCycle(Long paymentCycleId) {
        PaymentCycle cycle = paymentCycleRepository.findById(paymentCycleId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Payment cycle not found"));

        if ("PAID".equalsIgnoreCase(cycle.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot cancel a payment cycle that has already been PAID.");
        }

        if (!cycle.getDueDate().isAfter(LocalDate.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot cancel a payment cycle that has already started.");
        }
        if(cycle.getCycleOrder()==1){
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot cancel the first payment cycle.");
        }

        cycle.setStatus("CANCELED");
        paymentCycleRepository.save(cycle);

        Booking booking = cycle.getBooking();
        BigDecimal refundAmount = cycle.getAmountDue();

        // Cập nhật totalPrice của booking (giảm đi số tiền bị hủy)
        booking.setTotalPrice(booking.getTotalPrice().subtract(refundAmount));

        List<PaymentCycle> futureCycles = paymentCycleRepository.findByBookingId(booking.getId())
                .stream()
                .filter(c -> c.getDueDate().isAfter(cycle.getDueDate())) // Chỉ lấy các kỳ sau kỳ hiện tại
                .toList();
        // Nếu có futureCycles, lấy ngày kết thúc của cycle cuối cùng
        LocalDate endDateLastCycle = futureCycles.isEmpty() ? cycle.getEndDate() :
                futureCycles.stream().max(Comparator.comparing(PaymentCycle::getEndDate)).get().getEndDate();

        // Tính tổng số tiền bị hủy từ các kỳ thanh toán trong tương lai
        BigDecimal totalFutureRefund = futureCycles.stream()
                .map(PaymentCycle::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Cập nhật status và giảm totalPrice một lần duy nhất
        for (PaymentCycle futureCycle : futureCycles) {
            futureCycle.setStatus("CANCELED");
            paymentCycleRepository.save(futureCycle);
        }
        booking.setTotalPrice(booking.getTotalPrice().subtract(totalFutureRefund));
        booking.setStatus("CONFIRM_PAID");
        bookingRepository.save(booking);

        // Lọc danh sách BookingDetail có sessionDate nằm trong khoảng từ startDate đến endDate
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBookingId(booking.getId())
                .stream()
                .filter(detail -> !detail.getSessionDate().isBefore(cycle.getStartDate())
                        && !detail.getSessionDate().isAfter(endDateLastCycle))
                .toList();

        if (!bookingDetails.isEmpty()) {
            for (BookingDetail detail : bookingDetails) {
                detail.setTotalPrice(BigDecimal.ZERO);
                detail.setStatus("CANCELED");
                bookingDetailRepository.save(detail);
            }
        }

        return modelMapper.map(cycle, PaymentCycleResponseDto.class);
    }

    private int getNumOfCycles(Booking booking) {
        int durationDays = booking.getBookingPackage().getDurationDays();
        int numOfCycles;

        // 🔹 Xác định số kỳ thanh toán dựa trên thời gian của package
        if (durationDays <= 7) {
            numOfCycles = 1; // Thanh toán 1 lần
        } else if (durationDays <= 14) {
            numOfCycles = 2; // Thanh toán 2 lần
        } else if (durationDays <= 30) {
            numOfCycles = 3; // Thanh toán mỗi 10 ngày
        } else {
            numOfCycles = durationDays / 15; // Mỗi 15 ngày 1 lần thanh toán
        }
        return numOfCycles;
    }
}
