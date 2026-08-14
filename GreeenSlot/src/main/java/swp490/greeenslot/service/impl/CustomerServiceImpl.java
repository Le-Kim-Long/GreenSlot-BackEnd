package swp490.greeenslot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp490.greeenslot.dto.AvailableSlotDTO;
import swp490.greeenslot.dto.GardeningTaskResponseDTO;
import swp490.greeenslot.dto.RentalHistoryDTO;
import swp490.greeenslot.dto.SensorReadingResponseDTO;
import swp490.greeenslot.dto.ServiceCategoryDTO;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.EquipmentRepository;
import swp490.greeenslot.repository.GardenSlotRepository;
import swp490.greeenslot.repository.GardeningTaskRepository;
import swp490.greeenslot.repository.SensorReadingRepository;
import swp490.greeenslot.repository.ServiceCategoryRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.CustomerService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final GardenSlotRepository gardenSlotRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final EquipmentRepository equipmentRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final GardeningTaskRepository gardeningTaskRepository;
    private final SlotRentalRepository slotRentalRepository;
    private final UserRepository userRepository;

    @Override
    public List<AvailableSlotDTO> getAvailableSlots() {
        List<GardenSlot> slots = gardenSlotRepository.findByPillarLocationIdAndStatus(null, ESlotStatus.AVAILABLE);
        return slots.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AvailableSlotDTO> getAvailableSlotsByLocation(Long locationId) {
        List<GardenSlot> slots = gardenSlotRepository.findByStatusAndLocationId(ESlotStatus.AVAILABLE, locationId);
        return slots.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AvailableSlotDTO> getAvailableSlotsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        List<GardenSlot> slots = gardenSlotRepository.findByStatusAndPriceRange(ESlotStatus.AVAILABLE, minPrice, maxPrice);
        return slots.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AvailableSlotDTO> getAvailableSlotsByLocationAndPrice(Long locationId, BigDecimal minPrice, BigDecimal maxPrice) {
        List<GardenSlot> slots = gardenSlotRepository.findByStatusAndLocationIdAndPriceRange(ESlotStatus.AVAILABLE, locationId, minPrice, maxPrice);
        return slots.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public AvailableSlotDTO getSlotDetails(Long slotId) {
        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found with ID: " + slotId));
        return mapToDTO(slot);
    }

    @Override
    public List<SensorReadingResponseDTO> getSlotIoTHistory(Long slotId, String sensorType, int limit) {
        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found with ID: " + slotId));

        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_LOCATION_MANAGER"));

        if (!isAdmin) {
            boolean hasAccess = slotRentalRepository.findByUserUsernameWithSlotAndPillarAndLocation(currentUsername)
                    .stream()
                    .anyMatch(r -> r.getGardenSlot() != null && r.getGardenSlot().getId().equals(slotId));
            if (!hasAccess) {
                throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access IoT data for this slot.");
            }
        }

        Pillar pillar = slot.getPillar();
        if (pillar == null) {
            throw new IllegalArgumentException("Slot is not associated with a pillar");
        }

        List<Equipment> equipmentList = equipmentRepository.findByPillar(pillar);
        if (equipmentList.isEmpty()) {
            throw new IllegalArgumentException("No equipment found for this pillar");
        }

        String deviceId = equipmentList.get(0).getSerialNumber();
        ESensorType sensorTypeEnum = sensorType != null ? ESensorType.fromCode(sensorType) : null;

        return sensorReadingRepository.findByDeviceIdAndSensorTypeOrderByRecordedAtDesc(deviceId, sensorTypeEnum,
                org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(this::mapToSensorDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceCategoryDTO> getServiceCategories() {
        return serviceCategoryRepository.findAll().stream()
                .map(this::mapToServiceCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<GardeningTaskResponseDTO> getMyServiceRequests(String username) {
        List<GardeningTask> tasks = gardeningTaskRepository.findByRequestedBy_UsernameOrderByCreatedAtDesc(username);
        return tasks.stream()
                .map(this::mapToGardeningTaskDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RentalHistoryDTO> getMyRentalHistory(String username) {
        List<SlotRental> rentals = slotRentalRepository.findByUserUsernameWithSlotAndPillarAndLocation(username);
        return rentals.stream()
                .map(this::mapToRentalHistoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RentalHistoryDTO getActiveRental(String username) {
        List<SlotRental> rentals = slotRentalRepository.findByUserUsernameWithSlotAndPillarAndLocation(username);
        LocalDateTime now = LocalDateTime.now();
        return rentals.stream()
                .filter(r -> r.getStatus() == ERentalStatus.ACTIVE && r.getEndTime().isAfter(now))
                .findFirst()
                .map(this::mapToRentalHistoryDTO)
                .orElse(null);
    }

    private AvailableSlotDTO mapToDTO(GardenSlot slot) {
        Pillar pillar = slot.getPillar();
        Location location = pillar != null ? pillar.getLocation() : null;

        // Get default tree for this pillar
        Tree tree = pillar != null ? pillar.getDefaultTree() : null;

        // Get equipment/device for this pillar
        List<Equipment> equipmentList = pillar != null ? equipmentRepository.findByPillar(pillar) : List.of();
        String deviceId = !equipmentList.isEmpty() ? equipmentList.get(0).getSerialNumber() : (pillar != null ? pillar.getPillarCode() : null);

        // Get latest sensor readings
        Double currentSoilMoisture = null;
        Double currentPh = null;
        Double currentLightIntensity = null;

        if (deviceId != null) {
            currentSoilMoisture = sensorReadingRepository.findFirstByDeviceIdAndSensorTypeOrderByRecordedAtDesc(deviceId, ESensorType.SOIL_MOISTURE)
                    .map(SensorReading::getValue).orElse(null);
            currentPh = sensorReadingRepository.findFirstByDeviceIdAndSensorTypeOrderByRecordedAtDesc(deviceId, ESensorType.PH)
                    .map(SensorReading::getValue).orElse(null);
            currentLightIntensity = sensorReadingRepository.findFirstByDeviceIdAndSensorTypeOrderByRecordedAtDesc(deviceId, ESensorType.LIGHT_INTENSITY)
                    .map(SensorReading::getValue).orElse(null);
        }

        return AvailableSlotDTO.builder()
                .id(slot.getId())
                .slotNumber(slot.getSlotNumber())
                .status(slot.getStatus() != null ? slot.getStatus().name() : null)
                .price(slot.getPrice())
                .imageUrl(slot.getImageUrl())
                .pillarId(pillar != null ? pillar.getId() : null)
                .pillarCode(pillar != null ? pillar.getPillarCode() : null)
                .locationId(location != null ? location.getId() : null)
                .locationName(location != null ? location.getName() : null)
                .locationAddress(location != null ? location.getAddress() : null)
                .locationImageUrl(location != null ? location.getImageUrl() : null)
                .treeName(tree != null ? tree.getTreeName() : null)
                .treeDescription(tree != null ? tree.getDescription() : null)
                .lastWatered(null)
                .lastFertilized(null)
                .currentSoilMoisture(currentSoilMoisture)
                .currentPh(currentPh)
                .currentLightIntensity(currentLightIntensity)
                .deviceStatus(deviceId != null ? "ONLINE" : "OFFLINE")
                .build();
    }

    private SensorReadingResponseDTO mapToSensorDTO(SensorReading reading) {
        return SensorReadingResponseDTO.fromEntity(reading);
    }

    private ServiceCategoryDTO mapToServiceCategoryDTO(ServiceCategory category) {
        return new ServiceCategoryDTO(category.getId(), category.getCategoryName(), category.getDescription());
    }

    private GardeningTaskResponseDTO mapToGardeningTaskDTO(GardeningTask task) {
        return new GardeningTaskResponseDTO(
                task.getId(),
                task.getTaskName(),
                task.getDescription(),
                task.getStatus().name(),
                task.getEvidenceImageUrl(),
                task.getTaskType().name(),
                task.getAssignedStaff() != null ? task.getAssignedStaff().getId() : null,
                task.getAssignedStaff() != null ? task.getAssignedStaff().getFullName() : null,
                task.getTargetSlot() != null ? task.getTargetSlot().getId() : null,
                task.getTargetSlot() != null ? task.getTargetSlot().getSlotNumber() : null,
                task.getCreatedAt(),
                task.getRejectionReason()
        );
    }

    private RentalHistoryDTO mapToRentalHistoryDTO(SlotRental rental) {
        GardenSlot slot = rental.getGardenSlot();
        Pillar pillar = slot != null ? slot.getPillar() : null;
        Location location = pillar != null ? pillar.getLocation() : null;

        return new RentalHistoryDTO(
                rental.getId(),
                slot != null ? slot.getId() : null,
                slot != null ? slot.getSlotNumber() : null,
                pillar != null ? pillar.getPillarCode() : null,
                location != null ? location.getName() : null,
                location != null ? location.getAddress() : null,
                rental.getStartTime(),
                rental.getEndTime(),
                rental.getStatus() != null ? rental.getStatus().name() : null,
                null // transactions can be added later if needed
        );
    }

    @Override
    public void deactivateAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
