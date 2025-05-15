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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        //LocalDate firstPaymentDate = booking.getCreatedAt().toLocalDate().plusDays(1);

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
            LocalDate calculatedDueDate = startDate.minusDays(2);
            LocalDate today = LocalDate.now();
            if (calculatedDueDate.isBefore(today)) {
                calculatedDueDate = today;
            }

            paymentCycle.setDueDate(calculatedDueDate);
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
        Booking booking = cycle.getBooking();
        BigDecimal refundAmount = cycle.getAmountDue();


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
        boolean hasCompletedDetail = bookingDetailRepository.findByBookingId(booking.getId())
                .stream()
                .filter(detail -> !"CANCELED".equalsIgnoreCase(detail.getStatus()))
                .anyMatch(detail ->
                        !detail.getSessionDate().isBefore(cycle.getStartDate()) &&
                                !detail.getSessionDate().isAfter(cycle.getEndDate()) &&
                                "COMPLETED".equalsIgnoreCase(detail.getStatus())
                );
        if (hasCompletedDetail) {
            booking.setStatus("COMPLETED");
        } else {
            booking.setStatus("CONFIRMED_PAID");
        }
        refundAmount = refundAmount.add(totalFutureRefund);
        booking.setTotalPrice(booking.getTotalPrice().subtract(refundAmount));
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
        cycle.setStatus("CANCELED");
        paymentCycleRepository.save(cycle);

        return modelMapper.map(cycle, PaymentCycleResponseDto.class);
    }

    private int getNumOfCycles(Booking booking) {
        int durationDays = booking.getBookingPackage().getDurationDays();
        return (int) Math.ceil((double) durationDays / 5.0);
    }

    @Scheduled(cron = "0 10 0 * * *") // Run daily at 00:15
    @Transactional
    public void checkOverduePaymentCycles() {
        LocalDate today = LocalDate.now();
        List<PaymentCycle> overdueCycles = paymentCycleRepository.findByDueDateBeforeAndStatus(today, "PENDING");

        for (PaymentCycle cycle : overdueCycles) {
            Booking booking = cycle.getBooking();
            cycle.setStatus("OVERDUE");
            paymentCycleRepository.save(cycle);
            // Trường hợp cycleOrder == 1 => OVERDUE toàn bộ booking & bookingDetails
            if (cycle.getCycleOrder() == 1) {
                List<BookingDetail> allDetails = bookingDetailRepository.findByBookingId(booking.getId());
                for (BookingDetail detail : allDetails) {
                    detail.setStatus("OVERDUE");
                    bookingDetailRepository.save(detail);
                }
                booking.setStatus("OVERDUE");
                bookingRepository.save(booking);
            } else {
                List<BookingDetail> currentDetails = bookingDetailRepository.findByBookingId(booking.getId()).stream()
                        .filter(detail -> !detail.getSessionDate().isBefore(cycle.getStartDate()) &&
                                        !detail.getSessionDate().isAfter(cycle.getEndDate()))
                        .toList();
                for (BookingDetail detail : currentDetails) {
                    detail.setStatus("OVERDUE");
                    bookingDetailRepository.save(detail);
                }
                // Hủy các kỳ và buổi sau kỳ này
                List<PaymentCycle> futureCycles = paymentCycleRepository.findByBookingId(booking.getId()).stream()
                        .filter(c -> c.getCycleOrder() > cycle.getCycleOrder())
                        .toList();
                for (PaymentCycle fc : futureCycles) {
                    List<BookingDetail> currentDetails1 = bookingDetailRepository.findByBookingId(booking.getId()).stream()
                            .filter(detail -> !detail.getSessionDate().isBefore(cycle.getStartDate()) &&
                                    !detail.getSessionDate().isAfter(cycle.getEndDate()))
                            .toList();
                    for (BookingDetail detail : currentDetails1) {
                        detail.setStatus("OVERDUE");
                        bookingDetailRepository.save(detail);
                    }
                    fc.setStatus("OVERDUE");
                    paymentCycleRepository.save(fc);
                }
                Optional<PaymentCycle> firstCycleOpt = paymentCycleRepository.findByBookingId(booking.getId()).stream()
                        .filter(c -> c.getCycleOrder() == 1)
                        .findFirst();
                if (firstCycleOpt.isPresent() &&
                        firstCycleOpt.get().getEndDate().isBefore(today) &&
                        !booking.getStatus().equalsIgnoreCase("COMPLETED")) {
                    BigDecimal newTotalPrice = bookingDetailRepository.calculateTotalPriceByBooking(booking.getId());
                    booking.setTotalPrice(newTotalPrice);
                    booking.setStatus("COMPLETED");
                    bookingRepository.save(booking);
                }
            }
        }
        System.out.println("✅ Checked overdue payment cycles at " + today);
    }

}
