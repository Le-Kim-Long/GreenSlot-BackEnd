package swp490.greeenslot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.CustomerService;
import swp490.greeenslot.service.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final NotificationService notificationService;

    @Override
    public List<AvailableSlotDTO> getAvailableSlots() {
        List<GardenSlot> slots = gardenSlotRepository.findAll().stream()
                .filter(g -> g.getStatus() == ESlotStatus.AVAILABLE)
                .collect(Collectors.toList());
        return slots.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AvailableSlotDTO> getAvailableSlotsByLocation(Long locationId) {
        List<GardenSlot> slots = (locationId == null) 
                ? gardenSlotRepository.findAll().stream().filter(g -> g.getStatus() == ESlotStatus.AVAILABLE).collect(Collectors.toList())
                : gardenSlotRepository.findByLocationIdAndStatus(locationId, ESlotStatus.AVAILABLE);
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

        String deviceId = pillar.getPillarCode();
        if (deviceId == null || deviceId.isBlank()) {
            List<Equipment> equipmentList = equipmentRepository.findByPillar(pillar);
            if (!equipmentList.isEmpty()) {
                deviceId = equipmentList.get(0).getSerialNumber();
            } else {
                throw new IllegalArgumentException("No device or equipment configured for this pillar");
            }
        }
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
        Location location = slot.getLocation() != null ? slot.getLocation() : (pillar != null ? pillar.getLocation() : null);

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
                .area(slot.getArea())
                .maxPillars(slot.getMaxPillars() != null ? slot.getMaxPillars() : slot.calculateMaxPillars())
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
        Location location = (slot != null && slot.getLocation() != null) ? slot.getLocation() : (slot != null && slot.getPillar() != null ? slot.getPillar().getLocation() : null);

        List<Pillar> slotPillars = slot != null ? slot.getPillars() : null;
        List<String> pillarCodes = new ArrayList<>();
        List<RentalHistoryDTO.PillarInfo> pillarInfos = new ArrayList<>();
        if (slotPillars != null && !slotPillars.isEmpty()) {
            for (Pillar p : slotPillars) {
                pillarCodes.add(p.getPillarCode());
                pillarInfos.add(new RentalHistoryDTO.PillarInfo(
                        p.getId(),
                        p.getPillarCode(),
                        p.getStatus() != null ? p.getStatus().name() : "ACTIVE",
                        p.getCameraStreamUrl(),
                        p.getCameraStatus()
                ));
            }
        } else if (slot != null && slot.getPillar() != null) {
            pillarCodes.add(slot.getPillar().getPillarCode());
            pillarInfos.add(new RentalHistoryDTO.PillarInfo(
                    slot.getPillar().getId(),
                    slot.getPillar().getPillarCode(),
                    slot.getPillar().getStatus() != null ? slot.getPillar().getStatus().name() : "ACTIVE",
                    slot.getPillar().getCameraStreamUrl(),
                    slot.getPillar().getCameraStatus()
            ));
        }
        String primaryPillarCode = !pillarCodes.isEmpty() ? String.join(", ", pillarCodes) : (slot != null && slot.getPillar() != null ? slot.getPillar().getPillarCode() : "N/A");

        Integer harvestDays = rental.getTree() != null ? rental.getTree().getHarvestDays() : null;
        java.time.LocalDateTime expectedHarvestAt = (rental.getPlantedAt() != null && harvestDays != null && harvestDays > 0)
                ? rental.getPlantedAt().plusDays(harvestDays)
                : null;

        RentalHistoryDTO dto = new RentalHistoryDTO(
                rental.getId(),
                slot != null ? slot.getId() : null,
                slot != null ? slot.getSlotNumber() : null,
                primaryPillarCode,
                location != null ? location.getName() : null,
                location != null ? location.getAddress() : null,
                rental.getStartTime(),
                rental.getEndTime(),
                rental.getStatus() != null ? rental.getStatus().name() : null,
                null, // transactions can be added later if needed
                rental.getTree() != null ? rental.getTree().getTreeName() : null,
                rental.getHarvestNotifiedAt(),
                rental.getHarvestDecision(),
                rental.getPlantedAt(),
                expectedHarvestAt
        );
        dto.setPillars(pillarInfos);
        dto.setPillarCodes(pillarCodes);
        return dto;
    }

    @Override
    public void deactivateAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void recordHarvestDecision(Long rentalId, HarvestDecisionRequestDTO request, String username) {
        if (request == null || request.getDecision() == null) {
            throw new IllegalArgumentException("Harvest decision is required (SELF or STAFF).");
        }

        String decision = request.getDecision().trim().toUpperCase();
        if (!"SELF".equals(decision) && !"STAFF".equals(decision)) {
            throw new IllegalArgumentException("Decision must be either SELF or STAFF.");
        }

        SlotRental rental = slotRentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found with id: " + rentalId));

        if (rental.getUser() == null || !rental.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized: You do not own this rental contract.");
        }

        rental.setHarvestDecision(decision);

        if ("SELF".equals(decision)) {
            // Khách tự thu hoạch -> reset cây trên ô đất
            resetHarvestedTree(rental);
        }
        slotRentalRepository.save(rental);

        if (rental.getGardenSlot() != null) {
            List<GardeningTask> harvestTasks = gardeningTaskRepository
                    .findByTargetSlotIdAndTaskTypeOrderByCreatedAtDesc(rental.getGardenSlot().getId(), ETaskType.HARVEST);
            GardeningTask task = harvestTasks.stream()
                    .filter(t -> t.getStatus() != ETaskStatus.COMPLETED && t.getStatus() != ETaskStatus.CANCELLED)
                    .findFirst()
                    .orElse(null);

            if (task != null) {
                if ("SELF".equals(decision)) {
                    task.setStatus(ETaskStatus.CANCELLED);
                    gardeningTaskRepository.save(task);
                }
            }

            // Notify location managers and staff about customer's harvest decision
            if (notificationService != null) {
                String customerName = rental.getUser().getFullName() != null ? rental.getUser().getFullName() : username;
                String slotNumber = rental.getGardenSlot().getSlotNumber();
                String decisionText = "SELF".equals(decision) ? "Tự thu hoạch" : "Nhân viên hỗ trợ thu hoạch";
                String title = "Khách hàng đã chọn phương thức thu hoạch";
                String message = String.format("Khách hàng %s tại ô %s đã chọn phương thức thu hoạch: %s.",
                        customerName, slotNumber, decisionText);

                Long locationId = (rental.getGardenSlot().getLocation() != null)
                        ? rental.getGardenSlot().getLocation().getId()
                        : (rental.getGardenSlot().getPillar() != null && rental.getGardenSlot().getPillar().getLocation() != null
                            ? rental.getGardenSlot().getPillar().getLocation().getId() : null);

                List<User> recipients = new ArrayList<>();
                if (locationId != null) {
                    recipients.addAll(userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, locationId));
                    recipients.addAll(userRepository.findByRoleNameAndLocation(ERole.ROLE_GARDEN_STAFF, locationId));
                }
                if (recipients.isEmpty()) {
                    recipients.addAll(userRepository.findByRoleName(ERole.ROLE_MANAGER));
                    recipients.addAll(userRepository.findByRoleName(ERole.ROLE_GARDEN_STAFF));
                }

                for (User recipient : recipients) {
                    notificationService.createNotification(
                            recipient.getId(),
                            title,
                            message,
                            "HARVEST_DECISION_RECEIVED",
                            rental.getId(),
                            "/dashboard/manager/tasks"
                    );
                }
            }
        }
    }

    private void resetHarvestedTree(SlotRental rental) {
        rental.setTree(null);
        rental.setTreeStatus(null);
        rental.setTreeNotes(null);
        rental.setPlantedAt(null);
        rental.setHarvestReminderSent(false);
        rental.setHarvestNotifiedAt(null);
        rental.setHarvestDecision(null);
    }
}
