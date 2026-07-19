package swp490.greeenslot.service;

import swp490.greeenslot.dto.TreePlantingRequestCreateDTO;
import swp490.greeenslot.dto.TreePlantingRequestDTO;

import java.util.List;

public interface TreePlantingService {
    
    List<TreePlantingRequestDTO> getAllRequests();
    
    TreePlantingRequestDTO getRequestById(Long id);
    
    TreePlantingRequestDTO createRequest(TreePlantingRequestCreateDTO dto, String username);
    
    TreePlantingRequestDTO approveRequest(Long id, String username);
    
    TreePlantingRequestDTO rejectRequest(Long id, String reason, String username);
    
    TreePlantingRequestDTO completeRequest(Long id, String username);
    
    List<TreePlantingRequestDTO> getRequestsByUser(String username);
    
    List<TreePlantingRequestDTO> getPendingRequests();
}
