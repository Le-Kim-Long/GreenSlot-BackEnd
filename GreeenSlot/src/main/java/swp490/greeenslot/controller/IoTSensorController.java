package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.ArduinoSensorDataRequestDTO;
import swp490.greeenslot.dto.ArduinoSensorDataResponseDTO;
import swp490.greeenslot.dto.SensorReadingResponseDTO;
import swp490.greeenslot.dto.SensorTypeInfoDTO;
import swp490.greeenslot.entity.ESensorType;
import swp490.greeenslot.service.SensorReadingService;

import java.util.List;

/**
 * API nhan du lieu tu Arduino / ESP.
 *
 * Arduino POST: /api/iot/sensors/data
 * Header bat buoc: X-IoT-Api-Key: &lt;key trong application.yml&gt;
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/iot/sensors")
@Tag(name = "IoT Sensors", description = "Nhan va doc du lieu cam bien tu Arduino")
public class IoTSensorController {

    @Autowired
    private SensorReadingService sensorReadingService;

    @PostMapping("/data")
    @Operation(summary = "Arduino gui du lieu cam bien",
            description = "Gui mang readings de ho tro nhieu cam bien. THEM_CAM_BIEN_MOI: them phan tu vao readings.")
    public ResponseEntity<ArduinoSensorDataResponseDTO> receiveSensorData(
            @RequestHeader("X-IoT-Api-Key") String apiKey,
            @Valid @RequestBody ArduinoSensorDataRequestDTO request) {
        return ResponseEntity.ok(sensorReadingService.saveArduinoData(apiKey, request));
    }

    @GetMapping("/latest")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Gia tri moi nhat tung loai cam bien theo device")
    public ResponseEntity<List<SensorReadingResponseDTO>> getLatest(
            @Parameter(example = "arduino-greenhouse-01") @RequestParam String deviceId) {
        return ResponseEntity.ok(sensorReadingService.getLatestReadings(deviceId));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Lich su doc cam bien")
    public ResponseEntity<List<SensorReadingResponseDTO>> getHistory(
            @RequestParam String deviceId,
            @RequestParam(required = false) ESensorType sensorType,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(sensorReadingService.getHistory(deviceId, sensorType, limit));
    }

    @GetMapping("/types")
    @Operation(summary = "Danh sach loai cam bien dang ho tro")
    public ResponseEntity<List<SensorTypeInfoDTO>> getSupportedSensorTypes() {
        List<SensorTypeInfoDTO> types = java.util.Arrays.stream(ESensorType.values())
                .map(SensorTypeInfoDTO::from)
                .toList();
        return ResponseEntity.ok(types);
    }
}
