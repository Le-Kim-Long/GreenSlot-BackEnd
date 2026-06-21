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

    @Override
    @Transactional
    public GardeningTask requestService(ServiceRequestDTO request, String username) {
        // Validate active rental for the slot
        slotRentalRepository.findActiveRentalBySlotAndUser(request.getSlotId(), username, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("No active rental found for slot ID " + request.getSlotId() + " belonging to customer " + username));

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
        task.setAssignedStaff(null); // Unassigned initially
        task.setCreatedAt(LocalDateTime.now());

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

        return gardeningTaskRepository.save(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GardeningTask> getMyTasks(String username) {
        return gardeningTaskRepository.findByAssignedStaffUsernameOrderByCreatedAtDesc(username);
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

        return gardeningTaskRepository.save(issueTask);
    }
}
