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
    
    EquipmentDTO updateStock(Long id, Integer additionalQuantity);

    // Gán nhiều thiết bị IoT (lấy từ kho hoặc khai báo mới) vào 1 trụ trong cùng 1 transaction
    List<EquipmentDTO> bindEquipmentsToPillar(Long pillarId, List<swp490.greeenslot.dto.PillarEquipmentBindingDTO> bindings);
}
