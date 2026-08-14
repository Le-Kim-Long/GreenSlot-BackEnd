package swp490.greeenslot.service;

import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.GardeningTask;

import java.util.List;

public interface GardeningTaskService {
    GardeningTask requestService(ServiceRequestDTO request, String username);
    
    GardeningTask createTask(TaskAssignmentDTO request);
    
    GardeningTask assignStaffToTask(Long taskId, TaskAssignmentDTO request);
    
    List<GardeningTask> getMyTasks(String username);

    List<GardeningTask> getAllTasks();

    GardeningTask updateTaskStatus(Long taskId, TaskStatusUpdateDTO request, String username);
    
    GardeningTask reportIssue(Long taskId, IssueReportRequestDTO request, String username);

    GardeningTask reviewTaskEvidence(Long taskId, TaskReviewRequestDTO request);
}
