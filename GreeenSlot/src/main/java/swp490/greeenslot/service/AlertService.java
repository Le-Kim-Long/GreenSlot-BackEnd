package swp490.greeenslot.service;

import swp490.greeenslot.dto.AlertDTO;
import swp490.greeenslot.dto.AlertProcessingLogDTO;
import swp490.greeenslot.dto.AlertProcessingRequestDTO;

import java.util.List;

public interface AlertService {
    
    List<AlertDTO> getAllAlerts();
    
    AlertDTO getAlertById(Long id);
    
    List<AlertDTO> getAlertsByStatus(String status);
    
    List<AlertDTO> getAlertsByPillar(Long pillarId);
    
    List<AlertDTO> getPendingAlerts();
    
    AlertProcessingLogDTO processAlert(AlertProcessingRequestDTO request, String username);
    
    List<AlertProcessingLogDTO> getAlertProcessingLogs(Long alertId);
}
