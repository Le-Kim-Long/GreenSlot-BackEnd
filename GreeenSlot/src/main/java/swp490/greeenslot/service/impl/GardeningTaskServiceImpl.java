package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.GardeningTaskService;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GardeningTaskServiceImpl implements GardeningTaskService {

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private TreePlantingRequestRepository treePlantingRequestRepository;

    @Autowired
    private StaffScheduleRepository staffScheduleRepository;

    @Autowired
    private swp490.greeenslot.service.NotificationService notificationService;

    @Autowired
    private swp490.greeenslot.service.FirebaseMessagingService firebaseMessagingService;

    @Autowired
    private swp490.greeenslot.service.LocationContextService locationContextService;

    @Autowired
    private swp490.greeenslot.service.HarvestHistoryService harvestHistoryService;

    @Autowired
    private swp490.greeenslot.repository.HarvestHistoryRepository harvestHistoryRepository;

    @Autowired(required = false)
    private AlertRepository alertRepository;

    @Autowired(required = false)
    private AlertProcessingLogRepository alertProcessingLogRepository;

    private Long getSlotLocationId(GardenSlot slot) {
        if (slot != null) {
            if (slot.getLocation() != null) {
                return slot.getLocation().getId();
            }
            if (slot.getPillar() != null && slot.getPillar().getLocation() != null) {
                return slot.getPillar().getLocation().getId();
            }
        }
        return null;
    }

    private List<User> findLocationManagers(GardenSlot slot) {
        Long locationId = getSlotLocationId(slot);
        return locationId != null
                ? userRepository.findManagersForLocation(ERole.ROLE_LOCATION_MANAGER, locationId)
                : userRepository.findByRoleNames(List.of(ERole.ROLE_LOCATION_MANAGER, ERole.ROLE_MANAGER, ERole.ROLE_ADMIN));
    }

    @Override
    @Transactional
    public GardeningTask requestService(ServiceRequestDTO request, String username) {
        LocalDateTime now = LocalDateTime.now();
        // Validate active rental for the slot
        SlotRental rental = slotRentalRepository.findActiveRentalBySlotAndUser(request.getSlotId(), username, now)
                .orElseThrow(() -> new IllegalArgumentException("No active rental found for slot ID " + request.getSlotId() + " belonging to customer " + username));

        if (rental.getStatus() != ERentalStatus.ACTIVE) {
            throw new IllegalArgumentException("Service request denied: Slot rental is not ACTIVE (current status: " + rental.getStatus() + ").");
        }
        if (rental.getEndTime() != null && rental.getEndTime().isBefore(now)) {
            throw new IllegalArgumentException("Service request denied: Slot rental has expired.");
        }

        // Fetch ServiceType
        ServiceType serviceType = serviceTypeRepository.findById(request.getServiceTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Service type not found with ID " + request.getServiceTypeId()));

        // Fetch GardenSlot
        GardenSlot slot = gardenSlotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID " + request.getSlotId()));

        // Create new GardeningTask
        GardeningTask task = new GardeningTask();
        task.setTaskName(serviceType.getServiceName());
        task.setDescription(request.getDescription() != null ? request.getDescription() : "Customer requested service: " + serviceType.getServiceName());
        task.setStatus(ETaskStatus.PENDING);
        task.setTaskType(ETaskType.SERVICE_REQUEST);
        task.setTargetSlot(slot);
        task.setRequestedBy(userRepository.findByUsername(username).orElse(null));
        task.setAssignedStaff(null); // Unassigned initially
        task.setCreatedAt(now);

        GardeningTask savedTask = gardeningTaskRepository.save(task);

        // Notify location managers about new service request
        if (notificationService != null) {
            List<User> managers = findLocationManagers(slot);
            String title = "Yêu cầu dịch vụ mới";
            String message = String.format("Khách hàng %s đã yêu cầu dịch vụ '%s' cho ô đất %s.",
                    username, serviceType.getServiceName(), slot.getSlotNumber());
            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        title,
                        message,
                        "SERVICE_REQUEST_CREATED",
                        savedTask.getId(),
                        "/dashboard/staff/tasks"
                );
            }
        }

        return savedTask;
    }

    @Override
    @Transactional
    public GardeningTask createTask(TaskAssignmentDTO request) {
        if (request.getTaskName() == null || request.getTaskName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }
        if (request.getTaskType() == null) {
            throw new IllegalArgumentException("Task type is required");
        }
        if (request.getTargetSlotId() == null) {
            throw new IllegalArgumentException("Target slot ID is required");
        }

        GardenSlot slot = gardenSlotRepository.findById(request.getTargetSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID " + request.getTargetSlotId()));

        ETaskType type;
        try {
            type = ETaskType.valueOf(request.getTaskType().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid task type. Valid types are: MAINTENANCE, CLEANING, PLANTING, INSPECTION");
        }

        if (type == ETaskType.SERVICE_REQUEST) {
            throw new IllegalArgumentException("SERVICE_REQUEST tasks cannot be created directly by manager. They must originate from customer requests.");
        }

        GardeningTask task = new GardeningTask();
        task.setTaskName(request.getTaskName());
        task.setDescription(request.getDescription());
        task.setStatus(ETaskStatus.PENDING);
        task.setTaskType(type);
        task.setTargetSlot(slot);
        task.setAssignedStaff(null); // Unassigned initially
        if (request.getEvidenceImageUrl() != null && !request.getEvidenceImageUrl().trim().isEmpty()) {
            task.setEvidenceImageUrl(request.getEvidenceImageUrl().trim());
        }
        task.setCreatedAt(LocalDateTime.now());

        return gardeningTaskRepository.save(task);
    }

    @PostConstruct
    public void unassignOrphanPendingSetupTasks() {
        try {
            List<GardeningTask> pendingSetupTasks = gardeningTaskRepository.findAll().stream()
                    .filter(t -> t.getStatus() == ETaskStatus.PENDING && t.getTaskName() != null && t.getTaskName().startsWith("Lắp đặt bổ sung") && t.getAssignedStaff() != null)
                    .collect(Collectors.toList());
            for (GardeningTask t : pendingSetupTasks) {
                t.setAssignedStaff(null);
                gardeningTaskRepository.save(t);
            }
        } catch (Exception e) {
            // Ignore any errors during startup
        }
    }

    @Override
    @Transactional
    public GardeningTask assignStaffToTask(Long taskId, TaskAssignmentDTO request) {
        // Fetch the task
        GardeningTask task = gardeningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + taskId));

        Long taskLocId = getSlotLocationId(task.getTargetSlot());
        if (taskLocId != null) {
            locationContextService.validateLocationAccess(taskLocId);
        }

        // Support unassigning (bỏ gán)
        if (request.getStaffId() == null || request.getStaffId() <= 0) {
            task.setAssignedStaff(null);
            return gardeningTaskRepository.save(task);
        }

        // Fetch target staff and check role
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found with ID " + request.getStaffId()));

        boolean hasStaffRole = staff.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_GARDEN_STAFF);

        if (!hasStaffRole) {
            throw new IllegalArgumentException("User with ID " + request.getStaffId() + " does not have ROLE_GARDEN_STAFF");
        }

        if (taskLocId != null && staff.getLocation() != null && !taskLocId.equals(staff.getLocation().getId())) {
            throw new IllegalArgumentException("Nhân viên được chọn không thuộc cơ sở của ô vườn này.");
        }

        // Assign staff
        task.setAssignedStaff(staff);
        GardeningTask savedTask = gardeningTaskRepository.save(task);

        String slotNumber = task.getTargetSlot() != null ? task.getTargetSlot().getSlotNumber() : "N/A";

        // Notify staff about task assignment
        if (notificationService != null) {
            notificationService.createNotification(
                    staff.getId(),
                    "Phân công nhiệm vụ mới",
                    String.format("Bạn đã được phân công nhiệm vụ: %s tại ô %s",
                            task.getTaskName(), slotNumber),
                    "TASK_ASSIGNMENT",
                    task.getId(),
                    "/dashboard/garden-staff/schedules"
            );
        }

        if (firebaseMessagingService != null) {
            firebaseMessagingService.sendPushNotification(
                    staff.getId(),
                    "Phân công nhiệm vụ mới",
                    String.format("Nhiệm vụ: %s - Ô: %s", task.getTaskName(), slotNumber)
            );
        }

        return savedTask;
    }

    @Override
    public List<GardeningTask> getMyTasks(String username) {
        User staff = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);
        if (staff == null) {
            return Collections.emptyList();
        }
        List<GardeningTask> myTasks = gardeningTaskRepository.findByAssignedStaffIdOrderByCreatedAtDesc(staff.getId());
        if (staffScheduleRepository == null) {
            return myTasks;
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        List<StaffSchedule> staffTodaySchedules = staffScheduleRepository.findActiveSchedulesOnDate(today).stream()
                .filter(s -> s.getStaff() != null && s.getStaff().getId().equals(staff.getId()))
                .collect(Collectors.toList());

        return myTasks.stream().filter(task -> {
            boolean isSensorAlertTask = task.getTaskName() != null && (
                    task.getTaskName().startsWith("Khẩn cấp: Xử lý cảnh báo") ||
                    task.getTaskName().contains("cảnh báo") ||
                    task.getTaskName().contains("Cảnh báo"));
            if (!isSensorAlertTask) {
                return true;
            }
            if (task.getTargetSlot() == null) {
                return true;
            }
            Long slotId = task.getTargetSlot().getId();
            Long locId = getSlotLocationId(task.getTargetSlot());
            // Nếu hôm nay staff có lịch trực ca: kiểm tra ca trực trùng ô hoặc thuộc cơ sở này
            if (!staffTodaySchedules.isEmpty()) {
                return staffTodaySchedules.stream().anyMatch(sch ->
                        (sch.getGardenSlot() != null && sch.getGardenSlot().getId().equals(slotId)) ||
                        (sch.getLocation() != null && sch.getLocation().getId().equals(locId))
                );
            }
            // Nếu không có cấu hình lịch trực ca riêng hôm nay, nhưng task đã được hệ thống đích danh phân công cho staff: hiển thị để staff xử lý
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public List<GardeningTask> getAvailableTasks(String username) {
        // Toàn bộ công việc bắt buộc phải do Quản lý cơ sở phân công, không còn cơ chế tự nhận việc
        return List.of();
    }

    @Override
    @Transactional
    public GardeningTask claimTask(Long taskId, String username) {
        throw new org.springframework.security.access.AccessDeniedException(
                "Hệ thống chỉ cho phép Quản lý cơ sở phân công công việc. Nhân viên không thể tự nhận việc."
        );
    }

    @Override
    @Transactional
    public GardeningTask notifyHarvestChoice(Long taskId, String username) {
        GardeningTask task = gardeningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + taskId));

        if (task.getTaskType() != ETaskType.HARVEST) {
            throw new IllegalArgumentException("Only HARVEST tasks support the harvest-choice notification");
        }
        if (task.getAssignedStaff() == null || !task.getAssignedStaff().getUsername().equals(username)) {
            throw new IllegalArgumentException("Task is not assigned to the authenticated staff member " + username);
        }
        if (task.getTargetSlot() == null) {
            throw new IllegalArgumentException("Task has no target slot");
        }

        List<SlotRental> activeRentals = slotRentalRepository.findActiveRentals(task.getTargetSlot().getId(), LocalDateTime.now());
        if (activeRentals.isEmpty()) {
            throw new IllegalArgumentException("No active rental found for this slot");
        }
        SlotRental rental = activeRentals.get(0);
        if (rental.getUser() == null) {
            throw new IllegalArgumentException("Rental has no associated customer");
        }

        rental.setHarvestNotifiedAt(LocalDateTime.now());
        rental.setHarvestDecision(null);
        rental.setHarvestPillarCode(task.getPillarCodes());
        rental.setHarvestEvidenceImageUrl(task.getEvidenceImageUrl());
        rental.setHarvestStaffNotes(task.getStaffNotes());
        slotRentalRepository.save(rental);

        String staffName = task.getAssignedStaff() != null && task.getAssignedStaff().getFullName() != null 
                ? task.getAssignedStaff().getFullName().trim() : "Nhân viên vườn";
        if (staffName.startsWith("Nhân viên ")) {
            staffName = staffName.substring(10).trim();
        }
        String slotNumber = task.getTargetSlot().getSlotNumber();

        String effectivePillarCodes = (task.getPillarCodes() != null && !task.getPillarCodes().isBlank())
                ? task.getPillarCodes()
                : "";
        if (effectivePillarCodes.isBlank()) {
            if (rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty()) {
                effectivePillarCodes = rental.getRentedPillars().stream()
                        .map(swp490.greeenslot.entity.Pillar::getPillarCode)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.joining(", "));
            } else if (task.getTargetSlot().getPillars() != null) {
                effectivePillarCodes = task.getTargetSlot().getPillars().stream()
                        .map(swp490.greeenslot.entity.Pillar::getPillarCode)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.joining(", "));
            }
        }
        String effectiveTreeName = (task.getTreeName() != null && !task.getTreeName().isBlank())
                ? task.getTreeName()
                : (rental.getTree() != null ? rental.getTree().getTreeName() : "cây trồng");

        String pillarText = effectivePillarCodes.isBlank() ? "Toàn bộ trụ" : ("Trụ " + effectivePillarCodes);
        task.setPillarCodes(effectivePillarCodes.isBlank() ? null : effectivePillarCodes);
        task.setTreeName(effectiveTreeName);
        gardeningTaskRepository.save(task);

        String message = String.format(
                "Nhân viên %s báo: cây %s tại ô đất %s (%s) đã sẵn sàng thu hoạch. Bạn muốn tự thu hoạch hay nhờ nhân viên thu hoạch giúp?",
                staffName, effectiveTreeName, slotNumber, pillarText);

        String notifTitle = "Sẵn sàng thu hoạch: Ô " + slotNumber + (!effectivePillarCodes.isBlank() ? " (" + pillarText + ")" : "");

        if (notificationService != null) {
            notificationService.createNotification(
                    rental.getUser().getId(),
                    notifTitle,
                    message,
                    "HARVEST_CHOICE",
                    rental.getId(),
                    "/dashboard/customer/rentals"
            );
        }

        if (firebaseMessagingService != null) {
            firebaseMessagingService.sendPushNotification(
                    rental.getUser().getId(),
                    notifTitle,
                    String.format("%s báo cây %s tại ô %s (%s) đã sẵn sàng thu hoạch", staffName, effectiveTreeName, slotNumber, pillarText)
            );
        }

        return task;
    }

    @Override
    public List<GardeningTask> getAllTasks() {
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        List<GardeningTask> all = gardeningTaskRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        if (targetLocationId == null) {
            return all;
        }
        return all.stream().filter(t -> {
            Long slotLocId = getSlotLocationId(t.getTargetSlot());
            if (slotLocId != null) {
                return targetLocationId.equals(slotLocId);
            }
            if (t.getAssignedStaff() != null && t.getAssignedStaff().getLocation() != null) {
                return targetLocationId.equals(t.getAssignedStaff().getLocation().getId());
            }
            return false;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public GardeningTask updateTaskStatus(Long taskId, TaskStatusUpdateDTO request, String username) {
        GardeningTask task = gardeningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + taskId));

        // Validate that the task is assigned to the requesting staff
        if (task.getAssignedStaff() == null || !task.getAssignedStaff().getUsername().equals(username)) {
            throw new IllegalArgumentException("Task is not assigned to the authenticated staff member " + username);
        }

        ETaskStatus newStatus;
        try {
            newStatus = ETaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid task status. Must be PENDING, IN_PROGRESS, PENDING_APPROVAL, or COMPLETED");
        }

        // Validate status transition sequence
        if (task.getStatus() == ETaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify status of a COMPLETED task");
        }
        
        if (task.getStatus() == ETaskStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Task is currently under review by manager. Cannot modify.");
        }

        if (task.getStatus() == ETaskStatus.PENDING && (newStatus == ETaskStatus.PENDING_APPROVAL || newStatus == ETaskStatus.COMPLETED)) {
            throw new IllegalArgumentException("Cannot transition directly from PENDING to PENDING_APPROVAL/COMPLETED. Must go through IN_PROGRESS first.");
        }
        
        if (newStatus == ETaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Staff cannot mark task as COMPLETED directly. Must mark as PENDING_APPROVAL and provide evidence.");
        }

        if (newStatus == ETaskStatus.PENDING_APPROVAL) {
            // 1. Thu thập ảnh bằng chứng & ghi chú nhân viên
            java.util.List<String> allImages = new java.util.ArrayList<>();
            if (request.getEvidenceImageUrl() != null && !request.getEvidenceImageUrl().trim().isEmpty()) {
                allImages.add(request.getEvidenceImageUrl().trim());
            }
            if (request.getEquipmentBindings() != null) {
                for (swp490.greeenslot.dto.PillarEquipmentBindingDTO b : request.getEquipmentBindings()) {
                    if (b.getEvidenceImageUrl() != null && !b.getEvidenceImageUrl().trim().isEmpty()) {
                        String bImg = b.getEvidenceImageUrl().trim();
                        if (!allImages.contains(bImg)) {
                            allImages.add(bImg);
                        }
                    }
                }
            }
            if (allImages.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng cung cấp hình ảnh bằng chứng công việc khi nộp duyệt");
            }
            task.setEvidenceImageUrl(String.join(",", allImages));

            if (request.getStaffNotes() != null && !request.getStaffNotes().isBlank()) {
                task.setStaffNotes(request.getStaffNotes());
            } else if (request.getEquipmentBindings() != null && !request.getEquipmentBindings().isEmpty()) {
                StringBuilder notesSb = new StringBuilder();
                for (swp490.greeenslot.dto.PillarEquipmentBindingDTO b : request.getEquipmentBindings()) {
                    if (b.getNotes() != null && !b.getNotes().isBlank()) {
                        if (notesSb.length() > 0) notesSb.append("\n");
                        notesSb.append("[").append(b.getPillarCode()).append("]: ").append(b.getNotes().trim());
                    }
                }
                if (notesSb.length() > 0) {
                    task.setStaffNotes(notesSb.toString());
                }
            }

            // 2. Process equipment bindings if provided (Staff attaching equipment to pillars)
            if (request.getEquipmentBindings() != null && !request.getEquipmentBindings().isEmpty()) {
                Location taskLocation = (task.getTargetSlot() != null && task.getTargetSlot().getLocation() != null)
                        ? task.getTargetSlot().getLocation()
                        : (task.getAssignedStaff() != null ? task.getAssignedStaff().getLocation() : null);

                for (swp490.greeenslot.dto.PillarEquipmentBindingDTO binding : request.getEquipmentBindings()) {
                    if (binding.getPillarCode() == null || binding.getPillarCode().isBlank()) {
                        continue;
                    }
                    String pCode = binding.getPillarCode().trim();
                    Pillar pillar = pillarRepository.findByPillarCode(pCode).orElse(null);
                    if (pillar == null && task.getTargetSlot() != null) {
                        if (task.getTargetSlot().getPillars() != null && !task.getTargetSlot().getPillars().isEmpty()) {
                            pillar = task.getTargetSlot().getPillars().get(0);
                        } else if (task.getTargetSlot().getPillar() != null) {
                            pillar = task.getTargetSlot().getPillar();
                        }
                    }
                    if (pillar == null) {
                        throw new IllegalArgumentException("Không tìm thấy trụ với mã: " + pCode);
                    }

                    if (binding.getEquipmentId() != null && binding.getEquipmentId() > 0) {
                        Equipment existingEq = equipmentRepository.findById(binding.getEquipmentId())
                                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị ID: " + binding.getEquipmentId()));
                        
                        int takeQty = (binding.getQuantity() != null && binding.getQuantity() > 0) ? binding.getQuantity() : 1;
                        int currentStock = existingEq.getQuantity() != null ? existingEq.getQuantity() : 1;
                        if (currentStock < takeQty) {
                            throw new IllegalArgumentException(String.format(
                                "Thiết bị '%s' trong kho chỉ còn %d cái, không đủ số lượng lấy ra (%d cái).",
                                existingEq.getEquipmentName(), currentStock, takeQty
                            ));
                        }

                        if (currentStock > takeQty) {
                            // Giảm số lượng tồn kho còn lại của lô thiết bị
                            existingEq.setQuantity(currentStock - takeQty);
                            equipmentRepository.save(existingEq);

                            // Tạo bản ghi thiết bị đã gắn vào trụ
                            Equipment deployedEq = new Equipment();
                            deployedEq.setEquipmentName(existingEq.getEquipmentName());
                            String deployedSerial = binding.getNewSerialNumber() != null && !binding.getNewSerialNumber().isBlank() 
                                    ? binding.getNewSerialNumber().trim().toUpperCase() 
                                    : (existingEq.getSerialNumber() != null ? existingEq.getSerialNumber() + "-" + pCode : "EQ-" + pCode + "-" + (System.currentTimeMillis() % 10000));
                            if (equipmentRepository.findBySerialNumber(deployedSerial).isPresent()) {
                                deployedSerial = deployedSerial + "-" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                            }
                            deployedEq.setSerialNumber(deployedSerial);
                            deployedEq.setDescription(existingEq.getDescription());
                            deployedEq.setStatus(EEquipmentStatus.IN_USE);
                            deployedEq.setPillar(pillar);
                            deployedEq.setLocation(taskLocation != null ? taskLocation : existingEq.getLocation());
                            deployedEq.setQuantity(takeQty);
                            deployedEq.setPurchaseDate(existingEq.getPurchaseDate());
                            deployedEq.setLastMaintenanceDate(LocalDateTime.now());
                            deployedEq.setImageUrl(existingEq.getImageUrl());
                            equipmentRepository.save(deployedEq);
                        } else {
                            // Lấy hết toàn bộ số lượng của thiết bị này trong kho để gắn vào trụ
                            existingEq.setPillar(pillar);
                            existingEq.setStatus(EEquipmentStatus.IN_USE);
                            existingEq.setQuantity(takeQty);
                            if (taskLocation != null) {
                                existingEq.setLocation(taskLocation);
                            }
                            equipmentRepository.save(existingEq);
                        }
                    } else if (binding.getNewSerialNumber() != null && !binding.getNewSerialNumber().trim().isEmpty()) {
                        String cleanSerial = binding.getNewSerialNumber().trim().toUpperCase();
                        Equipment eq = equipmentRepository.findBySerialNumber(cleanSerial).orElse(null);
                        int newQty = (binding.getQuantity() != null && binding.getQuantity() > 0) ? binding.getQuantity() : 1;
                        if (eq == null) {
                            eq = new Equipment();
                            String eqName = (binding.getNewEquipmentName() != null && !binding.getNewEquipmentName().trim().isEmpty())
                                    ? binding.getNewEquipmentName().trim()
                                    : "Mạch điều khiển ESP32";
                            eq.setEquipmentName(eqName);
                            eq.setSerialNumber(cleanSerial);
                        }
                        eq.setQuantity(newQty);
                        eq.setPillar(pillar);
                        eq.setStatus(EEquipmentStatus.IN_USE);
                        if (taskLocation != null) {
                            eq.setLocation(taskLocation);
                        }
                        equipmentRepository.save(eq);
                    }
                }
            }

            // 3. Ràng buộc: Đối với task lắp đặt bổ sung trụ / gắn thiết bị mới
            String tName = task.getTaskName() != null ? task.getTaskName().toLowerCase() : "";
            String tDesc = task.getDescription() != null ? task.getDescription().toLowerCase() : "";
            boolean isPillarSetupTask = (task.getPillarCodes() != null && !task.getPillarCodes().isBlank()) && (
                    tName.contains("lắp") || tName.contains("lap") ||
                    tName.contains("bổ sung") || tName.contains("bo sung") ||
                    tName.contains("thiết bị") || tName.contains("thiet bi") ||
                    tName.contains("iot") || tName.contains("cảm biến") ||
                    tName.contains("gắn") || tName.contains("gán") ||
                    tDesc.contains("lắp") || tDesc.contains("thiết bị") || tDesc.contains("iot")
            );

            if (isPillarSetupTask) {
                String[] pCodes = task.getPillarCodes().split(",");
                boolean hasAnyEquipment = false;
                for (String codeRaw : pCodes) {
                    String pCode = codeRaw.trim();
                    if (pCode.isEmpty()) continue;
                    Pillar pillar = pillarRepository.findByPillarCode(pCode).orElse(null);
                    if (pillar != null) {
                        List<Equipment> attachedEquipments = equipmentRepository.findByPillar(pillar);
                        if (attachedEquipments != null && !attachedEquipments.isEmpty()) {
                            hasAnyEquipment = true;
                            break;
                        }
                    }
                }
                if (!hasAnyEquipment) {
                    throw new IllegalArgumentException(
                            "Nhiệm vụ này yêu cầu gắn thiết bị IoT (Mạch điều khiển/Cảm biến). Vui lòng chọn thiết bị từ kho hoặc nhập mã Serial của thiết bị đã lắp trước khi nộp duyệt."
                    );
                }
            }
            
            // Clear previous rejection reason if any
            if (task.getStatus() == ETaskStatus.REJECTED) {
                task.setRejectionReason(null);
            }

            // Notify location managers about task submission
            if (notificationService != null) {
                List<User> managers = findLocationManagers(task.getTargetSlot());
                String slotNumber = task.getTargetSlot() != null ? task.getTargetSlot().getSlotNumber() : "N/A";
                String title = "Nhiệm vụ chờ duyệt";
                String message = String.format("Nhân viên %s đã nộp bằng chứng hoàn thành nhiệm vụ '%s' tại ô %s. Vui lòng kiểm tra và duyệt.",
                        username, task.getTaskName(), slotNumber);
                for (User manager : managers) {
                    notificationService.createNotification(
                            manager.getId(),
                            title,
                            message,
                            "TASK_SUBMITTED",
                            task.getId(),
                            "/dashboard/staff/tasks"
                    );
                }
            }
        }

        task.setStatus(newStatus);
        return gardeningTaskRepository.save(task);
    }

    @Override
    @Transactional
    public GardeningTask reportIssue(Long taskId, IssueReportRequestDTO request, String username) {
        GardeningTask originalTask = gardeningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + taskId));

        // Validate that the task is assigned to the requesting staff
        if (originalTask.getAssignedStaff() == null || !originalTask.getAssignedStaff().getUsername().equals(username)) {
            throw new IllegalArgumentException("Original task is not assigned to the authenticated staff member " + username);
        }

        // Create new GardeningTask of type MAINTENANCE representing the issue
        GardeningTask issueTask = new GardeningTask();
        issueTask.setTaskName("Báo cáo sự cố: " + request.getIssueTitle());
        issueTask.setDescription("Nhân viên " + username + " báo cáo sự cố trên công việc #" + taskId + ": " + request.getDescription());
        issueTask.setStatus(ETaskStatus.PENDING);
        issueTask.setTaskType(ETaskType.MAINTENANCE);
        issueTask.setTargetSlot(originalTask.getTargetSlot());
        issueTask.setEvidenceImageUrl(request.getEvidenceImageUrl()); // Can be optional or populated
        issueTask.setAssignedStaff(null); // Left unassigned for manager review
        issueTask.setCreatedAt(LocalDateTime.now());
        
        GardeningTask savedIssue = gardeningTaskRepository.save(issueTask);

        // Update original task
        originalTask.setStatus(ETaskStatus.CANCELLED);
        originalTask.setDescription(originalTask.getDescription() + "\n[Tạm dừng do sự cố: " + request.getIssueTitle() + "]");
        gardeningTaskRepository.save(originalTask);

        // Notify location managers about reported issue
        if (notificationService != null) {
            List<User> managers = findLocationManagers(originalTask.getTargetSlot());
            String slotNumber = originalTask.getTargetSlot() != null ? originalTask.getTargetSlot().getSlotNumber() : "N/A";
            String title = "Báo cáo sự cố từ nhân viên";
            String message = String.format("Nhân viên %s báo cáo sự cố tại ô %s: %s - %s",
                    username, slotNumber, request.getIssueTitle(), request.getDescription());
            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        title,
                        message,
                        "TASK_ISSUE",
                        savedIssue.getId(),
                        "/dashboard/staff/tasks"
                );
            }
        }

        return savedIssue;
    }

    @Override
    @Transactional
    public GardeningTask reviewTaskEvidence(Long taskId, TaskReviewRequestDTO request) {
        GardeningTask task = gardeningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + taskId));

        if (task.getStatus() != ETaskStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Task must be in PENDING_APPROVAL status to be reviewed. Current status: " + task.getStatus());
        }

        String slotNumber = task.getTargetSlot() != null ? task.getTargetSlot().getSlotNumber() : "N/A";

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            task.setStatus(ETaskStatus.COMPLETED);
            task.setRejectionReason(null);

            // Notify staff
            if (task.getAssignedStaff() != null && notificationService != null) {
                notificationService.createNotification(
                        task.getAssignedStaff().getId(),
                        "Nhiệm vụ đã được duyệt",
                        String.format("Nhiệm vụ '%s' tại ô %s đã được quản lý phê duyệt.", task.getTaskName(), slotNumber),
                        "TASK_APPROVED",
                        task.getId(),
                        "/dashboard/garden-staff/schedules"
                );
            }

            // If requested by customer and not a harvest task, notify customer
            if (task.getRequestedBy() != null && notificationService != null && task.getTaskType() != ETaskType.HARVEST) {
                notificationService.createNotification(
                        task.getRequestedBy().getId(),
                        "Yêu cầu chăm sóc hoàn tất",
                        String.format("Yêu cầu dịch vụ '%s' tại ô đất %s đã được hoàn thành và nghiệm thu.", task.getTaskName(), slotNumber),
                        "TASK_COMPLETED",
                        task.getId(),
                        "/dashboard/customer/rentals"
                );
            }

            // Xử lý công việc thu hoạch
            if (task.getTaskType() == ETaskType.HARVEST && task.getTargetSlot() != null) {
                boolean isEarlyProposal = Boolean.TRUE.equals(task.getIsEarlyHarvest())
                        || (task.getTaskName() != null && task.getTaskName().contains("Đề xuất thu hoạch sớm"));

                if (isEarlyProposal) {
                    // TH1: Quản lý phê duyệt Đề xuất thu hoạch sớm -> Kích hoạt thông báo cho khách hàng chọn cách thu hoạch
                    notifyCustomerHarvestChoiceAfterApproval(task);
                } else if (task.getEvidenceImageUrl() != null && !task.getEvidenceImageUrl().trim().isEmpty()) {
                    // TH2: Nhân viên đã hoàn tất thu hoạch và nộp ảnh bằng chứng -> Báo kết quả thu hoạch hoàn tất cho khách
                    notifyCustomerHarvestDone(task);
                }
            }

            // Tự động đóng cảnh báo IoT nếu đây là task xử lý cảnh báo
            if (task.getTargetSlot() != null && task.getTaskType() == ETaskType.MAINTENANCE
                    && task.getTaskName() != null && (task.getTaskName().contains("cảnh báo") || task.getTaskName().contains("Cảnh báo"))
                    && alertRepository != null) {
                List<Alert> pendingAlerts = alertRepository.findByGardenSlotId(task.getTargetSlot().getId());
                for (Alert al : pendingAlerts) {
                    if (al.getStatus() == EAlertStatus.PENDING || al.getStatus() == EAlertStatus.IN_PROGRESS) {
                        al.setStatus(EAlertStatus.RESOLVED);
                        al.setResolvedAt(LocalDateTime.now());
                        alertRepository.save(al);

                        if (alertProcessingLogRepository != null) {
                            AlertProcessingLog pLog = new AlertProcessingLog();
                            pLog.setAlert(al);
                            pLog.setProcessedBy(task.getAssignedStaff());
                            pLog.setStatus(EAlertProcessingStatus.PROCESSED);
                            pLog.setComment("Đã xử lý thông qua hoàn tất nhiệm vụ: " + task.getTaskName());
                            pLog.setEvidenceImageUrl(task.getEvidenceImageUrl());
                            pLog.setProcessedAt(LocalDateTime.now());
                            alertProcessingLogRepository.save(pLog);
                        }
                    }
                }
            }
        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Rejection reason is required when rejecting a task evidence");
            }
            task.setStatus(ETaskStatus.REJECTED);
            task.setRejectionReason(request.getRejectionReason());
            
            // Tự động hoàn trả thiết bị IoT về kho khi bị từ chối duyệt
            rollbackEquipmentBindingsOnReject(task);

            // Notify staff
            if (task.getAssignedStaff() != null && notificationService != null) {
                notificationService.createNotification(
                        task.getAssignedStaff().getId(),
                        "Nhiệm vụ bị từ chối duyệt",
                        String.format("Nhiệm vụ '%s' tại ô %s bị từ chối duyệt. Lý do: %s", task.getTaskName(), slotNumber, request.getRejectionReason()),
                        "TASK_REJECTED",
                        task.getId(),
                        "/dashboard/garden-staff/schedules"
                );
            }
        } else {
            throw new IllegalArgumentException("Invalid review action. Must be APPROVE or REJECT");
        }

        return gardeningTaskRepository.save(task);
    }

    private void rollbackEquipmentBindingsOnReject(GardeningTask task) {
        if (task == null) return;
        String tName = task.getTaskName() != null ? task.getTaskName().toLowerCase() : "";
        String tDesc = task.getDescription() != null ? task.getDescription().toLowerCase() : "";
        boolean isPillarSetupTask = (task.getPillarCodes() != null && !task.getPillarCodes().isBlank()) && (
                tName.contains("lắp") || tName.contains("lap") ||
                tName.contains("bổ sung") || tName.contains("bo sung") ||
                tName.contains("thiết bị") || tName.contains("thiet bi") ||
                tName.contains("iot") || tName.contains("cảm biến") ||
                tName.contains("gắn") || tName.contains("gán") ||
                tName.contains("install") || tName.contains("setup") ||
                tDesc.contains("lắp") || tDesc.contains("thiết bị") || tDesc.contains("iot") ||
                tDesc.contains("install") || tDesc.contains("setup")
        );
        if (!isPillarSetupTask) {
            return;
        }

        String[] pCodes = task.getPillarCodes().split(",");
        for (String codeRaw : pCodes) {
            String pCode = codeRaw.trim();
            if (pCode.isEmpty()) continue;
            Pillar pillar = pillarRepository.findByPillarCode(pCode).orElse(null);
            if (pillar == null) continue;

            List<Equipment> attachedEquipments = equipmentRepository.findByPillar(pillar);
            if (attachedEquipments == null || attachedEquipments.isEmpty()) continue;

            for (Equipment eq : attachedEquipments) {
                String eqName = eq.getEquipmentName();
                Long locId = eq.getLocation() != null ? eq.getLocation().getId() : null;
                int returnQty = eq.getQuantity() != null ? eq.getQuantity() : 1;

                List<Equipment> warehouseItems = equipmentRepository.findByPillarIsNull();
                Equipment targetWarehouseItem = null;
                for (Equipment whItem : warehouseItems) {
                    if (whItem.getStatus() == EEquipmentStatus.AVAILABLE
                            && whItem.getEquipmentName() != null
                            && whItem.getEquipmentName().trim().equalsIgnoreCase(eqName != null ? eqName.trim() : "")) {
                        Long whLocId = whItem.getLocation() != null ? whItem.getLocation().getId() : null;
                        if ((locId == null && whLocId == null) || (locId != null && locId.equals(whLocId))) {
                            targetWarehouseItem = whItem;
                            break;
                        }
                    }
                }

                if (targetWarehouseItem != null && !targetWarehouseItem.getId().equals(eq.getId())) {
                    // Trả số lượng về lô hàng tồn trong kho và xoá bản ghi đã gắn vào trụ
                    int currentWhQty = targetWarehouseItem.getQuantity() != null ? targetWarehouseItem.getQuantity() : 1;
                    targetWarehouseItem.setQuantity(currentWhQty + returnQty);
                    equipmentRepository.save(targetWarehouseItem);
                    equipmentRepository.delete(eq);
                } else {
                    // Trả lại thiết bị về kho dạng AVAILABLE, ngắt liên kết với trụ
                    eq.setPillar(null);
                    eq.setStatus(EEquipmentStatus.AVAILABLE);
                    equipmentRepository.save(eq);
                }
            }
        }
    }

    private void notifyCustomerHarvestChoiceAfterApproval(GardeningTask task) {
        List<SlotRental> activeRentals = slotRentalRepository.findActiveRentals(task.getTargetSlot().getId(), LocalDateTime.now());
        if (activeRentals.isEmpty()) {
            return;
        }
        SlotRental rental = activeRentals.stream()
                .filter(r -> task.getRequestedBy() != null && r.getUser() != null && r.getUser().getId().equals(task.getRequestedBy().getId()))
                .findFirst()
                .orElse(activeRentals.get(0));
        if (rental.getUser() == null) {
            return;
        }

        // Tự động duyệt hàng loạt tất cả các task đề xuất thu hoạch sớm còn lại của cùng ô vườn này
        Set<String> allEarlyPillarCodes = new java.util.LinkedHashSet<>();
        if (task.getPillarCodes() != null && !task.getPillarCodes().isBlank()) {
            Arrays.stream(task.getPillarCodes().split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(allEarlyPillarCodes::add);
        }

        if (task.getTargetSlot() != null && task.getTargetSlot().getId() != null) {
            List<GardeningTask> siblingHarvestTasks = gardeningTaskRepository.findByTargetSlotIdAndTaskTypeOrderByCreatedAtDesc(
                    task.getTargetSlot().getId(), ETaskType.HARVEST
            );
            for (GardeningTask sibling : siblingHarvestTasks) {
                if (!sibling.getId().equals(task.getId()) && sibling.getStatus() == ETaskStatus.PENDING_APPROVAL) {
                    boolean isSiblingEarly = Boolean.TRUE.equals(sibling.getIsEarlyHarvest())
                            || (sibling.getTaskName() != null && sibling.getTaskName().contains("thu hoạch sớm"));
                    if (isSiblingEarly) {
                        sibling.setStatus(ETaskStatus.COMPLETED);
                        sibling.setRejectionReason(null);
                        gardeningTaskRepository.save(sibling);
                        if (sibling.getPillarCodes() != null && !sibling.getPillarCodes().isBlank()) {
                            Arrays.stream(sibling.getPillarCodes().split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(allEarlyPillarCodes::add);
                        }
                    }
                }
            }
        }

        String finalPillarCodes = allEarlyPillarCodes.isEmpty() ? task.getPillarCodes() : String.join(", ", allEarlyPillarCodes);

        rental.setHarvestNotifiedAt(LocalDateTime.now());
        rental.setHarvestDecision(null);
        rental.setHarvestPillarCode(finalPillarCodes);
        rental.setHarvestEvidenceImageUrl(task.getEvidenceImageUrl());
        rental.setHarvestStaffNotes(task.getStaffNotes());
        slotRentalRepository.save(rental);

        String staffName = task.getAssignedStaff() != null ? task.getAssignedStaff().getFullName() : "Nhân viên làm vườn";
        String slotNumber = task.getTargetSlot().getSlotNumber();
        String treeName = task.getTreeName() != null && !task.getTreeName().isBlank() ? task.getTreeName() : (rental.getTree() != null ? rental.getTree().getTreeName() : "cây trồng");
        String pillarText = finalPillarCodes != null && !finalPillarCodes.isBlank() ? (finalPillarCodes.startsWith("Trụ") ? finalPillarCodes : ("Trụ " + finalPillarCodes)) : "Toàn bộ trụ";

        String message = String.format(
                "Quản lý đã phê duyệt đề xuất thu hoạch sớm: Cây %s tại ô đất %s (%s) đã sẵn sàng thu hoạch. Bạn muốn tự thu hoạch hay nhờ nhân viên thu hoạch giúp?",
                treeName, slotNumber, pillarText);

        if (notificationService != null) {
            notificationService.createNotification(
                    rental.getUser().getId(),
                    "Sẵn sàng thu hoạch sớm: Ô " + slotNumber,
                    message,
                    "HARVEST_CHOICE",
                    rental.getId(),
                    "/dashboard/customer/rentals",
                    task.getEvidenceImageUrl()
            );
        }

        if (firebaseMessagingService != null) {
            firebaseMessagingService.sendPushNotification(
                    rental.getUser().getId(),
                    "Sẵn sàng thu hoạch sớm: Ô " + slotNumber,
                    String.format("Cây %s tại ô %s (%s) đã sẵn sàng thu hoạch", treeName, slotNumber, pillarText)
            );
        }
    }

    private void notifyCustomerHarvestDone(GardeningTask task) {
        List<SlotRental> activeRentals = slotRentalRepository.findActiveRentals(task.getTargetSlot().getId(), LocalDateTime.now());
        if (activeRentals.isEmpty()) {
            return;
        }
        SlotRental rental = activeRentals.stream()
                .filter(r -> task.getRequestedBy() != null && r.getUser() != null && r.getUser().getId().equals(task.getRequestedBy().getId()))
                .findFirst()
                .orElse(activeRentals.get(0));
        if (rental.getUser() == null) {
            return;
        }

        String staffName = task.getAssignedStaff() != null ? task.getAssignedStaff().getFullName() : "Nhân viên làm vườn";
        String slotNumber = task.getTargetSlot().getSlotNumber();
        String treeName = rental.getTree() != null ? rental.getTree().getTreeName() : "cây trồng";

        String message = String.format("Nhân viên %s đã thu hoạch xong cây %s tại ô đất %s.", staffName, treeName, slotNumber);

        if (notificationService != null) {
            notificationService.createNotification(
                    rental.getUser().getId(),
                    "Đã thu hoạch xong",
                    message,
                    "HARVEST_DONE",
                    rental.getId(),
                    "/dashboard/customer/harvest-history",
                    task.getEvidenceImageUrl()
            );
        }

        if (firebaseMessagingService != null) {
            firebaseMessagingService.sendPushNotification(
                    rental.getUser().getId(),
                    "Đã thu hoạch xong",
                    String.format("%s đã thu hoạch cây %s tại ô %s", staffName, treeName, slotNumber)
            );
        }

        // Lưu lại lịch sử thu hoạch TRƯỚC khi xóa dữ liệu cây khỏi rental
        harvestHistoryService.recordHarvest(rental, "STAFF", task.getAssignedStaff(), task.getPillarCodes(), task.getEvidenceImageUrl(), task.getStaffNotes());

        // Dọn dẹp defaultTree trên từng Pillar vừa thu hoạch
        List<Pillar> rentedPillars = rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty()
                ? rental.getRentedPillars()
                : (rental.getGardenSlot() != null && rental.getGardenSlot().getPillars() != null
                    ? rental.getGardenSlot().getPillars()
                    : (rental.getGardenSlot() != null && rental.getGardenSlot().getPillar() != null ? List.of(rental.getGardenSlot().getPillar()) : List.of()));

        Set<String> newlyHarvestedCodes = (task.getPillarCodes() != null && !task.getPillarCodes().isBlank())
                ? Arrays.stream(task.getPillarCodes().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet())
                : Collections.emptySet();

        for (Pillar p : rentedPillars) {
            String code = p.getPillarCode() != null ? p.getPillarCode() : ("Trụ " + p.getId());
            if (newlyHarvestedCodes.isEmpty() || newlyHarvestedCodes.contains(code)) {
                p.setDefaultTree(null);
                pillarRepository.save(p);
            }
        }

        // Kiểm tra xem sau đợt này còn trụ nào chưa thu hoạch không
        LocalDateTime plantTime = rental.getPlantedAt() != null ? rental.getPlantedAt() : rental.getStartTime();
        List<HarvestHistory> pastHistories = harvestHistoryRepository.findByRentalId(rental.getId());
        Set<String> allHarvestedPillars = new HashSet<>();
        if (plantTime != null && pastHistories != null) {
            for (HarvestHistory h : pastHistories) {
                if (h.getHarvestedAt() != null && (h.getHarvestedAt().isAfter(plantTime.minusMinutes(2)) || h.getHarvestedAt().isEqual(plantTime))) {
                    if (h.getPillarCodes() != null) {
                        for (String c : h.getPillarCodes().split(",")) {
                            if (!c.trim().isEmpty()) {
                                allHarvestedPillars.add(c.trim());
                            }
                        }
                    }
                }
            }
        }

        Set<String> allPillarsInRentalNorm = rentedPillars.stream()
                .map(p -> normalizePillarCode(p.getPillarCode() != null ? p.getPillarCode() : ("Trụ " + p.getId())))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> allHarvestedNorm = allHarvestedPillars.stream()
                .map(this::normalizePillarCode)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        boolean allHarvested = allPillarsInRentalNorm.isEmpty() || allHarvestedNorm.containsAll(allPillarsInRentalNorm);
        if (allHarvested) {
            // Toàn bộ các trụ đã được thu hoạch xong -> Giải phóng ô đất hoàn toàn để gieo lứa mới
            rental.setTree(null);
            rental.setTreeStatus(null);
            rental.setTreeNotes(null);
            rental.setPlantedAt(null);
            rental.setHarvestReminderSent(false);
            rental.setHarvestNotifiedAt(null);
            rental.setHarvestDecision(null);
            rental.setHarvestPillarCode(null);
            rental.setHarvestEvidenceImageUrl(null);
            rental.setHarvestStaffNotes(null);
        } else {
            // Vẫn còn trụ khác đang có cây -> Giữ lại cây trên rental, chỉ giải tỏa trạng thái quyết định thu hoạch tạm thời
            rental.setHarvestDecision(null);
            rental.setHarvestPillarCode(null);
            rental.setHarvestNotifiedAt(null);
            rental.setHarvestEvidenceImageUrl(null);
            rental.setHarvestStaffNotes(null);
        }
        slotRentalRepository.save(rental);
    }

    @Override
    @Transactional
    public GardeningTask updateEvidenceImage(Long taskId, String evidenceImageUrl) {
        GardeningTask task = gardeningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + taskId));
        task.setEvidenceImageUrl(evidenceImageUrl);
        return gardeningTaskRepository.save(task);
    }

    private boolean hasActiveHarvestTask(SlotRental rental) {
        if (rental.getGardenSlot() == null) {
            return false;
        }
        List<GardeningTask> tasks = gardeningTaskRepository.findByTargetSlotIdAndTaskTypeOrderByCreatedAtDesc(
                rental.getGardenSlot().getId(), ETaskType.HARVEST);
        LocalDateTime cutoff = rental.getPlantedAt() != null ? rental.getPlantedAt() : rental.getStartTime();
        return tasks.stream()
                .filter(t -> t.getStatus() != ETaskStatus.COMPLETED && t.getStatus() != ETaskStatus.CANCELLED)
                .filter(t -> cutoff == null || t.getCreatedAt() == null || !t.getCreatedAt().isBefore(cutoff.minusMinutes(5)))
                .findAny()
                .isPresent();
    }

    private boolean hasActiveHarvestTaskForPillar(SlotRental rental, String pillarCode, LocalDateTime plantedDate) {
        if (rental.getGardenSlot() == null || pillarCode == null || pillarCode.isBlank()) {
            return false;
        }
        List<GardeningTask> tasks = gardeningTaskRepository.findByTargetSlotIdAndTaskTypeOrderByCreatedAtDesc(
                rental.getGardenSlot().getId(), ETaskType.HARVEST);
        LocalDateTime cutoff = plantedDate != null ? plantedDate : (rental.getPlantedAt() != null ? rental.getPlantedAt() : rental.getStartTime());
        return tasks.stream()
                .filter(t -> t.getStatus() != ETaskStatus.COMPLETED && t.getStatus() != ETaskStatus.CANCELLED)
                .filter(t -> cutoff == null || t.getCreatedAt() == null || !t.getCreatedAt().isBefore(cutoff.minusMinutes(5)))
                .anyMatch(t -> {
                    if (t.getPillarCodes() == null || t.getPillarCodes().isBlank()) {
                        return false;
                    }
                    return t.getPillarCodes().equals(pillarCode) || t.getPillarCodes().contains(pillarCode);
                });
    }

    @Override
    public List<EligibleHarvestRentalDTO> getEligibleEarlyHarvestRentals(String username) {
        User staff = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        if (staff.getLocation() == null) {
            return List.of();
        }

        List<SlotRental> rentals = slotRentalRepository.findActiveRentalsByLocationId(staff.getLocation().getId());
        List<EligibleHarvestRentalDTO> result = new java.util.ArrayList<>();

        for (SlotRental r : rentals) {
            if (r.getGardenSlot() == null) {
                continue;
            }

            String slotNumber = r.getGardenSlot() != null ? r.getGardenSlot().getSlotNumber() : "N/A";

            List<TreePlantingRequest> requests = treePlantingRequestRepository.findByRental(r).stream()
                    .filter(req -> req.getStatus() == EPlantingRequestStatus.APPROVED && req.getNewTree() != null)
                    .toList();

            // Tập hợp toàn diện tất cả các trụ của ô thuê (từ rentedPillars, gardenSlot.pillars, và approved requests)
            java.util.Map<Long, Pillar> pillarMap = new java.util.LinkedHashMap<>();

            if (r.getRentedPillars() != null) {
                for (Pillar p : r.getRentedPillars()) {
                    if (p != null && p.getId() != null) {
                        pillarMap.put(p.getId(), p);
                    }
                }
            }
            if (r.getGardenSlot() != null && r.getGardenSlot().getPillars() != null) {
                for (Pillar p : r.getGardenSlot().getPillars()) {
                    if (p != null && p.getId() != null) {
                        pillarMap.putIfAbsent(p.getId(), p);
                    }
                }
            }
            if (r.getGardenSlot() != null && r.getGardenSlot().getPillar() != null) {
                Pillar p = r.getGardenSlot().getPillar();
                if (p.getId() != null) {
                    pillarMap.putIfAbsent(p.getId(), p);
                }
            }
            for (TreePlantingRequest req : requests) {
                if (req.getTargetPillar() != null && req.getTargetPillar().getId() != null) {
                    pillarMap.putIfAbsent(req.getTargetPillar().getId(), req.getTargetPillar());
                }
            }

            if (!pillarMap.isEmpty()) {
                List<EligibleHarvestRentalDTO> slotPillarItems = new java.util.ArrayList<>();
                for (Pillar p : pillarMap.values()) {
                    final Long targetPillarId = p.getId();
                    String pCode = p.getPillarCode() != null ? p.getPillarCode() : ("Trụ " + p.getId());

                    // Tìm giống cây trên trụ này (Ưu tiên: approved request mới nhất -> p.defaultTree -> r.tree)
                    TreePlantingRequest latestReq = requests.stream()
                            .filter(req -> req.getTargetPillar() != null && req.getTargetPillar().getId().equals(targetPillarId))
                            .reduce((first, second) -> second)
                            .orElse(null);

                    Tree tree = latestReq != null ? latestReq.getNewTree() : (p.getDefaultTree() != null ? p.getDefaultTree() : r.getTree());
                    if (tree == null) {
                        continue; // Trụ này chưa có cây
                    }

                    LocalDateTime plantedDate = latestReq != null && latestReq.getProcessedAt() != null
                            ? latestReq.getProcessedAt()
                            : (r.getPlantedAt() != null ? r.getPlantedAt() : r.getStartTime());

                    // Check if this pillar already has an active harvest task for the current planting cycle
                    if (hasActiveHarvestTaskForPillar(r, pCode, plantedDate)) {
                        continue; // Trụ này đang được thu hoạch hoặc đã gửi đề xuất -> ẩn khỏi dropdown
                    }
                    if (isPillarHarvestedSinceLastPlanting(r, p)) {
                        continue; // Trụ đã thu hoạch sau lần trồng gần nhất -> đang trống, không thể đề xuất
                    }

                    Integer harvestDays = tree.getHarvestDays();
                    Integer daysGrown = plantedDate != null
                            ? (int) java.time.temporal.ChronoUnit.DAYS.between(plantedDate.toLocalDate(), java.time.LocalDate.now())
                            : 0;

                    slotPillarItems.add(new EligibleHarvestRentalDTO(
                            r.getId(),
                            p.getId(),
                            pCode,
                            slotNumber,
                            tree.getTreeName(),
                            plantedDate,
                            pCode,
                            harvestDays,
                            Math.max(0, daysGrown)
                    ));
                }

                if (slotPillarItems.size() > 1) {
                    String allCodes = slotPillarItems.stream().map(EligibleHarvestRentalDTO::getPillarCode).collect(Collectors.joining(", "));
                    String firstTreeName = slotPillarItems.get(0).getTreeName();
                    result.add(new EligibleHarvestRentalDTO(
                            r.getId(),
                            null,
                            "ALL",
                            slotNumber,
                            firstTreeName,
                            slotPillarItems.get(0).getPlantedAt(),
                            "Tất cả " + slotPillarItems.size() + " trụ (" + allCodes + ")",
                            slotPillarItems.get(0).getHarvestDays(),
                            slotPillarItems.get(0).getDaysGrown()
                    ));
                }
                result.addAll(slotPillarItems);
            } else if (r.getTree() != null) {
                LocalDateTime plantedDate = r.getPlantedAt() != null ? r.getPlantedAt() : r.getStartTime();
                if (!hasActiveHarvestTaskForPillar(r, null, plantedDate)) {
                    Integer harvestDays = r.getTree().getHarvestDays();
                    Integer daysGrown = plantedDate != null
                            ? (int) java.time.temporal.ChronoUnit.DAYS.between(plantedDate.toLocalDate(), java.time.LocalDate.now())
                            : 0;
                    result.add(new EligibleHarvestRentalDTO(
                            r.getId(),
                            null,
                            null,
                            slotNumber,
                            r.getTree().getTreeName(),
                            plantedDate,
                            null,
                            harvestDays,
                            Math.max(0, daysGrown)
                    ));
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public GardeningTask notifyEarlyHarvest(Long rentalId, String username) {
        return notifyEarlyHarvest(rentalId, null, null, null, null, username);
    }

    @Override
    @Transactional
    public GardeningTask notifyEarlyHarvest(Long rentalId, Long pillarId, String pillarCode, String username) {
        return notifyEarlyHarvest(rentalId, pillarId, pillarCode, null, null, username);
    }

    @Override
    @Transactional
    public GardeningTask notifyEarlyHarvest(Long rentalId, Long pillarId, String pillarCode, String evidenceImageUrl, String staffNotes, String username) {
        User staff = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        SlotRental rental = slotRentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found with id: " + rentalId));

        if (rental.getStatus() != ERentalStatus.ACTIVE) {
            throw new IllegalArgumentException("This rental is not active");
        }
        if (rental.getGardenSlot() == null) {
            throw new IllegalArgumentException("Rental has no target slot");
        }

        Long rentalLocationId = getSlotLocationId(rental.getGardenSlot());
        if (staff.getLocation() == null || !staff.getLocation().getId().equals(rentalLocationId)) {
            throw new IllegalArgumentException("Bạn chỉ có thể gửi đề xuất thu hoạch sớm cho các ô vườn tại cơ sở làm việc của mình");
        }

        String staffName = staff.getFullName() != null ? staff.getFullName().trim() : "Nhân viên vườn";
        if (staffName.startsWith("Nhân viên ")) {
            staffName = staffName.substring(10).trim();
        }
        String slotNumber = rental.getGardenSlot().getSlotNumber();
        LocalDateTime plantedDateCutoff = rental.getPlantedAt() != null ? rental.getPlantedAt() : rental.getStartTime();

        boolean isAllPillars = "ALL".equalsIgnoreCase(pillarCode) 
                || (pillarId == null && (pillarCode == null || pillarCode.isBlank() || pillarCode.startsWith("Tất cả")));

        if (isAllPillars) {
            // TẬP HỢP TẤT CẢ CÁC TRỤ CỦA Ô VƯỜN ĐỂ TÁCH THÀNH TỪNG TASK RIÊNG BIỆT CHO MỖI TRỤ
            java.util.Map<Long, Pillar> pillarMap = new java.util.LinkedHashMap<>();
            if (rental.getRentedPillars() != null) {
                for (Pillar p : rental.getRentedPillars()) {
                    if (p != null && p.getId() != null) pillarMap.put(p.getId(), p);
                }
            }
            if (rental.getGardenSlot() != null && rental.getGardenSlot().getPillars() != null) {
                for (Pillar p : rental.getGardenSlot().getPillars()) {
                    if (p != null && p.getId() != null) pillarMap.putIfAbsent(p.getId(), p);
                }
            }

            List<GardeningTask> createdTasks = new java.util.ArrayList<>();
            List<TreePlantingRequest> requests = treePlantingRequestRepository.findByRental(rental).stream()
                    .filter(req -> req.getStatus() == EPlantingRequestStatus.APPROVED && req.getNewTree() != null)
                    .toList();

            for (Pillar p : pillarMap.values()) {
                final Long tPillarId = p.getId();
                String pCode = p.getPillarCode() != null ? p.getPillarCode() : ("Trụ " + p.getId());
                if (hasActiveHarvestTaskForPillar(rental, pCode, plantedDateCutoff)) {
                    continue; // Trụ này đã có đề xuất thu hoạch đang xử lý
                }

                Tree targetTree = requests.stream()
                        .filter(req -> req.getTargetPillar() != null && req.getTargetPillar().getId().equals(tPillarId))
                        .reduce((first, second) -> second)
                        .map(TreePlantingRequest::getNewTree)
                        .orElse(p.getDefaultTree() != null ? p.getDefaultTree() : rental.getTree());

                if (targetTree == null || isPillarHarvestedSinceLastPlanting(rental, p)) {
                    continue;
                }

                String treeName = targetTree.getTreeName();
                GardeningTask task = new GardeningTask();
                task.setTaskName("Đề xuất thu hoạch sớm: " + treeName + " - Ô " + slotNumber + " (Trụ " + pCode + ")");
                task.setDescription("Nhân viên " + staff.getFullName() + " đề xuất thu hoạch sớm cho cây " + treeName
                        + " tại ô " + slotNumber + " (Trụ " + pCode + ") (chưa đủ số ngày sinh trưởng dự kiến). Chờ Quản lý phê duyệt để gửi thông báo cho khách hàng.");
                task.setStatus(ETaskStatus.PENDING_APPROVAL);
                task.setTaskType(ETaskType.HARVEST);
                task.setTargetSlot(rental.getGardenSlot());
                task.setRequestedBy(rental.getUser());
                task.setAssignedStaff(staff);
                task.setPillarCodes(pCode);
                task.setTreeName(treeName);
                task.setIsEarlyHarvest(true);
                task.setEvidenceImageUrl(evidenceImageUrl != null && !evidenceImageUrl.isBlank() ? evidenceImageUrl.trim() : null);
                task.setStaffNotes(staffNotes != null && !staffNotes.isBlank() ? staffNotes.trim() : null);
                task.setCreatedAt(LocalDateTime.now());
                createdTasks.add(gardeningTaskRepository.save(task));
            }

            if (createdTasks.isEmpty()) {
                throw new IllegalArgumentException("Không có trụ nào tại ô này có thể đề xuất thu hoạch sớm (đã có đề xuất đang xử lý, hoặc trụ đã thu hoạch và chưa trồng cây mới).");
            }

            // Gửi thông báo cho Location Managers
            if (notificationService != null) {
                List<User> managers = findLocationManagers(rental.getGardenSlot());
                String title = "Đề xuất thu hoạch sớm (" + createdTasks.size() + " trụ) chờ duyệt: Ô " + slotNumber;
                String message = String.format("Nhân viên %s đề xuất thu hoạch sớm cho tất cả các trụ (%d trụ) tại ô %s. Vui lòng kiểm tra và duyệt.",
                        staffName, createdTasks.size(), slotNumber);
                for (User manager : managers) {
                    notificationService.createNotification(
                            manager.getId(),
                            title,
                            message,
                            "TASK_SUBMITTED",
                            createdTasks.get(0).getId(),
                            "/dashboard/staff/tasks"
                    );
                }
            }
            return createdTasks.get(0);
        }

        // TRƯỜNG HỢP BÁO THU HOẠCH SỚM CHO 1 TRỤ CỤ THỂ
        Pillar targetPillar = null;
        if (pillarId != null) {
            targetPillar = pillarRepository.findById(pillarId).orElse(null);
        } else if (pillarCode != null && !pillarCode.isBlank()) {
            targetPillar = pillarRepository.findByPillarCode(pillarCode).orElse(null);
        }

        String effectivePillarCode = targetPillar != null ? targetPillar.getPillarCode() : pillarCode;

        if (hasActiveHarvestTaskForPillar(rental, effectivePillarCode, plantedDateCutoff)) {
            throw new IllegalArgumentException("Trụ " + (effectivePillarCode != null ? effectivePillarCode : "") + " đã có đề xuất thu hoạch sớm đang chờ duyệt hoặc đang được xử lý.");
        }
        if (targetPillar != null && isPillarHarvestedSinceLastPlanting(rental, targetPillar)) {
            throw new IllegalArgumentException("Trụ " + effectivePillarCode + " đã được thu hoạch và chưa trồng cây mới, không thể đề xuất thu hoạch sớm.");
        }

        Tree targetTree = null;
        if (targetPillar != null && targetPillar.getDefaultTree() != null) {
            targetTree = targetPillar.getDefaultTree();
        }
        if (targetTree == null && targetPillar != null) {
            final Long targetPillarId = targetPillar.getId();
            targetTree = treePlantingRequestRepository.findByRental(rental).stream()
                    .filter(req -> req.getStatus() == EPlantingRequestStatus.APPROVED 
                            && req.getTargetPillar() != null 
                            && req.getTargetPillar().getId().equals(targetPillarId)
                            && req.getNewTree() != null)
                    .map(TreePlantingRequest::getNewTree)
                    .findFirst()
                    .orElse(null);
        }
        if (targetTree == null) {
            targetTree = rental.getTree();
        }
        if (targetTree == null) {
            throw new IllegalArgumentException("No tree planted on this pillar / rental to harvest");
        }

        String treeName = targetTree.getTreeName();
        String pillarText = effectivePillarCode != null ? ("Trụ " + effectivePillarCode) : "Toàn bộ trụ";

        // Tạo nhiệm vụ dạng ĐỀ XUẤT THU HOẠCH SỚM chờ Location Manager phê duyệt
        GardeningTask task = new GardeningTask();
        task.setTaskName("Đề xuất thu hoạch sớm: " + treeName + " - Ô " + slotNumber + " (" + pillarText + ")");
        task.setDescription("Nhân viên " + staff.getFullName() + " đề xuất thu hoạch sớm cho cây " + treeName
                + " tại ô " + slotNumber + " (" + pillarText + ") (chưa đủ số ngày sinh trưởng dự kiến). Chờ Quản lý phê duyệt để gửi thông báo cho khách hàng.");
        task.setStatus(ETaskStatus.PENDING_APPROVAL);
        task.setTaskType(ETaskType.HARVEST);
        task.setTargetSlot(rental.getGardenSlot());
        task.setRequestedBy(rental.getUser());
        task.setAssignedStaff(staff);
        task.setPillarCodes(effectivePillarCode);
        task.setTreeName(treeName);
        task.setIsEarlyHarvest(true);
        task.setEvidenceImageUrl(evidenceImageUrl != null && !evidenceImageUrl.isBlank() ? evidenceImageUrl.trim() : null);
        task.setStaffNotes(staffNotes != null && !staffNotes.isBlank() ? staffNotes.trim() : null);
        task.setCreatedAt(LocalDateTime.now());
        GardeningTask savedTask = gardeningTaskRepository.save(task);

        // Gửi thông báo cho Location Managers để kiểm tra và duyệt đề xuất
        if (notificationService != null) {
            List<User> managers = findLocationManagers(rental.getGardenSlot());
            String title = "Đề xuất thu hoạch sớm chờ duyệt: Ô " + slotNumber;
            String message = String.format("Nhân viên %s đề xuất thu hoạch sớm cho cây %s tại ô %s (%s). Vui lòng kiểm tra và duyệt.",
                    staffName, treeName, slotNumber, pillarText);
            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        title,
                        message,
                        "TASK_SUBMITTED",
                        savedTask.getId(),
                        "/dashboard/staff/tasks"
                );
            }
        }

        return savedTask;
    }

    private String normalizePillarCode(String code) {
        if (code == null) return "";
        return code.trim().replaceAll("^(?i)trụ\\s*", "").trim();
    }

    // Trụ đã được thu hoạch sau lần trồng gần nhất -> hiện đang trống, không còn cây để thu hoạch
    private boolean isPillarHarvestedSinceLastPlanting(SlotRental rental, Pillar pillar) {
        if (pillar == null || pillar.getPillarCode() == null) {
            return false;
        }
        String pillarNorm = normalizePillarCode(pillar.getPillarCode());
        LocalDateTime lastPlanted = rental.getPlantedAt() != null ? rental.getPlantedAt() : rental.getStartTime();
        for (TreePlantingRequest req : treePlantingRequestRepository.findByRental(rental)) {
            boolean planted = req.getStatus() == EPlantingRequestStatus.APPROVED || req.getStatus() == EPlantingRequestStatus.COMPLETED;
            boolean samePillar = req.getTargetPillar() == null
                    || normalizePillarCode(req.getTargetPillar().getPillarCode()).equals(pillarNorm);
            if (planted && samePillar && req.getProcessedAt() != null
                    && (lastPlanted == null || req.getProcessedAt().isAfter(lastPlanted))) {
                lastPlanted = req.getProcessedAt();
            }
        }
        if (lastPlanted == null) {
            return false;
        }
        LocalDateTime cutoff = lastPlanted.minusMinutes(2);
        return harvestHistoryRepository.findByRentalId(rental.getId()).stream()
                .filter(h -> h.getHarvestedAt() != null && h.getPillarCodes() != null && !h.getHarvestedAt().isBefore(cutoff))
                .flatMap(h -> java.util.Arrays.stream(h.getPillarCodes().split(",")))
                .map(this::normalizePillarCode)
                .anyMatch(pillarNorm::equals);
    }
}
