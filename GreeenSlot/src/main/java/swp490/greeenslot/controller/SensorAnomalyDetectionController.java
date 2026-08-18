package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.SensorAnomalyDTO;
import swp490.greeenslot.service.SensorAnomalyDetectionService;

import java.time.Instant;
import java.util.List;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/iot/anomalies")
@Tag(name = "Sensor Anomaly Detection", description = "APIs for detecting sensor anomalies and malfunctions")
public class SensorAnomalyDetectionController {

    @Autowired
    private SensorAnomalyDetectionService anomalyDetectionService;

    @GetMapping("/{deviceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER', 'ROLE_GARDEN_STAFF')")
    @Operation(summary = "Detect anomalies for device", description = "Detects sensor reading anomalies for a specific device in the last hour")
    public ResponseEntity<List<SensorAnomalyDTO>> detectAnomalies(@PathVariable String deviceId) {
        return ResponseEntity.ok(anomalyDetectionService.detectAnomalies(deviceId));
    }

    @GetMapping("/{deviceId}/time-range")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER', 'ROLE_GARDEN_STAFF')")
    @Operation(summary = "Detect anomalies by time range", description = "Detects sensor reading anomalies for a specific device within a time range")
    public ResponseEntity<List<SensorAnomalyDTO>> detectAnomaliesByTimeRange(
            @PathVariable String deviceId,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {
        return ResponseEntity.ok(anomalyDetectionService.detectAnomaliesByTimeRange(deviceId, startTime, endTime));
    }

    @GetMapping("/{deviceId}/health/{sensorType}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER', 'ROLE_GARDEN_STAFF')")
    @Operation(summary = "Check sensor health", description = "Checks the health status of a specific sensor type on a device")
    public ResponseEntity<SensorAnomalyDTO> checkSensorHealth(
            @PathVariable String deviceId,
            @PathVariable String sensorType) {
        return ResponseEntity.ok(anomalyDetectionService.checkSensorHealth(deviceId, sensorType));
    }
}
