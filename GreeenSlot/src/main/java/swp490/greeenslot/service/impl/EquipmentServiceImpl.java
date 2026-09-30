package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.EquipmentDTO;
import swp490.greeenslot.entity.EEquipmentStatus;
import swp490.greeenslot.entity.Equipment;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.EquipmentRepository;
import swp490.greeenslot.repository.LocationRepository;
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
    private LocationRepository locationRepository;

    @Autowired
    private swp490.greeenslot.service.LocationContextService locationContextService;

    private Long getEquipmentLocationId(Equipment equipment) {
        if (equipment != null) {
            if (equipment.getLocation() != null) {
                return equipment.getLocation().getId();
            }
            if (equipment.getPillar() != null && equipment.getPillar().getLocation() != null) {
                return equipment.getPillar().getLocation().getId();
            }
        }
        return null;
    }

    private boolean isEquipmentAccessible(Equipment equipment, Long locationId) {
        if (locationId == null) return true;
        Long locId = getEquipmentLocationId(equipment);
        return locId == null || locId.equals(locationId);
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
        if (locId != null) {
            locationContextService.validateLocationAccess(locId);
        }
        return mapToDTO(equipment);
    }

    @Override
    @Transactional
    public EquipmentDTO createEquipment(EquipmentDTO dto) {
        validateEquipmentDates(dto);

        Long targetLocationId = locationContextService.resolveTargetLocationId(dto.getLocationId());
        Location location = null;
        if (targetLocationId != null) {
            locationContextService.validateLocationAccess(targetLocationId);
            location = locationRepository.findById(targetLocationId)
                    .orElseThrow(() -> new RuntimeException("Location not found with id: " + targetLocationId));
        }

        Pillar pillar = null;
        if (dto.getPillarId() != null && dto.getPillarId() > 0) {
            pillar = pillarRepository.findById(dto.getPillarId())
                    .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + dto.getPillarId()));
            if (pillar.getLocation() != null) {
                locationContextService.validateLocationAccess(pillar.getLocation().getId());
                if (location == null) {
                    location = pillar.getLocation();
                } else if (!location.getId().equals(pillar.getLocation().getId())) {
                    throw new IllegalArgumentException("Trụ đã chọn không thuộc cơ sở này.");
                }
            }
        }

        Equipment equipment = mapToEntity(dto);
        equipment.setPillar(pillar);
        equipment.setLocation(location);

        Equipment savedEquipment = equipmentRepository.save(equipment);
        return mapToDTO(savedEquipment);
    }

    @Override
    @Transactional
    public EquipmentDTO updateEquipment(Long id, EquipmentDTO dto) {
        validateEquipmentDates(dto);
        Equipment existingEquipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        Long currentLocId = getEquipmentLocationId(existingEquipment);
        if (currentLocId != null) {
            locationContextService.validateLocationAccess(currentLocId);
        } else if (locationContextService.isLocationManager()) {
            User currentUser = locationContextService.getCurrentUser();
            if (currentUser != null && currentUser.getLocation() != null) {
                existingEquipment.setLocation(currentUser.getLocation());
            }
        }

        Long targetLocationId = dto.getLocationId() != null 
                ? locationContextService.resolveTargetLocationId(dto.getLocationId())
                : currentLocId;

        Location location = null;
        if (targetLocationId != null) {
            locationContextService.validateLocationAccess(targetLocationId);
            location = locationRepository.findById(targetLocationId)
                    .orElseThrow(() -> new RuntimeException("Location not found with id: " + targetLocationId));
        }

        Pillar newPillar = null;
        if (dto.getPillarId() != null && dto.getPillarId() > 0) {
            newPillar = pillarRepository.findById(dto.getPillarId())
                    .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + dto.getPillarId()));
            if (newPillar.getLocation() != null) {
                locationContextService.validateLocationAccess(newPillar.getLocation().getId());
                if (location == null) {
                    location = newPillar.getLocation();
                } else if (!location.getId().equals(newPillar.getLocation().getId())) {
                    throw new IllegalArgumentException("Trụ đã chọn không thuộc cơ sở này.");
                }
            }
        }

        updateEntityFromDTO(existingEquipment, dto);
        existingEquipment.setPillar(newPillar);
        if (location != null) {
            existingEquipment.setLocation(location);
        }

        Equipment updatedEquipment = equipmentRepository.save(existingEquipment);
        return mapToDTO(updatedEquipment);
    }

    private void validateEquipmentDates(EquipmentDTO dto) {
        if (dto.getQuantity() != null && dto.getQuantity() < 0) {
            throw new IllegalArgumentException("Số lượng thiết bị không được là số âm.");
        }
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
        if (locId != null) {
            locationContextService.validateLocationAccess(locId);
        }
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

    @Override
    @Transactional
    public EquipmentDTO updateStock(Long id, Integer additionalQuantity) {
        if (additionalQuantity == null || additionalQuantity <= 0) {
            throw new IllegalArgumentException("Số lượng thiết bị nhập thêm phải lớn hơn 0.");
        }
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        Long locId = getEquipmentLocationId(equipment);
        if (locId != null) {
            locationContextService.validateLocationAccess(locId);
        } else if (locationContextService.isLocationManager()) {
            User currentUser = locationContextService.getCurrentUser();
            if (currentUser != null && currentUser.getLocation() != null) {
                equipment.setLocation(currentUser.getLocation());
            }
        }

        int current = equipment.getQuantity() != null ? equipment.getQuantity() : 0;
        equipment.setQuantity(current + additionalQuantity);
        if (equipment.getQuantity() > 0 && equipment.getPillar() == null && equipment.getStatus() == EEquipmentStatus.IN_USE) {
            equipment.setStatus(EEquipmentStatus.AVAILABLE);
        }
        Equipment saved = equipmentRepository.save(equipment);
        return mapToDTO(saved);
    }

    private EquipmentDTO mapToDTO(Equipment equipment) {
        Long locId = null;
        String locName = null;
        if (equipment.getLocation() != null) {
            locId = equipment.getLocation().getId();
            locName = equipment.getLocation().getName();
        } else if (equipment.getPillar() != null && equipment.getPillar().getLocation() != null) {
            locId = equipment.getPillar().getLocation().getId();
            locName = equipment.getPillar().getLocation().getName();
        }

        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(equipment.getId());
        dto.setEquipmentName(equipment.getEquipmentName());
        dto.setSerialNumber(equipment.getSerialNumber());
        dto.setDescription(equipment.getDescription());
        dto.setStatus(equipment.getStatus() != null ? equipment.getStatus().name() : null);
        dto.setPillarId(equipment.getPillar() != null ? equipment.getPillar().getId() : null);
        dto.setPillarCode(equipment.getPillar() != null ? equipment.getPillar().getPillarCode() : null);
        dto.setLocationId(locId);
        dto.setLocationName(locName);
        dto.setPurchaseDate(equipment.getPurchaseDate());
        dto.setLastMaintenanceDate(equipment.getLastMaintenanceDate());
        dto.setImageUrl(equipment.getImageUrl());
        dto.setQuantity(equipment.getQuantity() != null ? equipment.getQuantity() : 1);
        return dto;
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
        equipment.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);
        return equipment;
    }

    private void updateEntityFromDTO(Equipment equipment, EquipmentDTO dto) {
        if (dto.getEquipmentName() != null) equipment.setEquipmentName(dto.getEquipmentName());
        if (dto.getSerialNumber() != null) equipment.setSerialNumber(dto.getSerialNumber());
        if (dto.getDescription() != null) equipment.setDescription(dto.getDescription());
        if (dto.getStatus() != null) equipment.setStatus(EEquipmentStatus.valueOf(dto.getStatus().toUpperCase()));
        if (dto.getPillarId() != null && dto.getPillarId() > 0) {
            Pillar pillar = pillarRepository.findById(dto.getPillarId())
                    .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + dto.getPillarId()));
            equipment.setPillar(pillar);
        } else {
            equipment.setPillar(null);
        }
        if (dto.getPurchaseDate() != null) equipment.setPurchaseDate(dto.getPurchaseDate());
        if (dto.getLastMaintenanceDate() != null) equipment.setLastMaintenanceDate(dto.getLastMaintenanceDate());
        if (dto.getImageUrl() != null) equipment.setImageUrl(dto.getImageUrl());
        if (dto.getQuantity() != null) equipment.setQuantity(dto.getQuantity());
    }
}
