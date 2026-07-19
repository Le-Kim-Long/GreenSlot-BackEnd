package swp490.greeenslot.service;

import swp490.greeenslot.dto.EquipmentDTO;

import java.util.List;

public interface EquipmentService {
    
    List<EquipmentDTO> getAllEquipment();
    
    EquipmentDTO getEquipmentById(Long id);
    
    EquipmentDTO createEquipment(EquipmentDTO dto);
    
    EquipmentDTO updateEquipment(Long id, EquipmentDTO dto);
    
    void deleteEquipment(Long id);
    
    List<EquipmentDTO> getEquipmentByPillar(Long pillarId);
    
    List<EquipmentDTO> getEquipmentByStatus(String status);
}
