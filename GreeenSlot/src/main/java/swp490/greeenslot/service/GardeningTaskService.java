package swp490.greeenslot.service;

import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.GardeningTask;

import java.util.List;

public interface GardeningTaskService {
    GardeningTask requestService(ServiceRequestDTO request, String username);
    
    GardeningTask assignTask(TaskAssignmentDTO request);
    
    List<GardeningTask> getMyTasks(String username);
    
    GardeningTask updateTaskStatus(Long taskId, TaskStatusUpdateDTO request, String username);
    
    GardeningTask reportIssue(Long taskId, IssueReportRequestDTO request, String username);
}
