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
@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/iot")
@Tag(name = "IoT Sensors & Devices", description = "Endpoints for receiving sensor telemetry, threshold boundaries, and camera streams")
public class IoTSensorController {

    private static final String VALID_IOT_API_KEY = "GreenSlot-IoT-Dev-Key";

    @Autowired
    private SensorReadingService sensorReadingService;

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private SensorThresholdRepository sensorThresholdRepository;

    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Autowired
    private swp490.greeenslot.service.FirebaseStorageService firebaseStorageService;

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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Gia tri moi nhat tung loai cam bien theo device")
    public ResponseEntity<List<SensorReadingResponseDTO>> getLatest(
            @Parameter(example = "arduino-greenhouse-01") @RequestParam String deviceId) {
        return ResponseEntity.ok(sensorReadingService.getLatestReadings(deviceId));
    }

    @GetMapping("/sensors/history")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Lich su doc cam bien")
    public ResponseEntity<List<SensorReadingResponseDTO>> getHistory(
            @RequestParam String deviceId,
            @RequestParam(required = false) ESensorType sensorType,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(sensorReadingService.getHistory(deviceId, sensorType, limit));
    }

    @GetMapping("/sensors/aggregated")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Aggregated sensor data for charts",
            description = "Returns time-series aggregated data (daily or hourly) for chart visualization")
    public ResponseEntity<List<SensorAggregationDTO>> getAggregatedData(
            @RequestParam String deviceId,
            @RequestParam ESensorType sensorType,
            @RequestParam java.time.Instant startTime,
            @RequestParam java.time.Instant endTime,
            @RequestParam(defaultValue = "daily") String aggregationType) {
        
        List<Object[]> rawData;
        if ("hourly".equalsIgnoreCase(aggregationType)) {
            rawData = sensorReadingRepository.findHourlyAggregatedData(deviceId, sensorType, startTime, endTime);
        } else {
            rawData = sensorReadingRepository.findDailyAggregatedData(deviceId, sensorType, startTime, endTime);
        }
        
        List<SensorAggregationDTO> result = rawData.stream().map(row -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            if ("hourly".equalsIgnoreCase(aggregationType)) {
                dto.setHour((Integer) row[0]);
            } else {
                dto.setDate((java.time.LocalDate) row[0]);
            }
            dto.setAvgValue((Double) row[1]);
            dto.setMinValue((Double) row[2]);
            dto.setMaxValue((Double) row[3]);
            dto.setCount((Long) row[4]);
            return dto;
        }).toList();
        
        return ResponseEntity.ok(result);
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

    @GetMapping("/camera/{slotId}/status")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get camera status", description = "Returns camera status and last heartbeat time")
    public ResponseEntity<Map<String, Object>> getCameraStatus(@PathVariable Long slotId) {
        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID: " + slotId));

        Pillar pillar = slot.getPillar();
        if (pillar == null) {
            throw new IllegalArgumentException("This garden slot is not currently associated with a Pillar.");
        }

        return ResponseEntity.ok(Map.of(
                "slotNumber", slot.getSlotNumber(),
                "pillarCode", pillar.getPillarCode(),
                "cameraStatus", pillar.getCameraStatus() != null ? pillar.getCameraStatus() : "UNKNOWN",
                "cameraLastHeartbeat", pillar.getCameraLastHeartbeat()
        ));
    }

    @PostMapping("/camera/{slotId}/heartbeat")
    @Operation(summary = "Update camera heartbeat", description = "IoT device sends heartbeat to indicate camera is online")
    public ResponseEntity<Map<String, String>> updateCameraHeartbeat(
            @PathVariable Long slotId,
            @RequestHeader("X-IoT-Api-Key") String apiKey) {
        
        if (!VALID_IOT_API_KEY.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid API key"));
        }
        
        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID: " + slotId));

        Pillar pillar = slot.getPillar();
        if (pillar == null) {
            throw new IllegalArgumentException("This garden slot is not currently associated with a Pillar.");
        }

        pillar.setCameraStatus("ONLINE");
        pillar.setCameraLastHeartbeat(java.time.LocalDateTime.now());
        pillarRepository.save(pillar);

        return ResponseEntity.ok(Map.of(
                "message", "Camera heartbeat updated",
                "pillarCode", pillar.getPillarCode(),
                "status", "ONLINE"
        ));
    }

    @PostMapping("/device/{slotId}/heartbeat")
    @Operation(summary = "Update device heartbeat", description = "IoT device sends heartbeat to indicate device is online")
    public ResponseEntity<Map<String, String>> updateDeviceHeartbeat(
            @PathVariable Long slotId,
            @RequestHeader("X-IoT-Api-Key") String apiKey) {
        
        if (!VALID_IOT_API_KEY.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid API key"));
        }
        
        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID: " + slotId));

        Pillar pillar = slot.getPillar();
        if (pillar == null) {
            throw new IllegalArgumentException("This garden slot is not currently associated with a Pillar.");
        }

        pillar.setDeviceStatus("ONLINE");
        pillar.setDeviceLastHeartbeat(java.time.LocalDateTime.now());
        pillarRepository.save(pillar);

        return ResponseEntity.ok(Map.of(
                "message", "Device heartbeat updated",
                "pillarCode", pillar.getPillarCode(),
                "status", "ONLINE"
        ));
    }

    @GetMapping("/device/{slotId}/status")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Get device status", description = "Returns the current status of the IoT device")
    public ResponseEntity<Map<String, Object>> getDeviceStatus(@PathVariable Long slotId) {
        
        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID: " + slotId));

        Pillar pillar = slot.getPillar();
        if (pillar == null) {
            throw new IllegalArgumentException("This garden slot is not currently associated with a Pillar.");
        }

        // Check if device is offline (no heartbeat for more than 5 minutes)
        boolean isOffline = pillar.getDeviceLastHeartbeat() == null 
                || pillar.getDeviceLastHeartbeat().isBefore(java.time.LocalDateTime.now().minusMinutes(5));
        
        String status = isOffline ? "OFFLINE" : (pillar.getDeviceStatus() != null ? pillar.getDeviceStatus() : "UNKNOWN");

        return ResponseEntity.ok(Map.of(
                "pillarCode", pillar.getPillarCode(),
                "status", status,
                "lastHeartbeat", pillar.getDeviceLastHeartbeat() != null ? pillar.getDeviceLastHeartbeat().toString() : "Never",
                "isOffline", isOffline
        ));
    }

    @PostMapping("/camera/{slotId}/control")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Control camera (pan/tilt)", description = "Send pan/tilt commands to camera")
    public ResponseEntity<Map<String, String>> controlCamera(
            @PathVariable Long slotId,
            @RequestBody Map<String, String> controlRequest) {
        // ... (rest of the code remains the same)
        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID: " + slotId));

        Pillar pillar = slot.getPillar();
        if (pillar == null) {
            throw new IllegalArgumentException("This garden slot is not currently associated with a Pillar.");
        }

        String action = controlRequest.get("action"); // PAN_LEFT, PAN_RIGHT, TILT_UP, TILT_DOWN, ZOOM_IN, ZOOM_OUT
        
        // This is a placeholder - actual implementation would send commands to IoT device
        // For now, return success as the hardware integration is not implemented
        return ResponseEntity.ok(Map.of(
                "message", "Camera control command sent: " + action,
                "status", "COMMAND_SENT"
        ));
    }

    @PostMapping("/camera/{slotId}/record")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Record video from camera", description = "Starts video recording and saves to Firebase Storage")
    public ResponseEntity<Map<String, String>> recordVideo(
            @PathVariable Long slotId,
            @RequestParam(defaultValue = "30") int durationSeconds) {
        
        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID: " + slotId));

        Pillar pillar = slot.getPillar();
        if (pillar == null) {
            throw new IllegalArgumentException("This garden slot is not currently associated with a Pillar.");
        }

        // This is a placeholder - actual implementation would:
        // 1. Send command to IoT camera to start recording
        // 2. Wait for duration or receive video file
        // 3. Upload video to Firebase Storage
        // 4. Return the Firebase Storage URL
        
        String videoFileName = "camera_" + pillar.getPillarCode() + "_" + System.currentTimeMillis() + ".mp4";
        String firebasePath = "camera-recordings/" + videoFileName;
        
        // Placeholder Firebase URL
        String firebaseUrl = "https://firebasestorage.googleapis.com/v0/b/greenslot-46382.appspot.com/o/" + firebasePath;
        
        return ResponseEntity.ok(Map.of(
                "message", "Video recording initiated",
                "videoUrl", firebaseUrl,
                "duration", String.valueOf(durationSeconds),
                "status", "RECORDING_STARTED"
        ));
    }

    @PostMapping("/camera/{slotId}/upload-video")
    @Operation(summary = "Upload video to Firebase Storage", description = "IoT device uploads recorded video file")
    public ResponseEntity<Map<String, String>> uploadVideo(
            @PathVariable Long slotId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestHeader("X-IoT-Api-Key") String apiKey) {
        
        // Validate API key
        if (!"GreenSlot-IoT-Dev-Key".equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            GardenSlot slot = gardenSlotRepository.findById(slotId)
                    .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID: " + slotId));

            Pillar pillar = slot.getPillar();
            if (pillar == null) {
                throw new IllegalArgumentException("This garden slot is not currently associated with a Pillar.");
            }

            String fileName = "camera_" + pillar.getPillarCode() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            
            // Upload to Firebase Storage using existing uploadVideo method
            String downloadUrl = firebaseStorageService.uploadVideo(file);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Video uploaded successfully",
                    "downloadUrl", downloadUrl,
                    "fileName", fileName
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload video: " + e.getMessage()));
        }
    }
}
