package swp490.greeenslot.service.impl;

import lombok.extern.slf4j.Slf4j;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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
    @Transactional
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
        if (targetLocationId != null && targetLocationId > 0) {
            locationContextService.validateLocationAccess(targetLocationId);
            location = locationRepository.findById(targetLocationId)
                    .orElseThrow(() -> new RuntimeException("Location not found with id: " + targetLocationId));
        }

        Pillar pillar = null;
        EEquipmentStatus status = dto.getStatus() != null ? EEquipmentStatus.valueOf(dto.getStatus().toUpperCase()) : EEquipmentStatus.AVAILABLE;
        if (dto.getPillarId() != null && dto.getPillarId() > 0 && status != EEquipmentStatus.MAINTENANCE && status != EEquipmentStatus.BROKEN) {
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
            status = EEquipmentStatus.IN_USE;
        } else {
            pillar = null;
        }

        Equipment equipment = mapToEntity(dto);
        equipment.setStatus(status);
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
        EEquipmentStatus newStatus = dto.getStatus() != null ? EEquipmentStatus.valueOf(dto.getStatus().toUpperCase()) : existingEquipment.getStatus();

        if (newStatus == EEquipmentStatus.AVAILABLE || newStatus == EEquipmentStatus.MAINTENANCE || newStatus == EEquipmentStatus.BROKEN) {
            // Thiết bị trong kho, bảo trì hoặc hỏng -> Bắt buộc ngắt kết nối với trụ
            newPillar = null;
        } else if (dto.getPillarId() != null && dto.getPillarId() > 0) {
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
            newStatus = EEquipmentStatus.IN_USE;
        }

        updateEntityFromDTO(existingEquipment, dto);
        existingEquipment.setStatus(newStatus);
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

        // Trường hợp 1: Thiết bị đang trong kho (chưa gán vào trụ: pillar == null)
        if (equipment.getPillar() == null) {
            int current = equipment.getQuantity() != null ? equipment.getQuantity() : 0;
            equipment.setQuantity(current + additionalQuantity);
            equipment.setStatus(EEquipmentStatus.AVAILABLE);
            Equipment saved = equipmentRepository.save(equipment);
            return mapToDTO(saved);
        }

        // Trường hợp 2: Thiết bị đã gắn vào trụ (pillar != null)
        // Thiết bị trên trụ giữ nguyên số lượng đang sử dụng, số lượng nhập thêm (+N) được đưa vào KHO sẵn sàng của cơ sở
        equipment.setStatus(EEquipmentStatus.IN_USE);
        equipmentRepository.save(equipment);

        Location targetLocation = equipment.getLocation() != null 
                ? equipment.getLocation() 
                : (equipment.getPillar() != null ? equipment.getPillar().getLocation() : null);

        Equipment warehouseEq = equipmentRepository.findByPillarIsNull().stream()
                .filter(w -> w.getEquipmentName().equalsIgnoreCase(equipment.getEquipmentName())
                        && (targetLocation == null || (w.getLocation() != null && w.getLocation().getId().equals(targetLocation.getId()))))
                .findFirst()
                .orElse(null);

        if (warehouseEq != null) {
            int currentWarehouse = warehouseEq.getQuantity() != null ? warehouseEq.getQuantity() : 0;
            warehouseEq.setQuantity(currentWarehouse + additionalQuantity);
            warehouseEq.setStatus(EEquipmentStatus.AVAILABLE);
            Equipment savedWarehouse = equipmentRepository.save(warehouseEq);
            return mapToDTO(savedWarehouse);
        } else {
            Equipment newWarehouseEq = new Equipment();
            newWarehouseEq.setEquipmentName(equipment.getEquipmentName());
            String prefix = equipment.getSerialNumber() != null && !equipment.getSerialNumber().isBlank()
                    ? equipment.getSerialNumber().replaceAll("-\\d+$", "")
                    : equipment.getEquipmentName().replaceAll("\\s+", "-").toUpperCase();
            newWarehouseEq.setSerialNumber(prefix + "-KHO-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            newWarehouseEq.setDescription(equipment.getDescription());
            newWarehouseEq.setStatus(EEquipmentStatus.AVAILABLE);
            newWarehouseEq.setPillar(null);
            newWarehouseEq.setLocation(targetLocation);
            newWarehouseEq.setQuantity(additionalQuantity);
            newWarehouseEq.setPurchaseDate(equipment.getPurchaseDate() != null ? equipment.getPurchaseDate() : LocalDateTime.now());
            newWarehouseEq.setLastMaintenanceDate(LocalDateTime.now());
            newWarehouseEq.setImageUrl(equipment.getImageUrl());
            Equipment savedNewWarehouse = equipmentRepository.save(newWarehouseEq);
            return mapToDTO(savedNewWarehouse);
        }
    }

    @Override
    @Transactional
    public List<EquipmentDTO> bindEquipmentsToPillar(Long pillarId, List<swp490.greeenslot.dto.PillarEquipmentBindingDTO> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 thiết bị để gắn vào trụ.");
        }
        Pillar pillar = pillarRepository.findById(pillarId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trụ ID: " + pillarId));
        Location pillarLocation = pillar.getLocation();
        if (pillarLocation != null) {
            locationContextService.validateLocationAccess(pillarLocation.getId());
        }

        List<Equipment> boundEquipments = new java.util.ArrayList<>();
        for (swp490.greeenslot.dto.PillarEquipmentBindingDTO binding : bindings) {
            int qty = (binding.getQuantity() != null && binding.getQuantity() > 0) ? binding.getQuantity() : 1;
            boolean fromStock = binding.getEquipmentId() != null && binding.getEquipmentId() > 0;
            boundEquipments.add(fromStock
                    ? takeFromStock(binding.getEquipmentId(), qty, pillar)
                    : declareNewDevice(binding, qty, pillar));
        }
        return boundEquipments.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // Lấy thiết bị từ kho: đủ số lượng thì chuyển nguyên lô, còn dư thì tách lô (giữ phần còn lại trong kho)
    private Equipment takeFromStock(Long equipmentId, int qty, Pillar pillar) {
        Equipment stockEq = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị ID: " + equipmentId));
        if (stockEq.getPillar() != null || stockEq.getStatus() != EEquipmentStatus.AVAILABLE) {
            throw new IllegalArgumentException("Thiết bị '" + stockEq.getEquipmentName() + "' không còn trong kho sẵn sàng.");
        }
        // Chỉ được lấy thiết bị của chính cơ sở trụ hoặc kho chung "Tất cả cơ sở" (không gắn cơ sở)
        Long stockLocId = stockEq.getLocation() != null ? stockEq.getLocation().getId() : null;
        Long pillarLocId = pillar.getLocation() != null ? pillar.getLocation().getId() : null;
        if (stockLocId != null && !stockLocId.equals(pillarLocId)) {
            throw new IllegalArgumentException("Thiết bị '" + stockEq.getEquipmentName()
                    + "' thuộc kho của cơ sở khác, không thể lắp vào trụ " + pillar.getPillarCode() + ".");
        }
        int stock = stockEq.getQuantity() != null ? stockEq.getQuantity() : 1;
        if (stock < qty) {
            throw new IllegalArgumentException(String.format(
                    "Thiết bị '%s' trong kho chỉ còn %d cái, không đủ %d cái.", stockEq.getEquipmentName(), stock, qty));
        }

        if (stock == qty) {
            stockEq.setPillar(pillar);
            stockEq.setStatus(EEquipmentStatus.IN_USE);
            if (pillar.getLocation() != null) stockEq.setLocation(pillar.getLocation());
            return equipmentRepository.save(stockEq);
        }

        stockEq.setQuantity(stock - qty);
        equipmentRepository.save(stockEq);

        String baseSerial = stockEq.getSerialNumber() != null ? stockEq.getSerialNumber() : "EQ";
        String deployedSerial = baseSerial + "-" + pillar.getPillarCode();
        if (equipmentRepository.findBySerialNumber(deployedSerial).isPresent()) {
            deployedSerial = deployedSerial + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }
        Equipment deployedEq = new Equipment();
        deployedEq.setEquipmentName(stockEq.getEquipmentName());
        deployedEq.setSerialNumber(deployedSerial);
        deployedEq.setDescription(stockEq.getDescription());
        deployedEq.setStatus(EEquipmentStatus.IN_USE);
        deployedEq.setPillar(pillar);
        deployedEq.setLocation(pillar.getLocation() != null ? pillar.getLocation() : stockEq.getLocation());
        deployedEq.setQuantity(qty);
        deployedEq.setPurchaseDate(stockEq.getPurchaseDate());
        deployedEq.setLastMaintenanceDate(LocalDateTime.now());
        deployedEq.setImageUrl(stockEq.getImageUrl());
        return equipmentRepository.save(deployedEq);
    }

    // Khai báo thiết bị mới (kho chưa có) và gắn thẳng vào trụ
    private Equipment declareNewDevice(swp490.greeenslot.dto.PillarEquipmentBindingDTO binding, int qty, Pillar pillar) {
        String serial = binding.getNewSerialNumber() != null ? binding.getNewSerialNumber().trim().toUpperCase() : "";
        if (serial.isEmpty()) {
            throw new IllegalArgumentException("Thiết bị mới bắt buộc phải có Mã Serial.");
        }
        if (equipmentRepository.findBySerialNumber(serial).isPresent()) {
            throw new IllegalArgumentException("Mã Serial '" + serial + "' đã tồn tại. Hãy chọn thiết bị từ kho hoặc dùng mã khác.");
        }
        String name = binding.getNewEquipmentName() != null && !binding.getNewEquipmentName().isBlank()
                ? binding.getNewEquipmentName().trim()
                : "Mạch điều khiển ESP32";
        Equipment eq = new Equipment();
        eq.setEquipmentName(name);
        eq.setSerialNumber(serial);
        eq.setStatus(EEquipmentStatus.IN_USE);
        eq.setPillar(pillar);
        eq.setLocation(pillar.getLocation());
        eq.setQuantity(qty);
        eq.setPurchaseDate(LocalDateTime.now());
        return equipmentRepository.save(eq);
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
        if (equipment.getStatus() == EEquipmentStatus.IN_USE) {
            dto.setPillarId(equipment.getPillar() != null ? equipment.getPillar().getId() : null);
            dto.setPillarCode(equipment.getPillar() != null ? equipment.getPillar().getPillarCode() : null);
        } else {
            dto.setPillarId(null);
            dto.setPillarCode(null);
        }
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
