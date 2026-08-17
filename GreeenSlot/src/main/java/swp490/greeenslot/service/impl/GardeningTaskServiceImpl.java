package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.GardeningTaskService;

import java.time.LocalDateTime;
import java.util.List;
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
    private swp490.greeenslot.service.NotificationService notificationService;

    @Autowired
    private swp490.greeenslot.service.FirebaseMessagingService firebaseMessagingService;

    @Autowired
    private swp490.greeenslot.service.LocationContextService locationContextService;

    private Long getSlotLocationId(GardenSlot slot) {
        if (slot != null && slot.getPillar() != null && slot.getPillar().getLocation() != null) {
            return slot.getPillar().getLocation().getId();
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
                        "/dashboard/manager/tasks"
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
                    "/dashboard/staff/tasks"
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
        return gardeningTaskRepository.findUnassignedByLocationId(staff.getLocation().getId());
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

        String staffName = task.getAssignedStaff().getFullName();
        String slotNumber = task.getTargetSlot().getSlotNumber();
        String treeName = rental.getTree() != null ? rental.getTree().getTreeName() : "cây trồng";

        String message = String.format(
                "Nhân viên %s báo: cây %s tại ô đất %s đã sẵn sàng thu hoạch. Bạn muốn tự thu hoạch hay nhờ nhân viên thu hoạch giúp?",
                staffName, treeName, slotNumber);

        if (notificationService != null) {
            notificationService.createNotification(
                    rental.getUser().getId(),
                    "Sẵn sàng thu hoạch",
                    message,
                    "HARVEST_CHOICE",
                    rental.getId(),
                    "/dashboard/customer/rentals"
            );
        }

        if (firebaseMessagingService != null) {
            firebaseMessagingService.sendPushNotification(
                    rental.getUser().getId(),
                    "Sẵn sàng thu hoạch",
                    String.format("%s báo cây %s tại ô %s đã sẵn sàng thu hoạch", staffName, treeName, slotNumber)
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
            if (t.getTargetSlot() != null && t.getTargetSlot().getPillar() != null && t.getTargetSlot().getPillar().getLocation() != null) {
                return targetLocationId.equals(t.getTargetSlot().getPillar().getLocation().getId());
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
                            "/dashboard/manager/tasks"
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
                        "/dashboard/manager/tasks"
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
                        "/dashboard/staff/tasks"
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
                        "/dashboard/staff/tasks"
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
                    "/dashboard/customer/rentals"
            );
        }

        if (firebaseMessagingService != null) {
            firebaseMessagingService.sendPushNotification(
                    rental.getUser().getId(),
                    "Đã thu hoạch xong",
                    String.format("%s đã thu hoạch cây %s tại ô %s", staffName, treeName, slotNumber)
            );
        }

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
        return tasks.stream().anyMatch(t -> t.getStatus() != ETaskStatus.COMPLETED && t.getStatus() != ETaskStatus.CANCELLED);
    }

    @Override
    public List<EligibleHarvestRentalDTO> getEligibleEarlyHarvestRentals(String username) {
        User staff = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        if (staff.getLocation() == null) {
            return List.of();
        }
        List<SlotRental> rentals = slotRentalRepository.findActiveWithTreeByLocationId(staff.getLocation().getId());
        return rentals.stream()
                .filter(r -> !hasActiveHarvestTask(r))
                .map(r -> new EligibleHarvestRentalDTO(
                        r.getId(),
                        r.getGardenSlot() != null ? r.getGardenSlot().getSlotNumber() : null,
                        r.getTree() != null ? r.getTree().getTreeName() : null,
                        r.getPlantedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GardeningTask notifyEarlyHarvest(Long rentalId, String username) {
        User staff = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        SlotRental rental = slotRentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found with id: " + rentalId));

        if (rental.getStatus() != ERentalStatus.ACTIVE || rental.getTree() == null) {
            throw new IllegalArgumentException("This rental has no active tree to harvest");
        }
        if (rental.getGardenSlot() == null) {
            throw new IllegalArgumentException("Rental has no target slot");
        }

        Long rentalLocationId = getSlotLocationId(rental.getGardenSlot());
        if (staff.getLocation() == null || !staff.getLocation().getId().equals(rentalLocationId)) {
            throw new IllegalArgumentException("You can only notify harvest for rentals at your own location");
        }

        if (hasActiveHarvestTask(rental)) {
            throw new IllegalArgumentException("This rental already has an active harvest task in progress");
        }

        String slotNumber = rental.getGardenSlot().getSlotNumber();
        String treeName = rental.getTree().getTreeName();

        GardeningTask task = new GardeningTask();
        task.setTaskName("Thu hoạch sớm: " + treeName + " - Ô " + slotNumber);
        task.setDescription("Nhân viên " + staff.getFullName() + " chủ động báo thu hoạch sớm cho cây " + treeName
                + " tại ô " + slotNumber + " (chưa đủ số ngày sinh trưởng dự kiến).");
        task.setStatus(ETaskStatus.PENDING);
        task.setTaskType(ETaskType.HARVEST);
        task.setTargetSlot(rental.getGardenSlot());
        task.setRequestedBy(rental.getUser());
        task.setAssignedStaff(staff);
        task.setCreatedAt(LocalDateTime.now());
        GardeningTask savedTask = gardeningTaskRepository.save(task);

        rental.setHarvestNotifiedAt(LocalDateTime.now());
        rental.setHarvestDecision(null);
        slotRentalRepository.save(rental);

        if (rental.getUser() != null) {
            String message = String.format(
                    "Nhân viên %s báo: cây %s tại ô đất %s đã sẵn sàng thu hoạch (báo sớm). Bạn muốn tự thu hoạch hay nhờ nhân viên thu hoạch giúp?",
                    staff.getFullName(), treeName, slotNumber);

            if (notificationService != null) {
                notificationService.createNotification(
                        rental.getUser().getId(),
                        "Sẵn sàng thu hoạch",
                        message,
                        "HARVEST_CHOICE",
                        rental.getId(),
                        "/dashboard/customer/rentals"
                );
            }

            if (firebaseMessagingService != null) {
                firebaseMessagingService.sendPushNotification(
                        rental.getUser().getId(),
                        "Sẵn sàng thu hoạch",
                        String.format("%s báo cây %s tại ô %s đã sẵn sàng thu hoạch", staff.getFullName(), treeName, slotNumber)
                );
            }
        }

        return savedTask;
    }
}
