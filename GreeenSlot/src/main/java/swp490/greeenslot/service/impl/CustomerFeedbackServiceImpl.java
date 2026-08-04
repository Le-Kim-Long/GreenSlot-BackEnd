package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.CustomerFeedbackDTO;
import swp490.greeenslot.entity.CustomerFeedback;
import swp490.greeenslot.entity.EFeedbackCategory;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.CustomerFeedbackRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.CustomerFeedbackService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerFeedbackServiceImpl implements CustomerFeedbackService {

    @Autowired
    private CustomerFeedbackRepository customerFeedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public CustomerFeedback submitFeedback(CustomerFeedbackDTO feedbackDTO, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Validate rating
        if (feedbackDTO.getRating() < 1 || feedbackDTO.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Parse category
        EFeedbackCategory category;
        try {
            category = EFeedbackCategory.valueOf(feedbackDTO.getCategory());
        } catch (IllegalArgumentException NullPointerException) {
            category = EFeedbackCategory.GENERAL;
        }

        CustomerFeedback feedback = new CustomerFeedback();
        feedback.setUser(user);
        feedback.setCategory(category);
        feedback.setRating(feedbackDTO.getRating());
        feedback.setComment(feedbackDTO.getComment());
        feedback.setIsAnonymous(feedbackDTO.getIsAnonymous() != null ? feedbackDTO.getIsAnonymous() : false);

        return customerFeedbackRepository.save(feedback);
    }

    @Override
    public List<CustomerFeedbackDTO> getMyFeedback(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        List<CustomerFeedback> feedbacks = customerFeedbackRepository.findByUserId(user.getId());
        return feedbacks.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<CustomerFeedbackDTO> getAllPublicFeedback() {
        List<CustomerFeedback> feedbacks = customerFeedbackRepository.findByIsAnonymousFalseOrderByCreatedAtDesc();
        return feedbacks.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<CustomerFeedbackDTO> getFeedbackByCategory(String category) {
        EFeedbackCategory categoryEnum;
        try {
            categoryEnum = EFeedbackCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }
        List<CustomerFeedback> feedbacks = customerFeedbackRepository.findByCategory(categoryEnum);
        return feedbacks.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private CustomerFeedbackDTO mapToDTO(CustomerFeedback feedback) {
        CustomerFeedbackDTO dto = new CustomerFeedbackDTO();
        dto.setId(feedback.getId());
        dto.setUserId(feedback.getUser() != null ? feedback.getUser().getId() : null);
        dto.setUserName(feedback.getIsAnonymous() != null && feedback.getIsAnonymous() 
                ? "Anonymous" 
                : (feedback.getUser() != null ? feedback.getUser().getFullName() : "N/A"));
        dto.setCategory(feedback.getCategory() != null ? feedback.getCategory().name() : null);
        dto.setRating(feedback.getRating());
        dto.setComment(feedback.getComment());
        dto.setIsAnonymous(feedback.getIsAnonymous());
        dto.setCreatedAt(feedback.getCreatedAt() != null ? feedback.getCreatedAt().toString() : null);
        return dto;
    }
}
