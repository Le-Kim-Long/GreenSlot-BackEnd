package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.EquipmentDTO;
import swp490.greeenslot.entity.EEquipmentStatus;
import swp490.greeenslot.entity.Equipment;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.repository.EquipmentRepository;
import swp490.greeenslot.repository.PillarRepository;
import swp490.greeenslot.service.EquipmentService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Override
    public List<EquipmentDTO> getAllEquipment() {
        return equipmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EquipmentDTO getEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
    }

    @Override
    @Transactional
    public EquipmentDTO createEquipment(EquipmentDTO dto) {
        Equipment equipment = mapToEntity(dto);
        Equipment savedEquipment = equipmentRepository.save(equipment);
        return mapToDTO(savedEquipment);
    }

    @Override
    @Transactional
    public EquipmentDTO updateEquipment(Long id, EquipmentDTO dto) {
        Equipment existingEquipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        
        updateEntityFromDTO(existingEquipment, dto);
        Equipment updatedEquipment = equipmentRepository.save(existingEquipment);
        return mapToDTO(updatedEquipment);
    }

    @Override
    @Transactional
    public void deleteEquipment(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        equipmentRepository.delete(equipment);
    }

    @Override
    public List<EquipmentDTO> getEquipmentByPillar(Long pillarId) {
        Pillar pillar = pillarRepository.findById(pillarId)
                .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + pillarId));
        return equipmentRepository.findByPillar(pillar).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EquipmentDTO> getEquipmentByStatus(String status) {
        EEquipmentStatus equipmentStatus = EEquipmentStatus.valueOf(status.toUpperCase());
        return equipmentRepository.findByStatus(equipmentStatus).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private EquipmentDTO mapToDTO(Equipment equipment) {
        return new EquipmentDTO(
                equipment.getId(),
                equipment.getEquipmentName(),
                equipment.getSerialNumber(),
                equipment.getDescription(),
                equipment.getStatus() != null ? equipment.getStatus().name() : null,
                equipment.getPillar() != null ? equipment.getPillar().getId() : null,
                equipment.getPillar() != null ? equipment.getPillar().getPillarCode() : null,
                equipment.getPurchaseDate(),
                equipment.getLastMaintenanceDate(),
                equipment.getImageUrl()
        );
    }

    private Equipment mapToEntity(EquipmentDTO dto) {
        Equipment equipment = new Equipment();
        equipment.setEquipmentName(dto.getEquipmentName());
        equipment.setSerialNumber(dto.getSerialNumber());
        equipment.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            equipment.setStatus(EEquipmentStatus.valueOf(dto.getStatus().toUpperCase()));
        }
        if (dto.getPillarId() != null) {
            Pillar pillar = pillarRepository.findById(dto.getPillarId())
                    .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + dto.getPillarId()));
            equipment.setPillar(pillar);
        }
        equipment.setPurchaseDate(dto.getPurchaseDate());
        equipment.setLastMaintenanceDate(dto.getLastMaintenanceDate());
        equipment.setImageUrl(dto.getImageUrl());
        return equipment;
    }

    private void updateEntityFromDTO(Equipment equipment, EquipmentDTO dto) {
        if (dto.getEquipmentName() != null) equipment.setEquipmentName(dto.getEquipmentName());
        if (dto.getSerialNumber() != null) equipment.setSerialNumber(dto.getSerialNumber());
        if (dto.getDescription() != null) equipment.setDescription(dto.getDescription());
        if (dto.getStatus() != null) equipment.setStatus(EEquipmentStatus.valueOf(dto.getStatus().toUpperCase()));
        if (dto.getPillarId() != null) {
            Pillar pillar = pillarRepository.findById(dto.getPillarId())
                    .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + dto.getPillarId()));
            equipment.setPillar(pillar);
        }
        if (dto.getPurchaseDate() != null) equipment.setPurchaseDate(dto.getPurchaseDate());
        if (dto.getLastMaintenanceDate() != null) equipment.setLastMaintenanceDate(dto.getLastMaintenanceDate());
        if (dto.getImageUrl() != null) equipment.setImageUrl(dto.getImageUrl());
    }
}
