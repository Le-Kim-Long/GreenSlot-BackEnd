package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.ServiceFeedbackDTO;
import swp490.greeenslot.entity.GardeningTask;
import swp490.greeenslot.entity.ServiceFeedback;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.GardeningTaskRepository;
import swp490.greeenslot.repository.ServiceFeedbackRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.ServiceFeedbackService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceFeedbackServiceImpl implements ServiceFeedbackService {

    @Autowired
    private ServiceFeedbackRepository serviceFeedbackRepository;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public ServiceFeedback createFeedback(ServiceFeedbackDTO feedbackDTO, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        GardeningTask task = gardeningTaskRepository.findById(feedbackDTO.getGardeningTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Gardening task not found with ID: " + feedbackDTO.getGardeningTaskId()));

        // Check if user already provided feedback for this task
        serviceFeedbackRepository.findByGardeningTaskIdAndUserId(task.getId(), user.getId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("You have already provided feedback for this task");
                });

        // Validate rating
        if (feedbackDTO.getRating() < 1 || feedbackDTO.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        ServiceFeedback feedback = new ServiceFeedback();
        feedback.setGardeningTask(task);
        feedback.setUser(user);
        feedback.setRating(feedbackDTO.getRating());
        feedback.setComment(feedbackDTO.getComment());

        return serviceFeedbackRepository.save(feedback);
    }

    @Override
    public List<ServiceFeedbackDTO> getFeedbackByTaskId(Long taskId) {
        List<ServiceFeedback> feedbacks = serviceFeedbackRepository.findByGardeningTaskId(taskId);
        return feedbacks.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<ServiceFeedbackDTO> getMyFeedback(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        List<ServiceFeedback> feedbacks = serviceFeedbackRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return feedbacks.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public Double getAverageRatingForTask(Long taskId) {
        List<ServiceFeedback> feedbacks = serviceFeedbackRepository.findByGardeningTaskId(taskId);
        if (feedbacks.isEmpty()) {
            return 0.0;
        }
        return feedbacks.stream().mapToInt(ServiceFeedback::getRating).average().orElse(0.0);
    }

    @Override
    public Double getAverageRatingForStaff(Long staffId) {
        // Get all tasks assigned to this staff
        List<GardeningTask> tasks = gardeningTaskRepository.findByAssignedStaffId(staffId);
        if (tasks.isEmpty()) {
            return 0.0;
        }

        // Get all feedback for these tasks
        List<ServiceFeedback> allFeedbacks = tasks.stream()
                .flatMap(task -> serviceFeedbackRepository.findByGardeningTaskId(task.getId()).stream())
                .toList();

        if (allFeedbacks.isEmpty()) {
            return 0.0;
        }

        return allFeedbacks.stream().mapToInt(ServiceFeedback::getRating).average().orElse(0.0);
    }

    private ServiceFeedbackDTO mapToDTO(ServiceFeedback feedback) {
        return new ServiceFeedbackDTO(
                feedback.getId(),
                feedback.getGardeningTask() != null ? feedback.getGardeningTask().getId() : null,
                feedback.getUser() != null ? feedback.getUser().getId() : null,
                feedback.getUser() != null ? feedback.getUser().getFullName() : null,
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt() != null ? feedback.getCreatedAt().toString() : null
        );
    }
}
