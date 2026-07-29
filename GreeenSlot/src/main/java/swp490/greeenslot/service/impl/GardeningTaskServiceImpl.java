package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.GardeningTaskService;
import swp490.greeenslot.service.NotificationService;

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
    private NotificationService notificationService;

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

        // Notify Location Managers about the new service request
        if (slot.getPillar() != null && slot.getPillar().getLocation() != null) {
            List<User> managers = userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, slot.getPillar().getLocation().getId());
            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        "New Service Request",
                        String.format("New service request for Slot %s: %s", slot.getSlotNumber(), serviceType.getServiceName()),
                        "SERVICE_REQUEST"
                );
            }
        }

        return savedTask;
    }

    @Override
    @Transactional
    public GardeningTask createTask(TaskCreateDTO request) {
        GardenSlot slot = gardenSlotRepository.findById(request.getTargetSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID " + request.getTargetSlotId()));

        ETaskType type;
        try {
            type = ETaskType.valueOf(request.getTaskType().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid task type. Must be MAINTENANCE or CLEANING");
        }

        if (type == ETaskType.SERVICE_REQUEST) {
            throw new IllegalArgumentException("SERVICE_REQUEST tasks cannot be created directly by manager.");
        }

        GardeningTask task = new GardeningTask();
        task.setTaskName(request.getTaskName());
        task.setDescription(request.getDescription());
        task.setStatus(ETaskStatus.PENDING);
        task.setTaskType(type);
        task.setTargetSlot(slot);
        task.setCreatedAt(LocalDateTime.now());

        return gardeningTaskRepository.save(task);
    }

    @Override
    @Transactional
    public GardeningTask assignStaff(Long taskId, Long staffId) {
        GardeningTask task = gardeningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + taskId));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found with ID " + staffId));

        boolean hasStaffRole = staff.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_GARDEN_STAFF);

        if (!hasStaffRole) {
            throw new IllegalArgumentException("User with ID " + staffId + " does not have ROLE_GARDEN_STAFF");
        }

        task.setAssignedStaff(staff);
        return gardeningTaskRepository.save(task);
    }

    @Override
    @Transactional
    public GardeningTask assignTask(TaskAssignmentDTO request) {
        // Fetch target staff and check role
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found with ID " + request.getStaffId()));

        boolean hasStaffRole = staff.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_GARDEN_STAFF);

        if (!hasStaffRole) {
            throw new IllegalArgumentException("User with ID " + request.getStaffId() + " does not have ROLE_GARDEN_STAFF");
        }

        GardeningTask task;

        if (request.getTaskId() != null) {
            // Assign existing task
            task = gardeningTaskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID " + request.getTaskId()));
            task.setAssignedStaff(staff);
        } else {
            // Create and assign a new task
            if (request.getTaskName() == null || request.getTaskName().trim().isEmpty()) {
                throw new IllegalArgumentException("Task name is required to create a new task");
            }
            if (request.getTaskType() == null) {
                throw new IllegalArgumentException("Task type is required to create a new task");
            }
            if (request.getTargetSlotId() == null) {
                throw new IllegalArgumentException("Target slot ID is required to create a new task");
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

            task = new GardeningTask();
            task.setTaskName(request.getTaskName());
            task.setDescription(request.getDescription());
            task.setStatus(ETaskStatus.PENDING);
            task.setTaskType(type);
            task.setTargetSlot(slot);
            task.setAssignedStaff(staff);
            task.setCreatedAt(LocalDateTime.now());
        }

        GardeningTask savedTask = gardeningTaskRepository.save(task);

        // Notify the assigned staff
        if (savedTask.getAssignedStaff() != null) {
            notificationService.createNotification(
                    savedTask.getAssignedStaff().getId(),
                    "New Task Assigned",
                    String.format("You have been assigned a new task: %s for Slot %s", savedTask.getTaskName(), savedTask.getTargetSlot().getSlotNumber()),
                    "TASK_ASSIGNED"
            );
        }

        return savedTask;
    }

    @Override
    public List<GardeningTask> getMyTasks(String username) {
        return gardeningTaskRepository.findByAssignedStaffUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    public List<GardeningTask> getAllTasks() {
        return gardeningTaskRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
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
            throw new IllegalArgumentException("Invalid task status. Must be PENDING, IN_PROGRESS, or COMPLETED");
        }

        // Validate status transition sequence
        if (task.getStatus() == ETaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify status of a COMPLETED task");
        }

        if (task.getStatus() == ETaskStatus.PENDING && newStatus == ETaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot transition directly from PENDING to COMPLETED. Must go through IN_PROGRESS first.");
        }

        if (newStatus == ETaskStatus.COMPLETED) {
            if (request.getEvidenceImageUrl() == null || request.getEvidenceImageUrl().trim().isEmpty()) {
                throw new IllegalArgumentException("Evidence image URL is required when marking task as COMPLETED");
            }
            task.setEvidenceImageUrl(request.getEvidenceImageUrl());
        }

        task.setStatus(newStatus);
        GardeningTask savedTask = gardeningTaskRepository.save(task);

        // Notify Location Managers about the task update
        if (savedTask.getTargetSlot() != null && savedTask.getTargetSlot().getPillar() != null && savedTask.getTargetSlot().getPillar().getLocation() != null) {
            List<User> managers = userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, savedTask.getTargetSlot().getPillar().getLocation().getId());
            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        "Task Status Updated",
                        String.format("Task '%s' for Slot %s updated to %s by %s", savedTask.getTaskName(), savedTask.getTargetSlot().getSlotNumber(), newStatus.name(), username),
                        "TASK_UPDATE"
                );
            }
        }

        // Notify requester if it's a service request
        if (savedTask.getTaskType() == ETaskType.SERVICE_REQUEST && savedTask.getRequestedBy() != null) {
            notificationService.createNotification(
                    savedTask.getRequestedBy().getId(),
                    "Service Request Update",
                    String.format("Your service request '%s' is now %s", savedTask.getTaskName(), newStatus.name()),
                    "SERVICE_UPDATE"
            );
        }

        return savedTask;
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

        // Notify Location Managers about the reported issue
        if (originalTask.getTargetSlot() != null && originalTask.getTargetSlot().getPillar() != null && originalTask.getTargetSlot().getPillar().getLocation() != null) {
            List<User> managers = userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, originalTask.getTargetSlot().getPillar().getLocation().getId());
            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        "Issue Reported on Task",
                        String.format("Staff %s reported an issue on Task #%d: %s", username, taskId, request.getIssueTitle()),
                        "ISSUE_REPORT"
                );
            }
        }

        // Update original task
        originalTask.setStatus(ETaskStatus.CANCELLED);
        originalTask.setDescription(originalTask.getDescription() + "\n[BLOCKED_BY_ISSUE: " + savedIssue.getTaskName() + "]");
        gardeningTaskRepository.save(originalTask);

        return savedIssue;
    }
}
