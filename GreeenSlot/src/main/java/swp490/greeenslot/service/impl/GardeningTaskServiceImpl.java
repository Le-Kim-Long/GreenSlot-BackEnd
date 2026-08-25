package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.GardeningTaskService;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        List<User> managers = locationId != null
                ? userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, locationId)
                : List.of();
        if (managers.isEmpty()) {
            managers = userRepository.findByRoleName(ERole.ROLE_MANAGER);
        }
        return managers;
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
            throw new IllegalArgumentException("Invalid task type. Must be MAINTENANCE or CLEANING");
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

    @Override
    @Transactional
    public GardeningTask assignStaffToTask(Long taskId, TaskAssignmentDTO request) {
        if (request.getStaffId() == null) {
            throw new IllegalArgumentException("Staff ID is required for task assignment");
        }
        
        // Fetch target staff and check role
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found with ID " + request.getStaffId()));

        boolean hasStaffRole = staff.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_GARDEN_STAFF);

        if (!hasStaffRole) {
            throw new IllegalArgumentException("User with ID " + request.getStaffId() + " does not have ROLE_GARDEN_STAFF");
        }

        // Fetch the task
        GardeningTask task = gardeningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + taskId));

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
        return gardeningTaskRepository.findByAssignedStaffUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    public List<GardeningTask> getAvailableTasks(String username) {
        User staff = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        if (staff.getLocation() == null) {
            return List.of();
        }

        List<GardeningTask> allUnassigned = gardeningTaskRepository.findUnassignedByLocationId(staff.getLocation().getId());
        if (allUnassigned.isEmpty()) {
            return List.of();
        }

        // Check active shifts of this staff for today
        LocalDate today = LocalDate.now();
        List<StaffSchedule> todaySchedules = staffScheduleRepository.findByStaffAndDateRange(staff.getId(), today, today)
                .stream().filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .toList();

        if (todaySchedules.isEmpty()) {
            // Check if there are any schedules configured in this location today
            List<StaffSchedule> locationSchedulesToday = staffScheduleRepository.findByLocationAndDate(staff.getLocation().getId(), today)
                    .stream().filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .toList();
            if (!locationSchedulesToday.isEmpty()) {
                // Location uses shift scheduling and this staff is not scheduled today -> return empty
                return List.of();
            }
            // Fallback for locations without active shift setup
            return allUnassigned;
        }

        // Check if staff has any whole-location shift (slot == null)
        boolean hasLocationWideShift = todaySchedules.stream().anyMatch(s -> s.getGardenSlot() == null);
        if (hasLocationWideShift) {
            return allUnassigned;
        }

        // Staff is assigned to specific slot(s)
        Set<Long> assignedSlotIds = todaySchedules.stream()
                .map(StaffSchedule::getGardenSlot)
                .filter(Objects::nonNull)
                .map(GardenSlot::getId)
                .collect(Collectors.toSet());

        return allUnassigned.stream()
                .filter(t -> t.getTargetSlot() == null || assignedSlotIds.contains(t.getTargetSlot().getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GardeningTask claimTask(Long taskId, String username) {
        GardeningTask task = gardeningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + taskId));

        if (task.getAssignedStaff() != null) {
            throw new IllegalArgumentException("Task has already been claimed by another staff member");
        }

        User staff = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        // Verify task belongs to staff location
        Long taskLocId = getSlotLocationId(task.getTargetSlot());
        if (taskLocId == null && task.getAssignedStaff() != null && task.getAssignedStaff().getLocation() != null) {
            taskLocId = task.getAssignedStaff().getLocation().getId();
        }
        if (staff.getLocation() != null && taskLocId != null && !staff.getLocation().getId().equals(taskLocId)) {
            throw new IllegalArgumentException("You can only claim tasks at your own location");
        }

        // If staff has schedules assigned to specific slots today, ensure task matches assigned slot
        LocalDate today = LocalDate.now();
        List<StaffSchedule> todaySchedules = staffScheduleRepository.findByStaffAndDateRange(staff.getId(), today, today)
                .stream().filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .toList();

        if (!todaySchedules.isEmpty()) {
            boolean hasLocationWideShift = todaySchedules.stream().anyMatch(s -> s.getGardenSlot() == null);
            if (!hasLocationWideShift && task.getTargetSlot() != null) {
                Set<Long> assignedSlotIds = todaySchedules.stream()
                        .map(StaffSchedule::getGardenSlot)
                        .filter(Objects::nonNull)
                        .map(GardenSlot::getId)
                        .collect(Collectors.toSet());
                if (!assignedSlotIds.contains(task.getTargetSlot().getId())) {
                    String assignedNames = todaySchedules.stream()
                            .filter(s -> s.getGardenSlot() != null)
                            .map(s -> "Ô " + s.getGardenSlot().getSlotNumber())
                            .collect(Collectors.joining(", "));
                    throw new IllegalArgumentException("Bạn chỉ có thể nhận công việc tại ô vườn đã được phân công trực (" + assignedNames + ")");
                }
            }
        }

        task.setAssignedStaff(staff);
        return gardeningTaskRepository.save(task);
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
            if (request.getEvidenceImageUrl() == null || request.getEvidenceImageUrl().trim().isEmpty()) {
                throw new IllegalArgumentException("Evidence image URL is required when submitting task for approval");
            }
            task.setEvidenceImageUrl(request.getEvidenceImageUrl());
            
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
        issueTask.setTaskName("ISSUE REPORT: " + request.getIssueTitle());
        issueTask.setDescription("Issue reported by Staff " + username + " on Task #" + taskId + ": " + request.getDescription());
        issueTask.setStatus(ETaskStatus.PENDING);
        issueTask.setTaskType(ETaskType.MAINTENANCE);
        issueTask.setTargetSlot(originalTask.getTargetSlot());
        issueTask.setEvidenceImageUrl(request.getEvidenceImageUrl()); // Can be optional or populated
        issueTask.setAssignedStaff(null); // Left unassigned for manager review
        issueTask.setCreatedAt(LocalDateTime.now());
        
        GardeningTask savedIssue = gardeningTaskRepository.save(issueTask);

        // Update original task
        originalTask.setStatus(ETaskStatus.CANCELLED);
        originalTask.setDescription(originalTask.getDescription() + "\n[BLOCKED_BY_ISSUE: " + savedIssue.getTaskName() + "]");
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

            // If requested by customer, notify customer
            if (task.getRequestedBy() != null && notificationService != null) {
                notificationService.createNotification(
                        task.getRequestedBy().getId(),
                        "Yêu cầu chăm sóc hoàn tất",
                        String.format("Yêu cầu dịch vụ '%s' tại ô đất %s đã được hoàn thành và nghiệm thu.", task.getTaskName(), slotNumber),
                        "TASK_COMPLETED",
                        task.getId(),
                        "/dashboard/customer/rentals"
                );
            }

            // Công việc thu hoạch hoàn tất -> báo cho khách hàng, ghi rõ nhân viên đã xử lý
            if (task.getTaskType() == ETaskType.HARVEST && task.getTargetSlot() != null) {
                notifyCustomerHarvestDone(task);
            }
        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Rejection reason is required when rejecting a task evidence");
            }
            task.setStatus(ETaskStatus.REJECTED);
            task.setRejectionReason(request.getRejectionReason());
            
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

    private void notifyCustomerHarvestDone(GardeningTask task) {
        List<SlotRental> activeRentals = slotRentalRepository.findActiveRentals(task.getTargetSlot().getId(), LocalDateTime.now());
        if (activeRentals.isEmpty()) {
            return;
        }
        SlotRental rental = activeRentals.get(0);
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
                    "/dashboard/customer/harvest-history"
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
        harvestHistoryService.recordHarvest(rental, "STAFF", task.getAssignedStaff());

        // Thu hoạch xong -> ô đất trở lại trạng thái "chưa trồng", sẵn sàng cho yêu cầu trồng cây mới
        rental.setTree(null);
        rental.setTreeStatus(null);
        rental.setTreeNotes(null);
        rental.setPlantedAt(null);
        rental.setHarvestReminderSent(false);
        rental.setHarvestNotifiedAt(null);
        rental.setHarvestDecision(null);
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
                        continue; // Trụ này đang được thu hoạch -> ẩn khỏi dropdown
                    }

                    Integer harvestDays = tree.getHarvestDays();
                    Integer daysGrown = plantedDate != null
                            ? (int) java.time.temporal.ChronoUnit.DAYS.between(plantedDate.toLocalDate(), java.time.LocalDate.now())
                            : 0;

                    result.add(new EligibleHarvestRentalDTO(
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
            } else if (r.getTree() != null) {
                if (!hasActiveHarvestTask(r)) {
                    LocalDateTime plantedDate = r.getPlantedAt() != null ? r.getPlantedAt() : r.getStartTime();
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
        return notifyEarlyHarvest(rentalId, null, null, username);
    }

    @Override
    @Transactional
    public GardeningTask notifyEarlyHarvest(Long rentalId, Long pillarId, String pillarCode, String username) {
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
            throw new IllegalArgumentException("You can only notify harvest for rentals at your own location");
        }

        Pillar targetPillar = null;
        if (pillarId != null) {
            targetPillar = pillarRepository.findById(pillarId).orElse(null);
        } else if (pillarCode != null && !pillarCode.isBlank()) {
            targetPillar = pillarRepository.findByPillarCode(pillarCode).orElse(null);
        }

        String effectivePillarCode = targetPillar != null ? targetPillar.getPillarCode() : pillarCode;
        LocalDateTime plantedDateCutoff = rental.getPlantedAt() != null ? rental.getPlantedAt() : rental.getStartTime();
        if (hasActiveHarvestTaskForPillar(rental, effectivePillarCode, plantedDateCutoff)) {
            throw new IllegalArgumentException("This pillar already has an active harvest task in progress");
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

        String staffName = staff.getFullName() != null ? staff.getFullName().trim() : "Nhân viên vườn";
        if (staffName.startsWith("Nhân viên ")) {
            staffName = staffName.substring(10).trim();
        }
        String slotNumber = rental.getGardenSlot().getSlotNumber();
        String treeName = targetTree.getTreeName();
        String pillarText = effectivePillarCode != null ? ("Trụ " + effectivePillarCode) : "Toàn bộ trụ";

        GardeningTask task = new GardeningTask();
        task.setTaskName("Thu hoạch sớm: " + treeName + " - Ô " + slotNumber + " (" + pillarText + ")");
        task.setDescription("Nhân viên " + staff.getFullName() + " chủ động báo thu hoạch sớm cho cây " + treeName
                + " tại ô " + slotNumber + " (" + pillarText + ") (chưa đủ số ngày sinh trưởng dự kiến).");
        task.setStatus(ETaskStatus.PENDING);
        task.setTaskType(ETaskType.HARVEST);
        task.setTargetSlot(rental.getGardenSlot());
        task.setRequestedBy(rental.getUser());
        task.setAssignedStaff(staff);
        task.setPillarCodes(effectivePillarCode);
        task.setTreeName(treeName);
        task.setIsEarlyHarvest(true);
        task.setCreatedAt(LocalDateTime.now());
        GardeningTask savedTask = gardeningTaskRepository.save(task);

        rental.setHarvestNotifiedAt(LocalDateTime.now());
        rental.setHarvestDecision(null);
        slotRentalRepository.save(rental);

        if (rental.getUser() != null) {
            String message = String.format(
                    "Nhân viên %s báo: cây %s tại ô đất %s (%s) đã sẵn sàng thu hoạch (Thu hoạch sớm). Bạn muốn tự thu hoạch hay nhờ nhân viên thu hoạch giúp?",
                    staffName, treeName, slotNumber, pillarText);

            if (notificationService != null) {
                notificationService.createNotification(
                        rental.getUser().getId(),
                        "Sẵn sàng thu hoạch sớm: Ô " + slotNumber,
                        message,
                        "HARVEST_CHOICE",
                        rental.getId(),
                        "/dashboard/customer/rentals"
                );
            }

            if (firebaseMessagingService != null) {
                firebaseMessagingService.sendPushNotification(
                        rental.getUser().getId(),
                        "Sẵn sàng thu hoạch sớm: Ô " + slotNumber,
                        String.format("%s báo cây %s tại ô %s (%s) đã sẵn sàng thu hoạch (Thu hoạch sớm)", staffName, treeName, slotNumber, pillarText)
                );
            }
        }

        return savedTask;
    }
}
