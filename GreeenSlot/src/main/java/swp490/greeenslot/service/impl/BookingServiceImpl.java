package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.config.VNPayUtils;
import swp490.greeenslot.dto.BookingRequestDTO;
import swp490.greeenslot.dto.BookingResponseDTO;
import swp490.greeenslot.dto.ExtensionRequestDTO;
import swp490.greeenslot.dto.RentalHistoryDTO;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.BookingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VNPayUtils vnPayUtils;

    @Override
    @Transactional(readOnly = true)
    public List<GardenSlot> getAvailableSlots(Long locationId) {
        if (locationId == null) {
            return gardenSlotRepository.findAll().stream()
                    .filter(g -> g.getStatus() == ESlotStatus.AVAILABLE)
                    .collect(Collectors.toList());
        }
        return gardenSlotRepository.findByPillarLocationIdAndStatus(locationId, ESlotStatus.AVAILABLE);
    }

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO request, String username, String ipAddress) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        GardenSlot slot = gardenSlotRepository.findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Garden slot not found: " + request.getSlotId()));

        if (slot.getStatus() != ESlotStatus.AVAILABLE) {
            throw new RuntimeException("Slot is not available for booking");
        }

        // Check if there is any active rental for this slot
        List<SlotRental> activeRentals = slotRentalRepository.findActiveRentals(slot.getId(), LocalDateTime.now());
        if (!activeRentals.isEmpty()) {
            throw new RuntimeException("Slot is already rented");
        }

        // Check if there is a pending payment in the last 15 minutes
        List<PaymentTransaction> recentPendingTxns = paymentTransactionRepository.findRecentPendingTransactions(slot.getId(), LocalDateTime.now().minusMinutes(15));
        if (!recentPendingTxns.isEmpty()) {
            throw new RuntimeException("Slot has a pending payment transaction. Please try again in 15 minutes.");
        }

        int months = request.getDurationInMonths();
        if (months <= 0) {
            throw new RuntimeException("Duration must be greater than 0");
        }
        BigDecimal amount = slot.getPrice().multiply(new BigDecimal(months));

        LocalDateTime start = request.getStartTime();
        if (start == null) {
            start = LocalDateTime.now();
        }
        LocalDateTime end = start.plusMonths(months);

        // Create SlotRental in PENDING state
        SlotRental rental = new SlotRental();
        rental.setUser(user);
        rental.setGardenSlot(slot);
        rental.setStartTime(start);
        rental.setEndTime(end);
        rental.setStatus(ERentalStatus.PENDING);
        rental = slotRentalRepository.save(rental);

        // Generate vnpTxnRef: BOOK_[slotId]_[duration]_[uuid]
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String txnRef = "BOOK_" + slot.getId() + "_" + months + "_" + uuid;

        // Create PaymentTransaction
        PaymentTransaction txn = new PaymentTransaction();
        txn.setRental(rental);
        txn.setAmount(amount);
        txn.setVnpTxnRef(txnRef);
        txn.setPaymentDate(LocalDateTime.now()); // used as transaction initiation time
        txn.setStatus(EPaymentStatus.PENDING);
        paymentTransactionRepository.save(txn);

        String orderInfo = "GreenSlot Booking: Slot " + slot.getSlotNumber() + " for " + months + " months";
        String paymentUrl = vnPayUtils.buildPaymentUrl(txnRef, amount, ipAddress, orderInfo);

        return new BookingResponseDTO(rental.getId(), paymentUrl, txnRef);
    }

    @Override
    @Transactional
    public BookingResponseDTO extendRental(ExtensionRequestDTO request, String username, String ipAddress) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        SlotRental rental = slotRentalRepository.findById(request.getRentalId())
                .orElseThrow(() -> new RuntimeException("Rental contract not found: " + request.getRentalId()));

        if (!rental.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You do not own this rental contract");
        }

        if (rental.getStatus() != ERentalStatus.ACTIVE) {
            throw new RuntimeException("Only ACTIVE rental contracts can be extended");
        }

        int months = request.getDurationInMonths();
        if (months <= 0) {
            throw new RuntimeException("Extension duration must be greater than 0");
        }

        GardenSlot slot = rental.getGardenSlot();
        BigDecimal amount = slot.getPrice().multiply(new BigDecimal(months));

        // Generate vnpTxnRef: EXT_[rentalId]_[duration]_[uuid]
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String txnRef = "EXT_" + rental.getId() + "_" + months + "_" + uuid;

        // Create PaymentTransaction
        PaymentTransaction txn = new PaymentTransaction();
        txn.setRental(rental);
        txn.setAmount(amount);
        txn.setVnpTxnRef(txnRef);
        txn.setPaymentDate(LocalDateTime.now());
        txn.setStatus(EPaymentStatus.PENDING);
        paymentTransactionRepository.save(txn);

        String orderInfo = "GreenSlot Extension: Rental " + rental.getId() + " for " + months + " months";
        String paymentUrl = vnPayUtils.buildPaymentUrl(txnRef, amount, ipAddress, orderInfo);

        return new BookingResponseDTO(rental.getId(), paymentUrl, txnRef);
    }

    @Override
    @Transactional
    public Map<String, String> processIpn(Map<String, String> params) {
        Map<String, String> response = new HashMap<>();

        if (!vnPayUtils.verifySignature(params)) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid Signature");
            return response;
        }

        String txnRef = params.get("vnp_TxnRef");
        String amountStr = params.get("vnp_Amount");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        if (txnRef == null || amountStr == null || responseCode == null) {
            response.put("RspCode", "99");
            response.put("Message", "Input data required");
            return response;
        }

        PaymentTransaction txn = paymentTransactionRepository.findByVnpTxnRef(txnRef).orElse(null);
        if (txn == null) {
            response.put("RspCode", "01");
            response.put("Message", "Order not Found");
            return response;
        }

        // VNPay amount is multiplied by 100, divide it to match decimal value
        BigDecimal vnpAmount = new BigDecimal(amountStr).divide(new BigDecimal(100));
        if (txn.getAmount().compareTo(vnpAmount) != 0) {
            response.put("RspCode", "04");
            response.put("Message", "Invalid Amount");
            return response;
        }

        if (txn.getStatus() != EPaymentStatus.PENDING) {
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return response;
        }

        boolean isSuccess = "00".equals(responseCode) && "00".equals(transactionStatus);
        txn.setPaymentDate(LocalDateTime.now());

        if (isSuccess) {
            txn.setStatus(EPaymentStatus.SUCCESS);
            paymentTransactionRepository.save(txn);

            if (txnRef.startsWith("BOOK_")) {
                SlotRental rental = txn.getRental();
                rental.setStatus(ERentalStatus.ACTIVE);
                slotRentalRepository.save(rental);

                GardenSlot slot = rental.getGardenSlot();
                slot.setStatus(ESlotStatus.RENTED);
                gardenSlotRepository.save(slot);

            } else if (txnRef.startsWith("EXT_")) {
                SlotRental rental = txn.getRental();
                String[] parts = txnRef.split("_");
                int durationMonths = Integer.parseInt(parts[2]);

                LocalDateTime currentEnd = rental.getEndTime();
                LocalDateTime newEnd = currentEnd.isBefore(LocalDateTime.now())
                        ? LocalDateTime.now().plusMonths(durationMonths)
                        : currentEnd.plusMonths(durationMonths);

                rental.setEndTime(newEnd);
                rental.setStatus(ERentalStatus.ACTIVE);
                slotRentalRepository.save(rental);

                GardenSlot slot = rental.getGardenSlot();
                slot.setStatus(ESlotStatus.RENTED);
                gardenSlotRepository.save(slot);
            }
        } else {
            txn.setStatus(EPaymentStatus.FAILED);
            paymentTransactionRepository.save(txn);

            if (txnRef.startsWith("BOOK_")) {
                SlotRental rental = txn.getRental();
                rental.setStatus(ERentalStatus.CANCELLED);
                slotRentalRepository.save(rental);

                GardenSlot slot = rental.getGardenSlot();
                slot.setStatus(ESlotStatus.AVAILABLE);
                gardenSlotRepository.save(slot);
            }
        }

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalHistoryDTO> getRentalHistory(String username) {
        List<SlotRental> rentals = slotRentalRepository.findByUserUsernameOrderByStartTimeDesc(username);
        List<RentalHistoryDTO> history = new ArrayList<>();

        for (SlotRental rental : rentals) {
            GardenSlot slot = rental.getGardenSlot();
            Pillar pillar = slot.getPillar();
            Location location = pillar.getLocation();

            List<PaymentTransaction> txns = paymentTransactionRepository.findByRentalIdOrderByPaymentDateDesc(rental.getId());
            List<RentalHistoryDTO.PaymentTransactionInfo> txnInfos = new ArrayList<>();
            for (PaymentTransaction txn : txns) {
                txnInfos.add(new RentalHistoryDTO.PaymentTransactionInfo(
                        txn.getId(),
                        txn.getAmount(),
                        txn.getVnpTxnRef(),
                        txn.getPaymentDate(),
                        txn.getStatus().name()
                ));
            }

            history.add(new RentalHistoryDTO(
                    rental.getId(),
                    slot.getSlotNumber(),
                    pillar.getPillarCode(),
                    location.getName(),
                    location.getAddress(),
                    rental.getStartTime(),
                    rental.getEndTime(),
                    rental.getStatus().name(),
                    txnInfos
            ));
        }

        return history;
    }
}
