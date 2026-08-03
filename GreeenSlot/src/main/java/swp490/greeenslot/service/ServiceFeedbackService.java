package swp490.greeenslot.service;

import swp490.greeenslot.dto.ServiceFeedbackDTO;
import swp490.greeenslot.entity.ServiceFeedback;

import java.util.List;

public interface ServiceFeedbackService {
    
    ServiceFeedback createFeedback(ServiceFeedbackDTO feedbackDTO, String username);
    
    List<ServiceFeedbackDTO> getFeedbackByTaskId(Long taskId);
    
    List<ServiceFeedbackDTO> getMyFeedback(String username);
    
    Double getAverageRatingForTask(Long taskId);
    
    Double getAverageRatingForStaff(Long staffId);
}
