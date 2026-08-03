package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.NotificationPreferenceDTO;
import swp490.greeenslot.entity.NotificationPreference;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.NotificationPreferenceRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.NotificationPreferenceService;

import java.util.Optional;

@Service
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public NotificationPreferenceDTO getPreferences(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Optional<NotificationPreference> prefOpt = notificationPreferenceRepository.findByUserId(user.getId());
        
        if (prefOpt.isPresent()) {
            return mapToDTO(prefOpt.get());
        } else {
            // Create default preferences if not exist
            NotificationPreference defaultPref = createDefaultPreferencesEntity(user.getId());
            return mapToDTO(defaultPref);
        }
    }

    @Override
    @Transactional
    public NotificationPreferenceDTO updatePreferences(NotificationPreferenceDTO dto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        NotificationPreference preference = notificationPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference();
                    newPref.setUser(user);
                    return newPref;
                });

        preference.setEmailEnabled(dto.getEmailEnabled() != null ? dto.getEmailEnabled() : preference.getEmailEnabled());
        preference.setPushEnabled(dto.getPushEnabled() != null ? dto.getPushEnabled() : preference.getPushEnabled());
        preference.setSmsEnabled(dto.getSmsEnabled() != null ? dto.getSmsEnabled() : preference.getSmsEnabled());
        preference.setIotAlertsEnabled(dto.getIotAlertsEnabled() != null ? dto.getIotAlertsEnabled() : preference.getIotAlertsEnabled());
        preference.setTaskAssignmentEnabled(dto.getTaskAssignmentEnabled() != null ? dto.getTaskAssignmentEnabled() : preference.getTaskAssignmentEnabled());
        preference.setPaymentAlertsEnabled(dto.getPaymentAlertsEnabled() != null ? dto.getPaymentAlertsEnabled() : preference.getPaymentAlertsEnabled());
        preference.setRentalExpirationEnabled(dto.getRentalExpirationEnabled() != null ? dto.getRentalExpirationEnabled() : preference.getRentalExpirationEnabled());
        preference.setMarketingEnabled(dto.getMarketingEnabled() != null ? dto.getMarketingEnabled() : preference.getMarketingEnabled());

        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public NotificationPreferenceDTO createDefaultPreferences(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        NotificationPreference preference = createDefaultPreferencesEntity(userId);
        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        return mapToDTO(saved);
    }

    private NotificationPreference createDefaultPreferencesEntity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        NotificationPreference preference = new NotificationPreference();
        preference.setUser(user);
        preference.setEmailEnabled(true);
        preference.setPushEnabled(true);
        preference.setSmsEnabled(false);
        preference.setIotAlertsEnabled(true);
        preference.setTaskAssignmentEnabled(true);
        preference.setPaymentAlertsEnabled(true);
        preference.setRentalExpirationEnabled(true);
        preference.setMarketingEnabled(false);
        return preference;
    }

    private NotificationPreferenceDTO mapToDTO(NotificationPreference preference) {
        return new NotificationPreferenceDTO(
                preference.getId(),
                preference.getUser() != null ? preference.getUser().getId() : null,
                preference.getEmailEnabled(),
                preference.getPushEnabled(),
                preference.getSmsEnabled(),
                preference.getIotAlertsEnabled(),
                preference.getTaskAssignmentEnabled(),
                preference.getPaymentAlertsEnabled(),
                preference.getRentalExpirationEnabled(),
                preference.getMarketingEnabled()
        );
    }
}
