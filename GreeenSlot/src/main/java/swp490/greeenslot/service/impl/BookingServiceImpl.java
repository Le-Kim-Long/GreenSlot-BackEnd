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
import java.time.temporal.ChronoUnit;
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
    private TreeRepository treeRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Autowired
    private TreePlantingRequestRepository treePlantingRequestRepository;

    @Autowired
    private VNPayUtils vnPayUtils;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Autowired
    private swp490.greeenslot.service.NotificationService notificationService;

    @Autowired
    private swp490.greeenslot.service.FirebaseMessagingService firebaseMessagingService;

    @Autowired
    private swp490.greeenslot.service.HarvestHistoryService harvestHistoryService;

    @Override
    @Transactional(readOnly = true)
    public List<GardenSlot> getAvailableSlots(Long locationId) {
        List<GardenSlot> allSlots;
        if (locationId == null) {
            allSlots = gardenSlotRepository.findAll();
        } else {
            allSlots = gardenSlotRepository.findByLocationId(locationId);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> rentedPillarIds = slotRentalRepository.findCurrentlyRentedPillarIds(now);
        Set<Long> rentedPillarIdSet = new HashSet<>(rentedPillarIds);

        return allSlots.stream()
                .filter(g -> g.getStatus() != ESlotStatus.MAINTENANCE)
                .filter(g -> {
                    List<Pillar> pillars = g.getPillars();
                    if (pillars == null || pillars.isEmpty()) {
                        if (g.getPillar() != null) {
                            pillars = List.of(g.getPillar());
                        } else if (g.getLocation() != null) {
                            pillars = pillarRepository.findByLocationId(g.getLocation().getId());
                        } else {
                            return g.getStatus() == ESlotStatus.AVAILABLE;
                        }
                    }
                    // At least one pillar must not be rented and status == ACTIVE
                    return pillars.stream().anyMatch(p -> p.getStatus() == EPillarStatus.ACTIVE && !rentedPillarIdSet.contains(p.getId()));
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO request, String username, String ipAddress) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        GardenSlot slot = gardenSlotRepository.findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Garden slot not found: " + request.getSlotId()));

        if (slot.getStatus() == ESlotStatus.MAINTENANCE) {
            throw new RuntimeException("Slot is currently under maintenance");
        }

        // Check if there is a pending payment in the last 15 minutes
        List<PaymentTransaction> recentPendingTxns = paymentTransactionRepository.findRecentPendingTransactions(slot.getId(), LocalDateTime.now().minusMinutes(15));
        if (!recentPendingTxns.isEmpty()) {
            throw new RuntimeException("Slot has a pending payment transaction. Please try again in 15 minutes.");
        }

        int months = request.getDurationInMonths();
        if (months <= 0) {
            throw new IllegalArgumentException("Duration must be at least 1 month");
        }
        if (months > 120) {
            throw new IllegalArgumentException("Duration cannot exceed 120 months");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = request.getStartTime();
        if (start == null) {
            start = now;
        } else if (start.toLocalDate().isBefore(now.toLocalDate())) {
            throw new IllegalArgumentException("Start time cannot be in the past");
        } else if (start.toLocalDate().isEqual(now.toLocalDate()) && start.isBefore(now)) {
            start = now;
        }
        LocalDateTime end = start.plusMonths(months);

        // Resolve available slot pillars (fallback to location's unassigned pillars if slot has no assigned pillars)
        List<Pillar> slotPillars = slot.getPillars();
        if (slotPillars == null || slotPillars.isEmpty()) {
            if (slot.getPillar() != null) {
                slotPillars = List.of(slot.getPillar());
            } else if (slot.getLocation() != null) {
                slotPillars = pillarRepository.findByLocationId(slot.getLocation().getId());
            } else {
                slotPillars = Collections.emptyList();
            }
        }

        List<Long> currentlyRentedIds = slotRentalRepository.findCurrentlyRentedPillarIds(now);
        Set<Long> currentlyRentedSet = new HashSet<>(currentlyRentedIds);

        double slotMaxArea = (slot.getArea() != null && slot.getArea() > 0) ? slot.getArea() : 3.0;

        boolean hasCustomCounts = (request.getSmallPillarsCount() != null && request.getSmallPillarsCount() > 0)
                || (request.getMediumPillarsCount() != null && request.getMediumPillarsCount() > 0)
                || (request.getLargePillarsCount() != null && request.getLargePillarsCount() > 0);

        List<Pillar> selectedPillars = new ArrayList<>();
        List<Pillar> newlyProvisionedPillars = new ArrayList<>();

        if (hasCustomCounts) {
            int smallCount = request.getSmallPillarsCount() != null ? Math.max(0, request.getSmallPillarsCount()) : 0;
            int mediumCount = request.getMediumPillarsCount() != null ? Math.max(0, request.getMediumPillarsCount()) : 0;
            int largeCount = request.getLargePillarsCount() != null ? Math.max(0, request.getLargePillarsCount()) : 0;

            double totalCustomArea = (smallCount * 1.0) + (mediumCount * 1.5) + (largeCount * 2.0);
            if (totalCustomArea > slotMaxArea + 0.01) {
                throw new IllegalArgumentException(String.format(
                    "Tổng diện tích các trụ đã chọn (%.1f m²) vượt quá diện tích tối đa của ô vườn (%.1f m²). Ô nhỏ không thể thuê nhiều trụ lớn, vui lòng giảm bớt trụ lớn hoặc chọn ô vườn có diện tích lớn hơn.",
                    totalCustomArea, slotMaxArea
                ));
            }

            // Allocate or create pillars matching requested counts
            PillarAllocationResult sRes = allocateOrCreatePillars(slot, EPillarType.SMALL, smallCount, currentlyRentedSet);
            PillarAllocationResult mRes = allocateOrCreatePillars(slot, EPillarType.MEDIUM, mediumCount, currentlyRentedSet);
            PillarAllocationResult lRes = allocateOrCreatePillars(slot, EPillarType.LARGE, largeCount, currentlyRentedSet);

            selectedPillars.addAll(sRes.allPillars);
            selectedPillars.addAll(mRes.allPillars);
            selectedPillars.addAll(lRes.allPillars);

            newlyProvisionedPillars.addAll(sRes.newlyCreatedPillars);
            newlyProvisionedPillars.addAll(mRes.newlyCreatedPillars);
            newlyProvisionedPillars.addAll(lRes.newlyCreatedPillars);
        } else if (request.getPillarIds() != null && !request.getPillarIds().isEmpty()) {
            Set<Long> requestedPillarIdSet = new HashSet<>(request.getPillarIds());
            for (Pillar p : slotPillars) {
                if (requestedPillarIdSet.contains(p.getId())) {
                    selectedPillars.add(p);
                }
            }
            if (selectedPillars.size() != requestedPillarIdSet.size()) {
                throw new IllegalArgumentException("Một hoặc nhiều trụ bạn chọn không thuộc ô vườn hoặc cơ sở này");
            }
        } else if (!slotPillars.isEmpty()) {
            // Default: select all active and non-rented pillars in this slot
            selectedPillars = slotPillars.stream()
                    .filter(p -> p.getStatus() == EPillarStatus.ACTIVE && !currentlyRentedSet.contains(p.getId()))
                    .collect(Collectors.toList());
        } else {
            // If slot has no pillars created yet in DB, automatically provision standard template pillars fitting slot area
            int defaultMediumPillars = Math.max(1, (int) Math.floor(slotMaxArea / 1.5));
            PillarAllocationResult mRes = allocateOrCreatePillars(slot, EPillarType.MEDIUM, defaultMediumPillars, currentlyRentedSet);
            selectedPillars.addAll(mRes.allPillars);
            newlyProvisionedPillars.addAll(mRes.newlyCreatedPillars);
        }

        if (selectedPillars.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 trụ canh tác còn trống để thuê");
        }

        // Slot area capacity validation
        double totalAreaUsed = selectedPillars.stream().mapToDouble(Pillar::getEffectiveArea).sum();
        if (totalAreaUsed > slotMaxArea + 0.01) {
            throw new IllegalArgumentException(String.format(
                "Tổng diện tích các trụ đã chọn (%.1f m²) vượt quá diện tích tối đa của ô vườn (%.1f m²). Vui lòng chọn số lượng trụ phù hợp với kích thước ô vườn.",
                totalAreaUsed, slotMaxArea
            ));
        }

        // Exclusivity validation: Ensure no selected pillar is already rented anywhere in the system
        for (Pillar p : selectedPillars) {
            if (p.getStatus() == EPillarStatus.RENTED || currentlyRentedSet.contains(p.getId())) {
                throw new RuntimeException("Trụ " + p.getPillarCode() + " đã được khách hàng khác thuê. Vui lòng chọn trụ khác.");
            }
        }

        // Resolve trees per pillar if multiple chosen, or single tree
        List<Tree> resolvedTrees = new java.util.ArrayList<>();
        if (request.getTreeIds() != null && !request.getTreeIds().isEmpty()) {
            for (Long tId : request.getTreeIds()) {
                if (tId != null && tId > 0) {
                    treeRepository.findById(tId).ifPresent(resolvedTrees::add);
                }
            }
        }
        
        Tree selectedTree = null;
        if (request.getTreeId() != null && request.getTreeId() > 0) {
            selectedTree = treeRepository.findById(request.getTreeId()).orElse(null);
        }
        if (selectedTree == null && !resolvedTrees.isEmpty()) {
            selectedTree = resolvedTrees.get(0);
        }
        if (selectedTree == null) {
            selectedTree = selectedPillars.stream()
                    .map(Pillar::getDefaultTree)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        // Validate tree growth time vs rental duration for all chosen trees
        int maxRentalDays = months * 30;
        List<Tree> allTreesToValidate = new java.util.ArrayList<>(resolvedTrees);
        if (selectedTree != null && !allTreesToValidate.contains(selectedTree)) {
            allTreesToValidate.add(selectedTree);
        }
        for (Tree t : allTreesToValidate) {
            if (t.getHarvestDays() != null && t.getHarvestDays() > 0 && t.getHarvestDays() > maxRentalDays) {
                int minMonths = (int) Math.ceil(t.getHarvestDays() / 30.0);
                throw new IllegalArgumentException(String.format(
                    "Thời gian sinh trưởng của giống cây '%s' (%d ngày) vượt quá thời hạn thuê (%d tháng = %d ngày). Vui lòng chọn thời gian thuê tối thiểu %d tháng.",
                    t.getTreeName(), t.getHarvestDays(), months, maxRentalDays, minMonths
                ));
            }
        }

        // Calculate amount:
        // Monthly rent = sum of selected pillars' monthly prices (Slot base price = 0)
        BigDecimal monthlySlotPrice = selectedPillars.stream()
                .map(Pillar::getEffectivePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Vegetable seedling cost scaled by hole capacity for each pillar (Option 1: price * holes / 24.0)
        BigDecimal totalTreeCost = BigDecimal.ZERO;
        for (int i = 0; i < selectedPillars.size(); i++) {
            Pillar p = selectedPillars.get(i);
            Tree treeForPillar = (i < resolvedTrees.size()) ? resolvedTrees.get(i) : selectedTree;
            if (treeForPillar != null && treeForPillar.getPrice() != null && treeForPillar.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                double scale = (double) p.getEffectiveHoles() / 24.0;
                BigDecimal scaledPrice = treeForPillar.getPrice().multiply(BigDecimal.valueOf(scale));
                totalTreeCost = totalTreeCost.add(scaledPrice);
            }
        }

        BigDecimal amount = monthlySlotPrice.multiply(new BigDecimal(months)).add(totalTreeCost);

        // Create SlotRental in PENDING state
        SlotRental rental = new SlotRental();
        rental.setUser(user);
        rental.setGardenSlot(slot);
        rental.setStartTime(start);
        rental.setEndTime(end);
        rental.setStatus(ERentalStatus.PENDING);
        rental.setRentedPillars(selectedPillars);
        if (selectedTree != null) {
            rental.setTree(selectedTree);
            rental.setTreeStatus(ETreeStatus.HEALTHY);
        }
        rental = slotRentalRepository.save(rental);

        // Set slot status to PENDING_PAYMENT to reserve it temporarily
        slot.setStatus(ESlotStatus.PENDING_PAYMENT);
        gardenSlotRepository.save(slot);

        // If new pillars need to be physically prepared and assembled at the location, trigger notification & task
        if (!newlyProvisionedPillars.isEmpty()) {
            long newSmall = newlyProvisionedPillars.stream().filter(p -> p.getEffectivePillarType() == EPillarType.SMALL).count();
            long newMedium = newlyProvisionedPillars.stream().filter(p -> p.getEffectivePillarType() == EPillarType.MEDIUM).count();
            long newLarge = newlyProvisionedPillars.stream().filter(p -> p.getEffectivePillarType() == EPillarType.LARGE).count();

            List<String> parts = new ArrayList<>();
            if (newLarge > 0) parts.add(newLarge + " Trụ Lớn");
            if (newMedium > 0) parts.add(newMedium + " Trụ Vừa");
            if (newSmall > 0) parts.add(newSmall + " Trụ Nhỏ");
            String pillarSummary = String.join(", ", parts);

            // 1. Create setup task for technical staff
            GardeningTask setupTask = new GardeningTask();
            setupTask.setTaskName("Lắp đặt bổ sung " + newlyProvisionedPillars.size() + " trụ cho Ô " + slot.getSlotNumber());
            setupTask.setDescription(String.format(
                "Khách hàng %s vừa đặt thuê %d trụ tại Ô %s. Cơ sở cần chuẩn bị và lắp đặt bổ sung %s trước ngày %s.",
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                selectedPillars.size(),
                slot.getSlotNumber(),
                pillarSummary,
                start.toLocalDate()
            ));
            setupTask.setTaskType(ETaskType.MAINTENANCE);
            setupTask.setStatus(ETaskStatus.PENDING);
            setupTask.setTargetSlot(slot);
            setupTask.setRequestedBy(user);

            if (slot.getLocation() != null) {
                List<User> staffList = userRepository.findByRoleNameAndLocation(ERole.ROLE_GARDEN_STAFF, slot.getLocation().getId());
                if (!staffList.isEmpty()) {
                    setupTask.setAssignedStaff(staffList.get(0));
                }
            }
            gardeningTaskRepository.save(setupTask);

            // 2. Notify Location Manager(s) of this location
            List<User> managers = (slot.getLocation() != null)
                ? userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, slot.getLocation().getId())
                : Collections.emptyList();
            if (managers.isEmpty()) {
                managers = userRepository.findByRoleName(ERole.ROLE_LOCATION_MANAGER);
            }
            if (managers.isEmpty()) {
                managers = userRepository.findByRoleName(ERole.ROLE_ADMIN);
            }

            for (User mgr : managers) {
                notificationService.createNotification(
                    mgr.getId(),
                    "⚠️ Yêu cầu bổ sung trụ: Ô " + slot.getSlotNumber(),
                    String.format(
                        "Khách hàng %s đã đặt thuê %d trụ tại Ô %s (Cơ sở cần bổ sung %d trụ: %s). Vui lòng điều phối nhân viên lắp đặt hoàn thiện trước ngày %s.",
                        user.getFullName() != null ? user.getFullName() : user.getUsername(),
                        selectedPillars.size(),
                        slot.getSlotNumber(),
                        newlyProvisionedPillars.size(),
                        pillarSummary,
                        start.toLocalDate()
                    ),
                    "PILLAR_SETUP_REQUIRED",
                    rental.getId(),
                    "/dashboard/manager/tasks"
                );
            }

            if (setupTask.getAssignedStaff() != null) {
                notificationService.createNotification(
                    setupTask.getAssignedStaff().getId(),
                    "Nhiệm vụ mới: Lắp đặt trụ cho Ô " + slot.getSlotNumber(),
                    String.format("Bạn được giao nhiệm vụ lắp đặt bổ sung %s cho Ô %s trước ngày %s.",
                        pillarSummary, slot.getSlotNumber(), start.toLocalDate()),
                    "TASK_ASSIGNED",
                    setupTask.getId(),
                    "/dashboard/staff/tasks"
                );
            }
        }

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

        int pillarCount = selectedPillars.size();
        String orderInfo = "GreenSlot - Thue vuon " + slot.getSlotNumber() + " (" + pillarCount + " tru) trong " + months + " thang";
        boolean isMobile = Boolean.TRUE.equals(request.getIsMobile());
        String customMobileRedirectUrl = request.getMobileRedirectUrl();
        String paymentUrl = vnPayUtils.buildPaymentUrl(txnRef, amount, ipAddress, orderInfo, isMobile, customMobileRedirectUrl);

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
            throw new IllegalArgumentException("Extension duration must be a positive integer greater than 0");
        }
        if (months > 120) {
            throw new IllegalArgumentException("Extension duration cannot exceed 120 months");
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

        String orderInfo = "GreenSlot - Gia han vuon #" + rental.getId() + " them " + months + " thang";
        boolean isMobile = Boolean.TRUE.equals(request.getIsMobile());
        String customMobileRedirectUrl = request.getMobileRedirectUrl();
        String paymentUrl = vnPayUtils.buildPaymentUrl(txnRef, amount, ipAddress, orderInfo, isMobile, customMobileRedirectUrl);

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
            response.put("TxnStatus", txn.getStatus().name());
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

                if (rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty()) {
                    for (Pillar p : rental.getRentedPillars()) {
                        p.setStatus(EPillarStatus.RENTED);
                        pillarRepository.save(p);
                    }
                }

                GardenSlot slot = rental.getGardenSlot();
                if (slot != null) {
                    List<Pillar> allSlotPillars = slot.getPillars();
                    boolean allRented = allSlotPillars != null && !allSlotPillars.isEmpty() &&
                            allSlotPillars.stream().allMatch(p -> p.getStatus() == EPillarStatus.RENTED);
                    if (allRented) {
                        slot.setStatus(ESlotStatus.RENTED);
                    } else {
                        slot.setStatus(ESlotStatus.AVAILABLE);
                    }
                    gardenSlotRepository.save(slot);
                }
                logger.info("Booking rental ID {} activated, Garden Slot ID {} status updated", rental.getId(), slot != null ? slot.getId() : null);

                // Notify customer of successful payment and rental activation
                User customer = rental.getUser();
                String slotNumber = slot.getSlotNumber();
                String locationName = slot.getPillar() != null && slot.getPillar().getLocation() != null 
                        ? slot.getPillar().getLocation().getName() : "N/A";
                
                notificationService.createNotification(
                        customer.getId(),
                        "Thanh toán thành công",
                        String.format("Thanh toán cho ô vườn %s tại %s thành công. Hợp đồng thuê của bạn đã được kích hoạt đến ngày %s.",
                                slotNumber, locationName, rental.getEndTime().toLocalDate()),
                        "PAYMENT_SUCCESS"
                );
                
                firebaseMessagingService.sendPushNotification(
                        customer.getId(),
                        "Thanh toán thành công",
                        String.format("Hợp đồng thuê ô vườn %s đã kích hoạt đến %s", slotNumber, rental.getEndTime().toLocalDate())
                );

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

                // Notify customer of successful extension
                User customer = rental.getUser();
                String slotNumber = slot.getSlotNumber();
                
                notificationService.createNotification(
                        customer.getId(),
                        "Gia hạn hợp đồng thành công",
                        String.format("Hợp đồng thuê ô vườn %s đã được gia hạn thêm %d tháng đến ngày %s.",
                                slotNumber, durationMonths, newEnd.toLocalDate()),
                        "PAYMENT_SUCCESS"
                );
                
                firebaseMessagingService.sendPushNotification(
                        customer.getId(),
                        "Gia hạn hợp đồng thành công",
                        String.format("Ô vườn %s đã gia hạn đến %s", slotNumber, newEnd.toLocalDate())
                );
            } else if (txnRef.startsWith("PLANT_")) {
                String[] parts = txnRef.split("_");
                Long requestId = Long.parseLong(parts[1]);
                TreePlantingRequest req = treePlantingRequestRepository.findById(requestId).orElse(null);
                if (req != null) {
                    req.setStatus(EPlantingRequestStatus.APPROVED);
                    req.setProcessedAt(LocalDateTime.now());
                    treePlantingRequestRepository.save(req);
                    logger.info("Tree planting request ID {} approved and paid via VNPay", req.getId());

                    if (req.getRequestedBy() != null && notificationService != null) {
                        notificationService.createNotification(
                                req.getRequestedBy().getId(),
                                "Thanh toán giống rau thành công",
                                "Thanh toán tiền giống rau " + (req.getNewTree() != null ? req.getNewTree().getTreeName() : "") + " thành công. Yêu cầu gieo trồng đã được tiếp nhận và phân công nhân viên xử lý.",
                                "PAYMENT_SUCCESS"
                        );
                    }
                }
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

                // Notify customer of payment failure
                User customer = rental.getUser();
                String slotNumber = slot.getSlotNumber();
                
                notificationService.createNotification(
                        customer.getId(),
                        "Thanh toán không thành công",
                        String.format("Giao dịch thanh toán thuê ô vườn %s không thành công. Vui lòng thử lại hoặc liên hệ hỗ trợ.",
                                slotNumber),
                        "PAYMENT_FAILED"
                );
                
                firebaseMessagingService.sendPushNotification(
                        customer.getId(),
                        "Thanh toán không thành công",
                        String.format("Thanh toán cho ô vườn %s chưa thành công. Vui lòng kiểm tra lại.", slotNumber)
                );
            }
        }

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        response.put("TxnStatus", txn.getStatus().name());
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
            Location location = slot.getLocation() != null ? slot.getLocation() : (slot.getPillar() != null ? slot.getPillar().getLocation() : null);

            List<Pillar> rentedPillars = (rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty())
                    ? rental.getRentedPillars()
                    : slot.getPillars();

            List<String> pillarCodes = new ArrayList<>();
            List<RentalHistoryDTO.PillarInfo> pillarInfos = new ArrayList<>();
            Set<String> seenCodes = new HashSet<>();

            if (rentedPillars != null && !rentedPillars.isEmpty()) {
                for (Pillar p : rentedPillars) {
                    if (p != null && p.getPillarCode() != null && !seenCodes.contains(p.getPillarCode())) {
                        seenCodes.add(p.getPillarCode());
                        pillarCodes.add(p.getPillarCode());
                        pillarInfos.add(new RentalHistoryDTO.PillarInfo(
                                p.getId(),
                                p.getPillarCode(),
                                p.getStatus() != null ? p.getStatus().name() : "ACTIVE",
                                p.getCameraStreamUrl(),
                                p.getCameraStatus()
                        ));
                    }
                }
            } else if (slot.getPillar() != null && !seenCodes.contains(slot.getPillar().getPillarCode())) {
                Pillar p = slot.getPillar();
                seenCodes.add(p.getPillarCode());
                pillarCodes.add(p.getPillarCode());
                pillarInfos.add(new RentalHistoryDTO.PillarInfo(
                        p.getId(),
                        p.getPillarCode(),
                        p.getStatus() != null ? p.getStatus().name() : "ACTIVE",
                        p.getCameraStreamUrl(),
                        p.getCameraStatus()
                ));
            }
            String primaryPillarCode = !pillarCodes.isEmpty() ? String.join(", ", pillarCodes) : "N/A";
            String locationName = location != null ? location.getName() : "N/A";
            String locationAddress = location != null ? location.getAddress() : "N/A";

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

            Integer harvestDays = rental.getTree() != null ? rental.getTree().getHarvestDays() : null;
            LocalDateTime expectedHarvestAt = (rental.getPlantedAt() != null && harvestDays != null && harvestDays > 0)
                    ? rental.getPlantedAt().plusDays(harvestDays)
                    : null;

            RentalHistoryDTO dto = new RentalHistoryDTO(
                    rental.getId(),
                    slot.getId(),
                    slot.getSlotNumber(),
                    primaryPillarCode,
                    locationName,
                    locationAddress,
                    rental.getStartTime(),
                    rental.getEndTime(),
                    rental.getStatus().name(),
                    txnInfos,
                    rental.getTree() != null ? rental.getTree().getTreeName() : null,
                    rental.getHarvestNotifiedAt(),
                    rental.getHarvestDecision(),
                    rental.getPlantedAt(),
                    expectedHarvestAt
            );
            dto.setPillars(pillarInfos);
            dto.setPillarCodes(pillarCodes);
            history.add(dto);
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

        if (rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty()) {
            for (Pillar p : rental.getRentedPillars()) {
                p.setStatus(EPillarStatus.ACTIVE);
                pillarRepository.save(p);
            }
        }

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
        slot.setStatus(ESlotStatus.AVAILABLE);
        gardenSlotRepository.save(slot);
    }

    @Override
    @Transactional
    public BookingResponseDTO getOrRegeneratePaymentUrl(Long rentalId, String username, String ipAddress) {
        return getOrRegeneratePaymentUrl(rentalId, username, ipAddress, false);
    }

    @Override
    @Transactional
    public BookingResponseDTO getOrRegeneratePaymentUrl(Long rentalId, String username, String ipAddress, boolean isMobile) {
        return getOrRegeneratePaymentUrl(rentalId, username, ipAddress, isMobile, null);
    }

    @Override
    @Transactional
    public BookingResponseDTO getOrRegeneratePaymentUrl(Long rentalId, String username, String ipAddress, boolean isMobile, String customMobileRedirectUrl) {
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

        // Generate a fresh unique vnpTxnRef so VNPay Sandbox accepts the retry attempt without duplicate/expired errors
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        long months = ChronoUnit.MONTHS.between(rental.getStartTime(), rental.getEndTime());
        if (months <= 0) months = 1;
        String newTxnRef = "BOOK_" + rental.getGardenSlot().getId() + "_" + months + "_" + uuid;

        pendingTxn.setVnpTxnRef(newTxnRef);
        pendingTxn.setPaymentDate(LocalDateTime.now());
        paymentTransactionRepository.save(pendingTxn);

        String orderInfo = "GreenSlot - Thanh toan don thue vuon #" + rentalId;
        String paymentUrl = vnPayUtils.buildPaymentUrl(newTxnRef, pendingTxn.getAmount(), ipAddress, orderInfo, isMobile, customMobileRedirectUrl);

        return new BookingResponseDTO(rentalId, paymentUrl, newTxnRef);
    }

    @Override
    @Transactional
    public void recordHarvestDecision(Long rentalId, String decision, String username) {
        if (!"SELF".equals(decision) && !"STAFF".equals(decision)) {
            throw new IllegalArgumentException("Decision must be either SELF or STAFF");
        }

        SlotRental rental = slotRentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found with id: " + rentalId));

        if (rental.getUser() == null || !rental.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized: You do not own this rental contract.");
        }
        if (rental.getHarvestNotifiedAt() == null) {
            throw new IllegalArgumentException("No pending harvest notification for this rental.");
        }

        rental.setHarvestDecision(decision);

        if ("SELF".equals(decision)) {
            // Lưu lại lịch sử thu hoạch TRƯỚC khi xóa dữ liệu cây khỏi rental
            harvestHistoryService.recordHarvest(rental, "SELF", null);
            // Khách tự nhận đã thu hoạch xong -> ô đất coi như trống ngay, sẵn sàng cho yêu cầu trồng cây mới
            resetHarvestedTree(rental);
        }
        slotRentalRepository.save(rental);

        if (rental.getGardenSlot() == null) {
            return;
        }

        List<GardeningTask> harvestTasks = gardeningTaskRepository
                .findByTargetSlotIdAndTaskTypeOrderByCreatedAtDesc(rental.getGardenSlot().getId(), ETaskType.HARVEST);
        GardeningTask task = harvestTasks.stream()
                .filter(t -> t.getStatus() != ETaskStatus.COMPLETED && t.getStatus() != ETaskStatus.CANCELLED)
                .findFirst()
                .orElse(null);

        if (task == null || task.getAssignedStaff() == null) {
            return;
        }

        String slotNumber = rental.getGardenSlot().getSlotNumber();
        if ("SELF".equals(decision)) {
            task.setStatus(ETaskStatus.CANCELLED);
            gardeningTaskRepository.save(task);

            notificationService.createNotification(
                    task.getAssignedStaff().getId(),
                    "Khách đã tự thu hoạch",
                    "Khách hàng ở ô " + slotNumber + " đã chọn tự thu hoạch, bạn không cần xử lý công việc này nữa.",
                    "HARVEST_SELF"
            );
        } else {
            notificationService.createNotification(
                    task.getAssignedStaff().getId(),
                    "Khách nhờ hỗ trợ thu hoạch",
                    "Khách hàng ở ô " + slotNumber + " nhờ hỗ trợ thu hoạch giúp. Tiến hành xử lý công việc nhé.",
                    "HARVEST_STAFF_CONFIRMED"
            );
        }
    }

    /**
     * Sau khi thu hoạch xong (dù khách tự làm hay staff làm), dọn sạch thông tin cây cũ
     * trên rental để ô đất trở lại trạng thái "chưa trồng", sẵn sàng cho yêu cầu trồng cây mới.
     */
    private void resetHarvestedTree(SlotRental rental) {
        rental.setTree(null);
        rental.setTreeStatus(null);
        rental.setTreeNotes(null);
        rental.setPlantedAt(null);
        rental.setHarvestReminderSent(false);
        rental.setHarvestNotifiedAt(null);
        rental.setHarvestDecision(null);
    }

    private static class PillarAllocationResult {
        List<Pillar> allPillars = new ArrayList<>();
        List<Pillar> newlyCreatedPillars = new ArrayList<>();
    }

    private PillarAllocationResult allocateOrCreatePillars(GardenSlot slot, EPillarType type, int count, Set<Long> currentlyRentedSet) {
        PillarAllocationResult result = new PillarAllocationResult();
        if (count <= 0) return result;

        List<Pillar> existing = (slot.getLocation() != null)
                ? pillarRepository.findByLocationId(slot.getLocation().getId())
                : Collections.emptyList();

        for (Pillar p : existing) {
            if (result.allPillars.size() >= count) break;
            if (p.getEffectivePillarType() == type 
                    && p.getStatus() == EPillarStatus.ACTIVE 
                    && !currentlyRentedSet.contains(p.getId()) 
                    && !result.allPillars.contains(p)) {
                result.allPillars.add(p);
            }
        }

        int needed = count - result.allPillars.size();
        for (int i = 1; i <= needed; i++) {
            Pillar p = new Pillar();
            String suffix = type == EPillarType.SMALL ? "S" : type == EPillarType.LARGE ? "L" : "M";
            String code = "P-" + slot.getSlotNumber() + "-" + suffix + (result.allPillars.size() + i);
            p.setPillarCode(code);
            p.setPillarType(type);
            p.setCapacityHoles(type.getDefaultHoles());
            p.setPrice(type.getDefaultPrice());
            p.setStatus(EPillarStatus.ACTIVE);
            p.setGardenSlot(slot);
            p.setLocation(slot.getLocation());
            Pillar saved = pillarRepository.save(p);
            result.allPillars.add(saved);
            result.newlyCreatedPillars.add(saved);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getPaymentStatus(Long rentalId, String username) {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        
        SlotRental rental = slotRentalRepository.findById(rentalId).orElse(null);
        if (rental == null) {
            status.put("error", "Rental not found");
            return status;
        }
        
        // Verify ownership
        if (!rental.getUser().getUsername().equals(username)) {
            status.put("error", "Unauthorized access to rental information");
            return status;
        }
        
        // Get the latest payment transaction for this rental
        List<PaymentTransaction> transactions = paymentTransactionRepository.findByRentalIdOrderByPaymentDateDesc(rentalId);
        PaymentTransaction latestTransaction = transactions.isEmpty() ? null : transactions.get(0);
        
        status.put("rentalId", rental.getId());
        status.put("rentalStatus", rental.getStatus().name());
        status.put("startTime", rental.getStartTime());
        status.put("endTime", rental.getEndTime());
        
        if (latestTransaction != null) {
            status.put("paymentStatus", latestTransaction.getStatus().name());
            status.put("amount", latestTransaction.getAmount());
            status.put("vnpTxnRef", latestTransaction.getVnpTxnRef());
            status.put("transactionDate", latestTransaction.getPaymentDate());
        } else {
            status.put("paymentStatus", "NO_TRANSACTION");
            status.put("amount", null);
            status.put("vnpTxnRef", null);
            status.put("transactionDate", null);
        }
        
        return status;
    }
}
