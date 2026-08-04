package swp490.greeenslot.service;

import swp490.greeenslot.dto.NotificationPreferenceDTO;

public interface NotificationPreferenceService {
    
    NotificationPreferenceDTO getPreferences(String username);
    
    NotificationPreferenceDTO updatePreferences(NotificationPreferenceDTO dto, String username);
    
    NotificationPreferenceDTO createDefaultPreferences(Long userId);
}
