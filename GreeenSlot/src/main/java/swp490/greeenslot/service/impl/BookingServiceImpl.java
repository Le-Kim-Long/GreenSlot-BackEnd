package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);

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

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GardenSlot> getAvailableSlots(Long locationId) {
        // Retrieve available slots (Hibernate automatically populates the newly added imageUrl field)
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

        // Set slot status to PENDING_PAYMENT to reserve it temporarily
        slot.setStatus(ESlotStatus.PENDING_PAYMENT);
        gardenSlotRepository.save(slot);

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
        logger.info("processIpn called with parameters: {}", params);
        Map<String, String> response = new HashMap<>();

        if (!vnPayUtils.verifySignature(params)) {
            logger.error("VNPay IPN signature verification failed for params: {}", params);
            response.put("RspCode", "97");
            response.put("Message", "Invalid Signature");
            return response;
        }

        String txnRef = params.get("vnp_TxnRef");
        String amountStr = params.get("vnp_Amount");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        if (txnRef == null || amountStr == null || responseCode == null) {
            logger.error("VNPay IPN processed failed: Input data required. txnRef={}, amount={}, responseCode={}", txnRef, amountStr, responseCode);
            response.put("RspCode", "99");
            response.put("Message", "Input data required");
            return response;
        }

        PaymentTransaction txn = paymentTransactionRepository.findByVnpTxnRef(txnRef).orElse(null);
        if (txn == null) {
            logger.error("VNPay IPN processed failed: Transaction not found for txnRef={}", txnRef);
            response.put("RspCode", "01");
            response.put("Message", "Order not Found");
            return response;
        }

        // VNPay amount is multiplied by 100, multiply txn amount by 100 for safe comparison without division
        BigDecimal expectedVnpAmount = txn.getAmount().multiply(new BigDecimal(100));
        if (expectedVnpAmount.compareTo(new BigDecimal(amountStr)) != 0) {
            logger.error("VNPay IPN processed failed: Invalid amount for txnRef={}. Expected: {}, Received: {}", txnRef, expectedVnpAmount, amountStr);
            response.put("RspCode", "04");
            response.put("Message", "Invalid Amount");
            return response;
        }

        if (txn.getStatus() != EPaymentStatus.PENDING) {
            logger.info("VNPay IPN order already confirmed for txnRef={}, current status: {}", txnRef, txn.getStatus());
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return response;
        }

        // vnp_TransactionStatus might be null, empty, or absent in return redirects, so check responseCode and fallback if present
        boolean isSuccess = "00".equals(responseCode) && (transactionStatus == null || transactionStatus.isEmpty() || "00".equals(transactionStatus));
        logger.info("VNPay transaction result for txnRef={}: success={}, responseCode={}, transactionStatus={}", txnRef, isSuccess, responseCode, transactionStatus);
        txn.setPaymentDate(LocalDateTime.now());

        if (isSuccess) {
            logger.info("Updating transaction and rental status to SUCCESS/ACTIVE for txnRef={}", txnRef);
            txn.setStatus(EPaymentStatus.SUCCESS);
            paymentTransactionRepository.save(txn);

            if (txnRef.startsWith("BOOK_")) {
                SlotRental rental = txn.getRental();
                rental.setStatus(ERentalStatus.ACTIVE);
                slotRentalRepository.save(rental);

                GardenSlot slot = rental.getGardenSlot();
                slot.setStatus(ESlotStatus.RENTED);
                gardenSlotRepository.save(slot);
                logger.info("Booking rental ID {} activated, Garden Slot ID {} status set to RENTED", rental.getId(), slot.getId());

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
                logger.info("Rental ID {} extension of {} months saved. New end time: {}", rental.getId(), durationMonths, newEnd);
            }
        } else {
            logger.warn("Transaction failed or cancelled for txnRef={}. Updating statuses to FAILED/CANCELLED", txnRef);
            txn.setStatus(EPaymentStatus.FAILED);
            paymentTransactionRepository.save(txn);

            if (txnRef.startsWith("BOOK_")) {
                SlotRental rental = txn.getRental();
                rental.setStatus(ERentalStatus.CANCELLED);
                slotRentalRepository.save(rental);

                GardenSlot slot = rental.getGardenSlot();
                // Explicitly check if there are any other active or pending rentals
                long otherCount = slotRentalRepository.countOtherActiveOrPending(slot.getId(), rental.getId());
                if (otherCount == 0) {
                    slot.setStatus(ESlotStatus.AVAILABLE);
                    gardenSlotRepository.save(slot);
                    logger.info("Booking rental ID {} cancelled, Garden Slot ID {} status set to AVAILABLE", rental.getId(), slot.getId());
                } else {
                    logger.info("Booking rental ID {} cancelled, Garden Slot ID {} kept status because of other active/pending rentals", rental.getId(), slot.getId());
                }
            }
        }

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalHistoryDTO> getRentalHistory(String username) {
        List<SlotRental> rentals = slotRentalRepository.findByUserUsernameWithSlotAndPillarAndLocation(username);
        
        // Fetch all transactions for this user's rentals in one query and group them by rental ID
        List<PaymentTransaction> allTxns = paymentTransactionRepository.findAllTransactionsForUser(username);
        Map<Long, List<PaymentTransaction>> txnsByRentalId = allTxns.stream()
                .filter(t -> t.getRental() != null)
                .collect(Collectors.groupingBy(t -> t.getRental().getId()));

        List<RentalHistoryDTO> history = new ArrayList<>();

        for (SlotRental rental : rentals) {
            GardenSlot slot = rental.getGardenSlot();
            Pillar pillar = slot.getPillar();
            Location location = pillar.getLocation();

            List<PaymentTransaction> txns = txnsByRentalId.getOrDefault(rental.getId(), Collections.emptyList());
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

    @Override
    @Transactional
    public void cancelPendingBooking(Long rentalId, String username) {
        SlotRental rental = slotRentalRepository.findByIdWithPessimisticLock(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Slot rental not found with ID: " + rentalId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrManager = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER"));

        if (!isAdminOrManager && !rental.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized: Only the contract owner or admin/manager can cancel this booking");
        }

        if (rental.getStatus() != ERentalStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be cancelled");
        }

        rental.setStatus(ERentalStatus.CANCELLED);
        slotRentalRepository.save(rental);

        List<PaymentTransaction> pendingTxns = paymentTransactionRepository.findByRentalIdOrderByPaymentDateDesc(rentalId);
        for (PaymentTransaction txn : pendingTxns) {
            if (txn.getStatus() == EPaymentStatus.PENDING) {
                txn.setStatus(EPaymentStatus.FAILED);
                paymentTransactionRepository.save(txn);
            }
        }

        List<GardeningTask> pendingTasks = gardeningTaskRepository.findPendingTasksBySlotId(rental.getGardenSlot().getId());
        for (GardeningTask task : pendingTasks) {
            task.setStatus(ETaskStatus.CANCELLED);
            gardeningTaskRepository.save(task);
        }

        GardenSlot slot = rental.getGardenSlot();
        long activeOrPendingCount = slotRentalRepository.countOtherActiveOrPending(slot.getId(), rental.getId());
        if (activeOrPendingCount == 0) {
            slot.setStatus(ESlotStatus.AVAILABLE);
            gardenSlotRepository.save(slot);
        }
    }

    @Override
    @Transactional
    public BookingResponseDTO getOrRegeneratePaymentUrl(Long rentalId, String username, String ipAddress) {
        SlotRental rental = slotRentalRepository.findByIdWithPessimisticLock(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Slot rental not found with ID: " + rentalId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrManager = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER"));

        if (!isAdminOrManager && !rental.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized: Only the contract owner can repay this booking");
        }

        if (rental.getStatus() != ERentalStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can generate payment URL");
        }

        List<PaymentTransaction> txns = paymentTransactionRepository.findByRentalIdOrderByPaymentDateDesc(rentalId);
        PaymentTransaction pendingTxn = txns.stream()
                .filter(t -> t.getStatus() == EPaymentStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending payment transaction found for this booking"));

        String orderInfo = "Thanh toan don thue vuon GreenSlot ID: " + rentalId;
        String paymentUrl = vnPayUtils.buildPaymentUrl(pendingTxn.getVnpTxnRef(), pendingTxn.getAmount(), ipAddress, orderInfo);

        return new BookingResponseDTO(rentalId, paymentUrl, pendingTxn.getVnpTxnRef());
    }
}
