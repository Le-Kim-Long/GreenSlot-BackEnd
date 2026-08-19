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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Autowired
    private swp490.greeenslot.service.LocationContextService locationContextService;

    private Long getEquipmentLocationId(Equipment equipment) {
        if (equipment != null && equipment.getPillar() != null && equipment.getPillar().getLocation() != null) {
            return equipment.getPillar().getLocation().getId();
        }
        return null;
    }

    private boolean isEquipmentAccessible(Equipment equipment, Long locationId) {
        if (locationId == null) return true;
        Long locId = getEquipmentLocationId(equipment);
        return locId != null && locId.equals(locationId);
    }

    @Override
    public List<EquipmentDTO> getAllEquipment() {
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        return equipmentRepository.findAll().stream()
                .filter(e -> isEquipmentAccessible(e, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EquipmentDTO getEquipmentById(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        Long locId = getEquipmentLocationId(equipment);
        locationContextService.validateLocationAccess(locId);
        return mapToDTO(equipment);
    }

    @Override
    @Transactional
    public EquipmentDTO createEquipment(EquipmentDTO dto) {
        validateEquipmentDates(dto);
        if (dto.getPillarId() != null) {
            Pillar pillar = pillarRepository.findById(dto.getPillarId())
                    .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + dto.getPillarId()));
            if (pillar.getLocation() != null) {
                locationContextService.validateLocationAccess(pillar.getLocation().getId());
            }
        }
        Equipment equipment = mapToEntity(dto);
        Equipment savedEquipment = equipmentRepository.save(equipment);
        return mapToDTO(savedEquipment);
    }

    @Override
    @Transactional
    public EquipmentDTO updateEquipment(Long id, EquipmentDTO dto) {
        validateEquipmentDates(dto);
        Equipment existingEquipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        Long locId = getEquipmentLocationId(existingEquipment);
        locationContextService.validateLocationAccess(locId);

        if (dto.getPillarId() != null) {
            Pillar newPillar = pillarRepository.findById(dto.getPillarId())
                    .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + dto.getPillarId()));
            if (newPillar.getLocation() != null) {
                locationContextService.validateLocationAccess(newPillar.getLocation().getId());
            }
        }

        updateEntityFromDTO(existingEquipment, dto);
        Equipment updatedEquipment = equipmentRepository.save(existingEquipment);
        return mapToDTO(updatedEquipment);
    }

    private void validateEquipmentDates(EquipmentDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        if (dto.getPurchaseDate() != null && dto.getPurchaseDate().isAfter(now)) {
            throw new IllegalArgumentException("Purchase date cannot be in the future");
        }
        if (dto.getLastMaintenanceDate() != null && dto.getLastMaintenanceDate().isAfter(now)) {
            throw new IllegalArgumentException("Last maintenance date cannot be in the future");
        }
    }

    @Override
    @Transactional
    public void deleteEquipment(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        Long locId = getEquipmentLocationId(equipment);
        locationContextService.validateLocationAccess(locId);
        equipmentRepository.delete(equipment);
    }

    @Override
    public List<EquipmentDTO> getEquipmentByPillar(Long pillarId) {
        Pillar pillar = pillarRepository.findById(pillarId)
                .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + pillarId));
        if (pillar.getLocation() != null) {
            locationContextService.validateLocationAccess(pillar.getLocation().getId());
        }
        return equipmentRepository.findByPillar(pillar).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EquipmentDTO> getEquipmentByStatus(String status) {
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        EEquipmentStatus equipmentStatus = EEquipmentStatus.valueOf(status.toUpperCase());
        return equipmentRepository.findByStatus(equipmentStatus).stream()
                .filter(e -> isEquipmentAccessible(e, targetLocationId))
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
