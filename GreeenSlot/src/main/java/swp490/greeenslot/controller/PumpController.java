package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.AssignedSlotPumpsDTO;
import swp490.greeenslot.dto.PumpStatusDTO;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.entity.StaffSchedule;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.GardenSlotRepository;
import swp490.greeenslot.repository.GardeningTaskRepository;
import swp490.greeenslot.repository.PillarRepository;
import swp490.greeenslot.repository.StaffScheduleRepository;
import swp490.greeenslot.service.LocationContextService;
import swp490.greeenslot.service.PumpService;

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/iot/pump")
@Tag(name = "Pump Control API", description = "API điều khiển máy bơm tự động và thủ công")
public class PumpController {

    private final PumpService pumpService;
    private final LocationContextService locationContextService;
    private final StaffScheduleRepository staffScheduleRepository;
    private final GardeningTaskRepository gardeningTaskRepository;
    private final GardenSlotRepository gardenSlotRepository;
    private final PillarRepository pillarRepository;

    // Dependency Injection
    public PumpController(
            PumpService pumpService,
            LocationContextService locationContextService,
            StaffScheduleRepository staffScheduleRepository,
            GardeningTaskRepository gardeningTaskRepository,
            GardenSlotRepository gardenSlotRepository,
            PillarRepository pillarRepository
    ) {
        this.pumpService = pumpService;
        this.locationContextService = locationContextService;
        this.staffScheduleRepository = staffScheduleRepository;
        this.gardeningTaskRepository = gardeningTaskRepository;
        this.gardenSlotRepository = gardenSlotRepository;
        this.pillarRepository = pillarRepository;
    }

    @GetMapping("/status")
    @Operation(summary = "Lấy trạng thái máy bơm", description = "API này được ESP32 / Arduino / Python Bridge và Frontend gọi để đồng bộ với mạch.")
    public ResponseEntity<PumpStatusDTO> getPumpStatus() {
        return ResponseEntity.ok(pumpService.getFullStatus());
    }

    @PostMapping("/status")
    @Operation(summary = "Bật/Tắt máy bơm hoặc cập nhật chế độ tự động", description = "Truyền vào ON hoặc OFF và cờ autoMode để điều khiển máy bơm.")
    public ResponseEntity<PumpStatusDTO> updatePumpStatus(@RequestBody PumpStatusDTO requestDto) {
        if (requestDto.getAutoMode() != null) {
            pumpService.setAutoMode(requestDto.getAutoMode());
        }
        if (requestDto.getStatus() != null && !requestDto.getStatus().isBlank()) {
            pumpService.setPumpStatus(requestDto.getStatus());
        }
        return ResponseEntity.ok(pumpService.getFullStatus());
    }

    @PutMapping("/auto-mode")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Bật/Tắt chế độ tự động tưới nước khi độ ẩm thấp")
    public ResponseEntity<PumpStatusDTO> setAutoMode(@RequestParam boolean enabled) {
        pumpService.setAutoMode(enabled);
        return ResponseEntity.ok(pumpService.getFullStatus());
    }

    @GetMapping("/my-assigned-pumps")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Lấy danh sách máy bơm các trụ thuộc ô vườn mà nhân viên được phân công")
    public ResponseEntity<List<AssignedSlotPumpsDTO>> getMyAssignedPumps() {
        User currentUser = locationContextService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Map<Long, GardenSlot> slotMap = new LinkedHashMap<>();

        if (locationContextService.isGlobalManagerOrAdmin()) {
            List<GardenSlot> allSlots = gardenSlotRepository.findAll();
            for (GardenSlot slot : allSlots) {
                slotMap.put(slot.getId(), slot);
            }
        } else if (locationContextService.isLocationManager()) {
            Long locId = locationContextService.getCurrentUserLocationId();
            if (locId != null) {
                List<GardenSlot> locSlots = gardenSlotRepository.findByLocationId(locId);
                for (GardenSlot slot : locSlots) {
                    slotMap.put(slot.getId(), slot);
                }
            }
        } else {
            // Garden Staff: Lấy từ StaffSchedule và GardeningTask
            staffScheduleRepository.findByStaff(currentUser).stream()
                    .map(StaffSchedule::getGardenSlot)
                    .filter(Objects::nonNull)
                    .forEach(slot -> slotMap.put(slot.getId(), slot));

            gardeningTaskRepository.findByAssignedStaffId(currentUser.getId()).stream()
                    .map(swp490.greeenslot.entity.GardeningTask::getTargetSlot)
                    .filter(Objects::nonNull)
                    .forEach(slot -> slotMap.put(slot.getId(), slot));
        }

        List<AssignedSlotPumpsDTO> result = new ArrayList<>();

        for (GardenSlot slot : slotMap.values()) {
            List<Pillar> pillars = pillarRepository.findByGardenSlotId(slot.getId());
            List<AssignedSlotPumpsDTO.PillarPumpInfoDTO> pillarDtos = pillars.stream().map(p -> {
                PumpStatusDTO statusDto = pumpService.getPillarPumpStatus(p.getId());
                return AssignedSlotPumpsDTO.PillarPumpInfoDTO.builder()
                        .pillarId(p.getId())
                        .pillarCode(p.getPillarCode())
                        .capacityHoles(p.getCapacityHoles())
                        .pillarType(p.getEffectivePillarType() != null ? p.getEffectivePillarType().name() : "MEDIUM")
                        .pillarTypeName(p.getEffectivePillarType() != null ? p.getEffectivePillarType().getDisplayName() : "Trụ Vừa")
                        .treeName(p.getDefaultTree() != null ? p.getDefaultTree().getTreeName() : null)
                        .pumpStatus(statusDto.getStatus())
                        .autoMode(statusDto.getAutoMode())
                        .lastTriggerReason(statusDto.getLastTriggerReason())
                        .lastTriggerTime(statusDto.getLastTriggerTime())
                        .build();
            }).collect(Collectors.toList());

            result.add(AssignedSlotPumpsDTO.builder()
                    .slotId(slot.getId())
                    .slotNumber(slot.getSlotNumber())
                    .area(slot.getArea())
                    .locationId(slot.getLocation() != null ? slot.getLocation().getId() : null)
                    .locationName(slot.getLocation() != null ? slot.getLocation().getName() : null)
                    .pillars(pillarDtos)
                    .build());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/pillars/{pillarId}/status")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Bật/Tắt máy bơm của một trụ cụ thể (Bật hẹn giờ 5s tự ngắt)")
    public ResponseEntity<PumpStatusDTO> updatePillarPumpStatus(
            @PathVariable Long pillarId,
            @RequestBody PumpStatusDTO requestDto
    ) {
        String status = requestDto.getStatus();
        int autoOffSeconds = (status != null && status.equalsIgnoreCase("ON")) ? 5 : 0;
        PumpStatusDTO updated = pumpService.setPillarPumpStatus(pillarId, status, autoOffSeconds);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/pillars/{pillarId}/auto-mode")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Bật/Tắt chế độ tự động cho một trụ cụ thể")
    public ResponseEntity<PumpStatusDTO> setPillarAutoMode(
            @PathVariable Long pillarId,
            @RequestParam boolean enabled
    ) {
        PumpStatusDTO updated = pumpService.setPillarAutoMode(pillarId, enabled);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/slots/{slotId}/trigger-all")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Kích hoạt tưới toàn bộ trụ trong một ô vườn (chạy 5s tự ngắt)")
    public ResponseEntity<Map<String, Object>> triggerAllPumpsInSlot(@PathVariable Long slotId) {
        List<Pillar> pillars = pillarRepository.findByGardenSlotId(slotId);
        for (Pillar p : pillars) {
            pumpService.setPillarPumpStatus(p.getId(), "ON", 5);
        }
        return ResponseEntity.ok(Map.of(
                "message", "Đã kích hoạt tưới 5s cho toàn bộ " + pillars.size() + " trụ trong ô vườn",
                "count", pillars.size()
        ));
    }

    @PostMapping("/slots/{slotId}/turn-off-all")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Tắt toàn bộ máy bơm trong một ô vườn")
    public ResponseEntity<Map<String, Object>> turnOffAllPumpsInSlot(@PathVariable Long slotId) {
        List<Pillar> pillars = pillarRepository.findByGardenSlotId(slotId);
        for (Pillar p : pillars) {
            pumpService.setPillarPumpStatus(p.getId(), "OFF", 0);
        }
        return ResponseEntity.ok(Map.of(
                "message", "Đã tắt toàn bộ máy bơm trong ô vườn",
                "count", pillars.size()
        ));
    }
}