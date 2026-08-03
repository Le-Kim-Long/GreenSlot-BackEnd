package swp490.greeenslot.service;

import swp490.greeenslot.dto.CustomerFeedbackDTO;
import swp490.greeenslot.entity.CustomerFeedback;

import java.util.List;

public interface CustomerFeedbackService {
    
    CustomerFeedback submitFeedback(CustomerFeedbackDTO feedbackDTO, String username);
    
    List<CustomerFeedbackDTO> getMyFeedback(String username);
    
    List<CustomerFeedbackDTO> getAllPublicFeedback();
    
    List<CustomerFeedbackDTO> getFeedbackByCategory(String category);
}
