package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.SensorReadingService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller handling IoT operations, telemetry, thresholds, and camera livestreams.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/iot")
@Tag(name = "IoT Sensors & Devices", description = "Endpoints for receiving sensor telemetry, threshold boundaries, and camera streams")
public class IoTSensorController {

    @Autowired
    private SensorReadingService sensorReadingService;

    @Autowired
    private SensorThresholdRepository sensorThresholdRepository;

    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private UserRepository userRepository;

    // --- TASK 3: TELEMETRY INGESTION ---

    @PostMapping("/sensors/data")
    @Operation(summary = "Arduino gui du lieu cam bien",
            description = "Gui mang readings de ho tro nhieu cam bien. THEM_CAM_BIEN_MOI: them phan tu vao readings.")
    public ResponseEntity<ArduinoSensorDataResponseDTO> receiveSensorData(
            @RequestHeader("X-IoT-Api-Key") String apiKey,
            @Valid @RequestBody ArduinoSensorDataRequestDTO request) {
        return ResponseEntity.ok(sensorReadingService.saveArduinoData(apiKey, request));
    }

    @PostMapping("/device/data")
    @Operation(summary = "PlatformIO ESP32 gui du lieu cam bien",
            description = "Nhan du lieu telemetry rieng le va tu dong danh gia nguong can canh bao.")
    public ResponseEntity<ArduinoSensorDataResponseDTO> receiveDeviceTelemetry(
            @RequestHeader("X-IoT-Api-Key") String apiKey,
            @Valid @RequestBody DeviceTelemetryRequestDTO request) {
        return ResponseEntity.ok(sensorReadingService.saveDeviceTelemetry(apiKey, request));
    }

    // --- EXISTING SENSOR READINGS READ APIS ---

    @GetMapping("/sensors/latest")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Gia tri moi nhat tung loai cam bien theo device")
    public ResponseEntity<List<SensorReadingResponseDTO>> getLatest(
            @Parameter(example = "arduino-greenhouse-01") @RequestParam String deviceId) {
        return ResponseEntity.ok(sensorReadingService.getLatestReadings(deviceId));
    }

    @GetMapping("/sensors/history")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Lich su doc cam bien")
    public ResponseEntity<List<SensorReadingResponseDTO>> getHistory(
            @RequestParam String deviceId,
            @RequestParam(required = false) ESensorType sensorType,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(sensorReadingService.getHistory(deviceId, sensorType, limit));
    }

    @GetMapping("/sensors/types")
    @Operation(summary = "Danh sach loai cam bien dang ho tro")
    public ResponseEntity<List<SensorTypeInfoDTO>> getSupportedSensorTypes() {
        List<SensorTypeInfoDTO> types = java.util.Arrays.stream(ESensorType.values())
                .map(SensorTypeInfoDTO::from)
                .toList();
        return ResponseEntity.ok(types);
    }

    // --- TASK 1: SENSOR THRESHOLD CRUD ---

    @PostMapping("/sensors/thresholds")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create sensor threshold boundary",
            description = "Allows location managers or administrators to define min/max threshold values for a sensor type on a specific device.")
    public ResponseEntity<SensorThreshold> createThreshold(@Valid @RequestBody SensorThreshold threshold) {
        // Validate sensor type
        try {
            ESensorType.fromCode(threshold.getSensorType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported sensor type: " + threshold.getSensorType());
        }

        // Check if threshold already exists for this device & sensor type
        Optional<SensorThreshold> existing = sensorThresholdRepository.findByDeviceIdAndSensorType(
                threshold.getDeviceId(), threshold.getSensorType()
        );
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Threshold already configured for device " + threshold.getDeviceId() + " and sensor type " + threshold.getSensorType());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(sensorThresholdRepository.save(threshold));
    }

    @GetMapping("/sensors/thresholds")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get all sensor thresholds", description = "Retrieves all sensor threshold boundaries.")
    public ResponseEntity<List<SensorThreshold>> getAllThresholds() {
        return ResponseEntity.ok(sensorThresholdRepository.findAll());
    }

    @GetMapping("/sensors/thresholds/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get sensor threshold details", description = "Retrieves details of a specific threshold.")
    public ResponseEntity<SensorThreshold> getThresholdById(@PathVariable Long id) {
        SensorThreshold threshold = sensorThresholdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sensor threshold not found with ID: " + id));
        return ResponseEntity.ok(threshold);
    }

    @PutMapping("/sensors/thresholds/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update sensor threshold boundaries", description = "Updates min/max boundaries for a threshold.")
    public ResponseEntity<SensorThreshold> updateThreshold(
            @PathVariable Long id,
            @Valid @RequestBody SensorThreshold thresholdDetails) {
        SensorThreshold threshold = sensorThresholdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sensor threshold not found with ID: " + id));

        threshold.setMinValue(thresholdDetails.getMinValue());
        threshold.setMaxValue(thresholdDetails.getMaxValue());

        return ResponseEntity.ok(sensorThresholdRepository.save(threshold));
    }

    @DeleteMapping("/sensors/thresholds/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Delete sensor threshold boundary", description = "Deletes a threshold boundary configuration.")
    public ResponseEntity<Map<String, String>> deleteThreshold(@PathVariable Long id) {
        SensorThreshold threshold = sensorThresholdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sensor threshold not found with ID: " + id));

        sensorThresholdRepository.delete(threshold);
        return ResponseEntity.ok(Map.of("message", "Sensor threshold deleted successfully."));
    }

    // --- TASK 4: CAMERA STREAM API ---

    @GetMapping("/camera/{slotId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get camera livestream URL",
            description = "Looks up the slot, gets its parent Pillar, and returns the livestream URL for the authenticated customer if they have an active rental on it, or if they are staff/manager/admin.")
    public ResponseEntity<Map<String, String>> getCameraStream(@PathVariable Long slotId, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID: " + slotId));

        Pillar pillar = slot.getPillar();
        if (pillar == null) {
            throw new IllegalArgumentException("This garden slot is not currently associated with a Pillar.");
        }

        // Security check: Check if user has staff, manager, or admin roles. If not, they MUST have an active rental on this slot.
        boolean isStaffOrManagerOrAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_ADMIN 
                        || r.getName() == ERole.ROLE_MANAGER 
                        || r.getName() == ERole.ROLE_LOCATION_MANAGER 
                        || r.getName() == ERole.ROLE_GARDEN_STAFF);

        if (!isStaffOrManagerOrAdmin) {
            // Verify active rental for the customer
            slotRentalRepository.findActiveRentalBySlotAndUser(slotId, username, LocalDateTime.now())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: You do not have an active rental on this slot."));
        }

        String streamUrl = pillar.getCameraStreamUrl();
        if (streamUrl == null || streamUrl.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "slotNumber", slot.getSlotNumber(),
                    "pillarCode", pillar.getPillarCode(),
                    "cameraStreamUrl", "No camera livestream configured for this pillar."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "slotNumber", slot.getSlotNumber(),
                "pillarCode", pillar.getPillarCode(),
                "cameraStreamUrl", streamUrl
        ));
    }
}
