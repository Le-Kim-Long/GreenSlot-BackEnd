package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.TreePlantingRequestCreateDTO;
import swp490.greeenslot.dto.TreePlantingRequestDTO;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.GardeningTaskRepository;
import swp490.greeenslot.repository.PaymentTransactionRepository;
import swp490.greeenslot.repository.PillarRepository;
import swp490.greeenslot.repository.SensorThresholdRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.TreePlantingRequestRepository;
import swp490.greeenslot.repository.TreeRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.NotificationService;
import swp490.greeenslot.service.TreePlantingService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TreePlantingServiceImpl implements TreePlantingService {

    @Autowired
    private TreePlantingRequestRepository treePlantingRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private TreeRepository treeRepository;

    @Autowired
    private SensorThresholdRepository sensorThresholdRepository;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Autowired(required = false)
    private swp490.greeenslot.config.VNPayUtils vnPayUtils;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private swp490.greeenslot.service.LocationContextService locationContextService;

    private Long getRequestLocationId(TreePlantingRequest request) {
        if (request != null && request.getRental() != null && request.getRental().getGardenSlot() != null) {
            GardenSlot slot = request.getRental().getGardenSlot();
            if (slot.getLocation() != null) {
                return slot.getLocation().getId();
            }
            if (slot.getPillar() != null && slot.getPillar().getLocation() != null) {
                return slot.getPillar().getLocation().getId();
            }
        }
        return null;
    }

    private String getRequestLocationName(TreePlantingRequest request) {
        if (request != null && request.getRental() != null && request.getRental().getGardenSlot() != null) {
            GardenSlot slot = request.getRental().getGardenSlot();
            if (slot.getLocation() != null) {
                return slot.getLocation().getName();
            }
            if (slot.getPillar() != null && slot.getPillar().getLocation() != null) {
                return slot.getPillar().getLocation().getName();
            }
        }
        return null;
    }

    private boolean isRequestAccessible(TreePlantingRequest request, Long locationId) {
        if (locationId == null) return true;
        Long reqLocId = getRequestLocationId(request);
        return reqLocId != null && reqLocId.equals(locationId);
    }

    @Override
    public List<TreePlantingRequestDTO> getAllRequests() {
        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        return treePlantingRequestRepository.findAll().stream()
                .filter(r -> isRequestAccessible(r, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TreePlantingRequestDTO getRequestById(Long id) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        if (locationContextService != null && locationContextService.isLocationManager()) {
            Long locId = getRequestLocationId(request);
            locationContextService.validateLocationAccess(locId);
        }
        return mapToDTO(request);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO createRequest(TreePlantingRequestCreateDTO dto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        
        SlotRental rental = slotRentalRepository.findById(dto.getRentalId())
                .orElseThrow(() -> new IllegalArgumentException("Rental not found with id: " + dto.getRentalId()));
        
        // 1. Check rental ownership
        if (rental.getUser() == null || !rental.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized: You do not own this rental contract.");
        }
        
        // 2. Check rental status is ACTIVE
        if (rental.getStatus() != ERentalStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot request planting: Slot rental is not ACTIVE (current status: " + rental.getStatus() + ").");
        }
        
        // 3. Check rental has not expired
        LocalDateTime now = LocalDateTime.now();
        if (rental.getEndTime() == null || rental.getEndTime().isBefore(now)) {
            throw new IllegalArgumentException("Cannot request planting: Slot rental has already expired.");
        }

        // 3b. Kiểm tra cây đang trồng và yêu cầu theo trụ
        List<TreePlantingRequest> existingReqs = treePlantingRequestRepository.findByRental(rental);
        
        if (dto.getTargetPillarId() != null && dto.getTargetPillarId() > 0) {
            // Nếu chọn cụ thể 1 trụ: kiểm tra xem trụ đó đã có yêu cầu PENDING chưa
            boolean isPillarPending = existingReqs.stream().anyMatch(r -> 
                r.getStatus() == EPlantingRequestStatus.PENDING && 
                r.getTargetPillar() != null && 
                r.getTargetPillar().getId().equals(dto.getTargetPillarId())
            );
            if (isPillarPending) {
                throw new IllegalArgumentException("Trụ này hiện đã có yêu cầu trồng cây đang chờ duyệt.");
            }
        } else {
            // Yêu cầu áp dụng cho toàn bộ các trụ trong ô:
            // Chỉ cho phép khi hợp đồng chưa có cây đang canh tác
            if (rental.getTree() != null) {
                throw new IllegalArgumentException("Cannot request planting for all pillars: This slot already has an active tree planted. " +
                        "Please select a specific empty pillar or wait until current crop is harvested.");
            }
            boolean hasPending = existingReqs.stream().anyMatch(r -> r.getStatus() == EPlantingRequestStatus.PENDING);
            if (hasPending) {
                throw new IllegalArgumentException("Hợp đồng này hiện đã có yêu cầu trồng cây đang chờ xử lý.");
            }
        }

        // 4. Check tree exists and is active
        Tree newTree = treeRepository.findById(dto.getNewTreeId())
                .orElseThrow(() -> new IllegalArgumentException("Tree not found with id: " + dto.getNewTreeId()));
        
        if (Boolean.FALSE.equals(newTree.getIsActive())) {
            throw new IllegalArgumentException("Cannot request planting: Selected tree type is inactive.");
        }
        
        // 5. Check tree growth duration vs remaining rental duration
        long remainingDays = ChronoUnit.DAYS.between(now, rental.getEndTime());
        Integer harvestDays = newTree.getHarvestDays();
        Integer minRentalDays = newTree.getMinRentalDays();
        
        int requiredDays = 0;
        if (harvestDays != null && harvestDays > 0) {
            requiredDays = Math.max(requiredDays, harvestDays);
        }
        if (minRentalDays != null && minRentalDays > 0) {
            requiredDays = Math.max(requiredDays, minRentalDays);
        }
        
        if (requiredDays > 0 && remainingDays < requiredDays) {
            throw new IllegalArgumentException(String.format(
                "Cannot request planting: Tree '%s' requires at least %d days to grow/harvest, but your rental only has %d day(s) remaining (until %s). Please extend your rental duration.",
                newTree.getTreeName(),
                requiredDays,
                remainingDays,
                rental.getEndTime()
            ));
        }
        
        List<Pillar> rentedPillars = rental.getRentedPillars();
        if (rentedPillars == null || rentedPillars.isEmpty()) {
            if (rental.getGardenSlot() != null && rental.getGardenSlot().getPillars() != null) {
                rentedPillars = rental.getGardenSlot().getPillars();
            } else if (rental.getGardenSlot() != null && rental.getGardenSlot().getPillar() != null) {
                rentedPillars = List.of(rental.getGardenSlot().getPillar());
            } else {
                rentedPillars = java.util.Collections.emptyList();
            }
        }

        Pillar targetPillar = null;
        if (dto.getTargetPillarId() != null && dto.getTargetPillarId() > 0) {
            targetPillar = pillarRepository.findById(dto.getTargetPillarId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trụ canh tác với ID: " + dto.getTargetPillarId()));
            
            final Long targetId = targetPillar.getId();
            boolean belongsToRental = false;
            if (!rentedPillars.isEmpty()) {
                belongsToRental = rentedPillars.stream().anyMatch(p -> p.getId().equals(targetId));
            }
            if (!belongsToRental && rental.getGardenSlot() != null && rental.getGardenSlot().getPillars() != null) {
                belongsToRental = rental.getGardenSlot().getPillars().stream().anyMatch(p -> p.getId().equals(targetId));
            }

            if (!belongsToRental) {
                throw new IllegalArgumentException("Trụ " + targetPillar.getPillarCode() + " không thuộc hợp đồng thuê này.");
            }
        }

        java.math.BigDecimal totalTreeCost = java.math.BigDecimal.ZERO;
        if (targetPillar != null) {
            totalTreeCost = newTree.getEffectivePriceForPillar(targetPillar);
        } else if (!rentedPillars.isEmpty()) {
            for (Pillar p : rentedPillars) {
                totalTreeCost = totalTreeCost.add(newTree.getEffectivePriceForPillar(p));
            }
        } else {
            totalTreeCost = newTree.getEffectivePriceSmall();
        }


        TreePlantingRequest request = new TreePlantingRequest();
        request.setRental(rental);
        request.setNewTree(newTree);
        request.setTargetPillar(targetPillar);
        request.setRequestedBy(user);
        request.setStatus(EPlantingRequestStatus.PENDING);
        request.setReason(dto.getReason());
        request.setNotes(dto.getNotes());
        request.setAmount(totalTreeCost);
        
        TreePlantingRequest savedRequest = treePlantingRequestRepository.save(request);

        if (vnPayUtils != null && totalTreeCost.compareTo(java.math.BigDecimal.ZERO) > 0) {
            String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
            String txnRef = "PLANT_" + savedRequest.getId() + "_" + uuid;
            PaymentTransaction txn = new PaymentTransaction();
            txn.setRental(rental);
            txn.setAmount(totalTreeCost);
            txn.setVnpTxnRef(txnRef);
            txn.setPaymentDate(LocalDateTime.now());
            txn.setStatus(EPaymentStatus.PENDING);
            paymentTransactionRepository.save(txn);

            String pillarDesc = targetPillar != null ? ("Trụ " + targetPillar.getPillarCode()) : (rentedPillars.size() + " tru");
            String orderInfo = "GreenSlot - Mua giong cay " + newTree.getTreeName() + " (" + pillarDesc + ")";
            boolean isMobile = Boolean.TRUE.equals(dto.getIsMobile());
            String paymentUrl = vnPayUtils.buildPaymentUrl(txnRef, totalTreeCost, "127.0.0.1", orderInfo, isMobile, dto.getEffectiveRedirectUrl());
            savedRequest.setPaymentUrl(paymentUrl);
            savedRequest = treePlantingRequestRepository.save(savedRequest);

        }

        // Notify location managers about planting request
        if (notificationService != null) {
            String slotNumber = rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A";
            Long locId = getRequestLocationId(savedRequest);
            List<User> managers = locId != null
                    ? userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, locId)
                    : List.of();
            if (managers.isEmpty()) {
                managers = userRepository.findByRoleName(ERole.ROLE_MANAGER);
            }
            String title = "Yêu cầu trồng cây mới";
            String message = String.format("Khách hàng %s yêu cầu trồng cây %s tại ô đất %s.",
                    user.getFullName() != null ? user.getFullName() : username,
                    newTree.getTreeName(),
                    slotNumber);

            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        title,
                        message,
                        "PLANTING_REQUEST_CREATED",
                        savedRequest.getId(),
                        "/dashboard/staff/tree-planting"
                );
            }
        }

        return mapToDTO(savedRequest);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO approveRequest(Long id, String username) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        if (locationContextService != null && locationContextService.isLocationManager()) {
            Long locId = getRequestLocationId(request);
            locationContextService.validateLocationAccess(locId);
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        LocalDateTime now = LocalDateTime.now();
        request.setStatus(EPlantingRequestStatus.APPROVED);
        request.setProcessedBy(user);
        request.setProcessedAt(now);

        // Duyệt xong thì cây yêu cầu mới thực sự được trồng vào ô đất của rental này
        SlotRental rental = request.getRental();
        Tree newTree = request.getNewTree();
        rental.setTree(newTree);
        rental.setTreeStatus(swp490.greeenslot.entity.ETreeStatus.HEALTHY);
        rental.setPlantedAt(now);
        rental.setHarvestReminderSent(false);
        slotRentalRepository.save(rental);

        // Đồng bộ ngưỡng của cây trồng sang cấu hình cảm biến IoT của Trụ (Pillar) thuộc ô đất này
        if (newTree != null) {
            if (request.getTargetPillar() != null && request.getTargetPillar().getPillarCode() != null) {
                syncTreeThresholdsToDevice(request.getTargetPillar().getPillarCode(), newTree);
            } else if (rental.getGardenSlot() != null && rental.getGardenSlot().getPillars() != null) {
                for (Pillar p : rental.getGardenSlot().getPillars()) {
                    if (p.getPillarCode() != null && !p.getPillarCode().isBlank()) {
                        syncTreeThresholdsToDevice(p.getPillarCode(), newTree);
                    }
                }
            } else if (rental.getGardenSlot() != null && rental.getGardenSlot().getPillar() != null) {
                Pillar pillar = rental.getGardenSlot().getPillar();
                if (pillar.getPillarCode() != null && !pillar.getPillarCode().isBlank()) {
                    syncTreeThresholdsToDevice(pillar.getPillarCode(), newTree);
                }
            }
        }

        TreePlantingRequest updatedRequest = treePlantingRequestRepository.save(request);

        // Notify customer about approved planting request
        if (notificationService != null && updatedRequest.getRequestedBy() != null) {
            String slotNumber = (rental.getGardenSlot() != null) ? rental.getGardenSlot().getSlotNumber() : "N/A";
            String treeName = updatedRequest.getNewTree() != null ? updatedRequest.getNewTree().getTreeName() : "cây trồng";
            String title = "Yêu cầu trồng cây đã được duyệt";
            String message = String.format("Yêu cầu trồng cây %s tại ô đất %s của bạn đã được duyệt.", treeName, slotNumber);

            notificationService.createNotification(
                    updatedRequest.getRequestedBy().getId(),
                    title,
                    message,
                    "PLANTING_REQUEST_APPROVED",
                    updatedRequest.getId(),
                    "/dashboard/customer/tree-planting"
            );
        }

        // Tự động tạo nhiệm vụ chăm sóc cây mới trồng để nhân viên nhận việc (chưa gán ai — hiện trong danh sách "công việc khả dụng")
        GardenSlot targetSlot = rental.getGardenSlot();
        if (targetSlot != null) {
            String slotNumber = targetSlot.getSlotNumber();
            String treeName = newTree != null ? newTree.getTreeName() : "cây trồng";

            GardeningTask careTask = new GardeningTask();
            careTask.setTaskName("Kiểm tra & chăm sóc cây mới trồng: " + treeName + " - Ô " + slotNumber);
            careTask.setDescription(String.format(
                    "Cây %s vừa được duyệt trồng tại ô %s. Vui lòng kiểm tra đất, cảm biến và chăm sóc ban đầu cho cây.",
                    treeName, slotNumber));
            careTask.setStatus(ETaskStatus.PENDING);
            careTask.setTaskType(ETaskType.MAINTENANCE);
            careTask.setTargetSlot(targetSlot);
            careTask.setRequestedBy(request.getRequestedBy());
            careTask.setAssignedStaff(null);
            careTask.setCreatedAt(now);
            GardeningTask savedCareTask = gardeningTaskRepository.save(careTask);

            // Báo cho toàn bộ nhân viên tại location đó biết có việc mới cần nhận
            if (notificationService != null) {
                Long locId = getRequestLocationId(request);
                List<User> staffList = locId != null
                        ? userRepository.findByRoleNameAndLocation(ERole.ROLE_GARDEN_STAFF, locId)
                        : List.of();
                String staffTitle = "Có cây mới cần chăm sóc";
                String staffMessage = String.format("Cây %s vừa được duyệt trồng tại ô %s. Vào mục nhiệm vụ để nhận việc chăm sóc.",
                        treeName, slotNumber);
                for (User staff : staffList) {
                    notificationService.createNotification(
                            staff.getId(),
                            staffTitle,
                            staffMessage,
                            "NEW_CARE_TASK_AVAILABLE",
                            savedCareTask.getId(),
                            "/dashboard/garden-staff"
                    );
                }
            }
        }

        return mapToDTO(updatedRequest);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO rejectRequest(Long id, String reason, String username) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        if (locationContextService != null && locationContextService.isLocationManager()) {
            Long locId = getRequestLocationId(request);
            locationContextService.validateLocationAccess(locId);
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        request.setStatus(EPlantingRequestStatus.REJECTED);
        request.setProcessedBy(user);
        request.setProcessedAt(LocalDateTime.now());
        if (reason != null) {
            request.setNotes(reason);
        }
        
        TreePlantingRequest updatedRequest = treePlantingRequestRepository.save(request);

        // Notify customer about rejected planting request
        if (notificationService != null && updatedRequest.getRequestedBy() != null) {
            String slotNumber = (updatedRequest.getRental() != null && updatedRequest.getRental().getGardenSlot() != null)
                    ? updatedRequest.getRental().getGardenSlot().getSlotNumber() : "N/A";
            String treeName = updatedRequest.getNewTree() != null ? updatedRequest.getNewTree().getTreeName() : "cây trồng";
            String title = "Yêu cầu trồng cây bị từ chối";
            String message = String.format("Yêu cầu trồng cây %s tại ô đất %s bị từ chối. Lý do: %s",
                    treeName, slotNumber, reason != null ? reason : "Không có lý do cụ thể");

            notificationService.createNotification(
                    updatedRequest.getRequestedBy().getId(),
                    title,
                    message,
                    "PLANTING_REQUEST_REJECTED",
                    updatedRequest.getId(),
                    "/dashboard/customer/tree-planting"
            );
        }

        return mapToDTO(updatedRequest);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO completeRequest(Long id, String username) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        if (locationContextService != null && locationContextService.isLocationManager()) {
            Long locId = getRequestLocationId(request);
            locationContextService.validateLocationAccess(locId);
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        request.setStatus(EPlantingRequestStatus.COMPLETED);
        request.setProcessedBy(user);
        request.setProcessedAt(LocalDateTime.now());
        
        SlotRental rental = request.getRental();
        rental.setTree(request.getNewTree());
        rental.setTreeStatus(swp490.greeenslot.entity.ETreeStatus.HEALTHY);
        slotRentalRepository.save(rental);
        
        TreePlantingRequest updatedRequest = treePlantingRequestRepository.save(request);

        // Notify customer about completed planting request
        if (notificationService != null && updatedRequest.getRequestedBy() != null) {
            String slotNumber = (rental.getGardenSlot() != null) ? rental.getGardenSlot().getSlotNumber() : "N/A";
            String treeName = updatedRequest.getNewTree() != null ? updatedRequest.getNewTree().getTreeName() : "cây trồng";
            String title = "Đã hoàn thành trồng cây";
            String message = String.format("Cây %s đã được trồng thành công vào ô đất %s.", treeName, slotNumber);

            notificationService.createNotification(
                    updatedRequest.getRequestedBy().getId(),
                    title,
                    message,
                    "PLANTING_REQUEST_COMPLETED",
                    updatedRequest.getId(),
                    "/dashboard/customer/tree-planting"
            );
        }

        return mapToDTO(updatedRequest);
    }

    @Override
    public List<TreePlantingRequestDTO> getRequestsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return treePlantingRequestRepository.findByRequestedBy(user).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TreePlantingRequestDTO> getPendingRequests() {
        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        return treePlantingRequestRepository.findByStatus(EPlantingRequestStatus.PENDING).stream()
                .filter(r -> isRequestAccessible(r, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TreePlantingRequestDTO mapToDTO(TreePlantingRequest request) {
        return new TreePlantingRequestDTO(
                request.getId(),
                request.getRental() != null ? request.getRental().getId() : null,
                request.getRental() != null && request.getRental().getGardenSlot() != null ?
                    request.getRental().getGardenSlot().getSlotNumber() : null,
                getRequestLocationId(request),
                getRequestLocationName(request),
                request.getNewTree() != null ? request.getNewTree().getId() : null,
                request.getNewTree() != null ? request.getNewTree().getTreeName() : null,
                request.getRequestedBy() != null ? request.getRequestedBy().getId() : null,
                request.getRequestedBy() != null ? request.getRequestedBy().getFullName() : null,
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getReason(),
                request.getNotes(),
                request.getRequestedAt(),
                request.getProcessedAt(),
                request.getProcessedBy() != null ? request.getProcessedBy().getId() : null,
                request.getProcessedBy() != null ? request.getProcessedBy().getFullName() : null,
                request.getAmount(),
                request.getPaymentUrl(),
                request.getTargetPillar() != null ? request.getTargetPillar().getId() : null,
                request.getTargetPillar() != null ? request.getTargetPillar().getPillarCode() : null
        );
    }

    private void syncTreeThresholdsToDevice(String deviceId, Tree tree) {
        if (tree.getSoilMoistureMin() != null && tree.getSoilMoistureMax() != null) {
            updateOrCreateThreshold(deviceId, "SOIL_MOISTURE", tree.getSoilMoistureMin(), tree.getSoilMoistureMax());
        }
        if (tree.getLightMin() != null && tree.getLightMax() != null) {
            updateOrCreateThreshold(deviceId, "LIGHT_INTENSITY", tree.getLightMin(), tree.getLightMax());
        }
        if (tree.getPhMin() != null && tree.getPhMax() != null) {
            updateOrCreateThreshold(deviceId, "PH", tree.getPhMin(), tree.getPhMax());
        }
    }

    private void updateOrCreateThreshold(String deviceId, String sensorType, Double minVal, Double maxVal) {
        Optional<SensorThreshold> existing = sensorThresholdRepository.findByDeviceIdAndSensorType(deviceId, sensorType);
        SensorThreshold threshold;
        if (existing.isPresent()) {
            threshold = existing.get();
            threshold.setMinValue(minVal);
            threshold.setMaxValue(maxVal);
        } else {
            threshold = new SensorThreshold();
            threshold.setDeviceId(deviceId);
            threshold.setSensorType(sensorType);
            threshold.setMinValue(minVal);
            threshold.setMaxValue(maxVal);
        }
        sensorThresholdRepository.save(threshold);
    }
}
