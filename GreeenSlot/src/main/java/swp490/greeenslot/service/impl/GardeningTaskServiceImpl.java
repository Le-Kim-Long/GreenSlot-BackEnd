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

        return gardeningTaskRepository.save(task);
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

        // Notify staff about task assignment
        notificationService.createNotification(
                staff.getId(),
                "New Task Assigned",
                String.format("You have been assigned to task: %s for slot %s",
                        task.getTaskName(), task.getTargetSlot() != null ? task.getTargetSlot().getSlotNumber() : "N/A"),
                "TASK_ASSIGNMENT"
        );

        firebaseMessagingService.sendPushNotification(
                staff.getId(),
                "New Task Assigned",
                String.format("Task: %s - Slot: %s",
                        task.getTaskName(), task.getTargetSlot() != null ? task.getTargetSlot().getSlotNumber() : "N/A")
        );

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
        String treeName = rental.getTree() != null ? rental.getTree().getTreeName() : "cay trong";

        String message = String.format(
                "Nhan vien %s bao: cay %s tai o dat %s da san sang thu hoach. Ban muon tu thu hoach hay nho nhan vien thu hoach giup?",
                staffName, treeName, slotNumber);

        notificationService.createNotification(
                rental.getUser().getId(),
                "San sang thu hoach",
                message,
                "HARVEST_CHOICE"
        );

        firebaseMessagingService.sendPushNotification(
                rental.getUser().getId(),
                "San sang thu hoach",
                String.format("%s bao cay %s tai o %s da san sang thu hoach", staffName, treeName, slotNumber)
        );

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

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            task.setStatus(ETaskStatus.COMPLETED);
            task.setRejectionReason(null);

            // Notify staff
            if (task.getAssignedStaff() != null) {
                notificationService.createNotification(
                        task.getAssignedStaff().getId(),
                        "Task Approved",
                        "Your work on task " + task.getTaskName() + " has been approved.",
                        "TASK_APPROVED"
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
            if (task.getAssignedStaff() != null) {
                notificationService.createNotification(
                        task.getAssignedStaff().getId(),
                        "Task Rejected",
                        "Your work on task " + task.getTaskName() + " has been rejected. Reason: " + request.getRejectionReason(),
                        "TASK_REJECTED"
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

        String staffName = task.getAssignedStaff() != null ? task.getAssignedStaff().getFullName() : "Nhan vien lam vuon";
        String slotNumber = task.getTargetSlot().getSlotNumber();
        String treeName = rental.getTree() != null ? rental.getTree().getTreeName() : "cay trong";

        String message = String.format("Nhan vien %s da thu hoach xong cay %s tai o dat %s.", staffName, treeName, slotNumber);

        notificationService.createNotification(
                rental.getUser().getId(),
                "Da thu hoach xong",
                message,
                "HARVEST_DONE"
        );

        firebaseMessagingService.sendPushNotification(
                rental.getUser().getId(),
                "Da thu hoach xong",
                String.format("%s da thu hoach cay %s tai o %s", staffName, treeName, slotNumber)
        );

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
}
