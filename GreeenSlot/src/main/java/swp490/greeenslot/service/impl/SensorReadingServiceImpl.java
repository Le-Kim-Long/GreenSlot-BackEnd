package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.ArduinoSensorDataRequestDTO;
import swp490.greeenslot.dto.ArduinoSensorDataResponseDTO;
import swp490.greeenslot.dto.DeviceTelemetryRequestDTO;
import swp490.greeenslot.dto.SensorReadingItemDTO;
import swp490.greeenslot.dto.SensorReadingResponseDTO;
import swp490.greeenslot.dto.SensorAggregateDTO;
import swp490.greeenslot.entity.ESensorType;
import swp490.greeenslot.entity.SensorReading;
import swp490.greeenslot.entity.SensorThreshold;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.SensorReadingRepository;
import swp490.greeenslot.repository.SensorThresholdRepository;
import swp490.greeenslot.repository.PillarRepository;
import swp490.greeenslot.repository.GardenSlotRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.GardeningTaskRepository;
import swp490.greeenslot.service.SensorReadingService;
import swp490.greeenslot.service.NotificationService;

import java.time.LocalDateTime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class SensorReadingServiceImpl implements SensorReadingService {

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private SensorThresholdRepository sensorThresholdRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private swp490.greeenslot.service.FirebaseMessagingService firebaseMessagingService;

    @Autowired
    private swp490.greeenslot.service.AlertService alertService;

    @Value("${greeenslot.iot.api-key:GreenSlot-IoT-Dev-Key}")
    private String iotApiKey;

    @Override
    @Transactional
    public ArduinoSensorDataResponseDTO saveArduinoData(String apiKey, ArduinoSensorDataRequestDTO request) {
        validateApiKey(apiKey);

        String deviceId = request.getDeviceId().trim();
        Instant recordedAt = Instant.now();
        List<SensorReading> saved = new ArrayList<>();

        for (SensorReadingItemDTO item : request.getReadings()) {
            ESensorType sensorType = ESensorType.fromCode(item.getSensorType());
            validateReading(sensorType, item.getValue());

            String unit = item.getUnit() != null && !item.getUnit().isBlank()
                    ? item.getUnit().trim()
                    : sensorType.getDefaultUnit();

            SensorReading reading = new SensorReading(
                    deviceId,
                    sensorType,
                    item.getValue(),
                    unit,
                    recordedAt);
            SensorReading savedReading = sensorReadingRepository.save(reading);
            saved.add(savedReading);
            
            // Evaluate thresholds for each reading
            evaluateThresholds(deviceId, sensorType, item.getValue(), unit);
        }

        List<SensorReadingResponseDTO> responseReadings = saved.stream()
                .map(SensorReadingResponseDTO::fromEntity)
                .toList();

        return new ArduinoSensorDataResponseDTO(
                "Sensor data saved successfully.",
                deviceId,
                responseReadings.size(),
                responseReadings);
    }

    @Override
    @Transactional
    public ArduinoSensorDataResponseDTO saveDeviceTelemetry(String apiKey, DeviceTelemetryRequestDTO request) {
        validateApiKey(apiKey);
        String deviceId = request.getDeviceId().trim();
        String sensorTypeStr = request.getSensorType().trim();
        ESensorType sensorType = ESensorType.fromCode(sensorTypeStr);
        Double value = request.getValue();
        
        validateReading(sensorType, value);

        String unit = request.getUnit() != null && !request.getUnit().isBlank()
                ? request.getUnit().trim()
                : sensorType.getDefaultUnit();

        SensorReading reading = new SensorReading(
                deviceId,
                sensorType,
                value,
                unit,
                Instant.now()
        );
        SensorReading savedReading = sensorReadingRepository.save(reading);

        // Evaluate thresholds
        evaluateThresholds(deviceId, sensorType, value, unit);

        SensorReadingResponseDTO responseDTO = SensorReadingResponseDTO.fromEntity(savedReading);
        return new ArduinoSensorDataResponseDTO(
                "Device telemetry saved successfully.",
                deviceId,
                1,
                List.of(responseDTO)
        );
    }

    private void evaluateThresholds(String deviceId, ESensorType sensorType, Double value, String unit) {
        Optional<SensorThreshold> thresholdOpt = sensorThresholdRepository.findByDeviceIdAndSensorType(deviceId, sensorType.name());
        if (thresholdOpt.isEmpty()) {
            // Try with code if name doesn't match
            thresholdOpt = sensorThresholdRepository.findByDeviceIdAndSensorType(deviceId, sensorType.getCode());
        }

        if (thresholdOpt.isPresent()) {
            SensorThreshold threshold = thresholdOpt.get();
            if (value < threshold.getMinValue() || value > threshold.getMaxValue()) {
                // Threshold violation detected!
                Optional<Pillar> pillarOpt = pillarRepository.findByPillarCode(deviceId);
                if (pillarOpt.isPresent()) {
                    Pillar pillar = pillarOpt.get();
                    
                    // Create Alert record for managers
                    swp490.greeenslot.entity.Alert alert = new swp490.greeenslot.entity.Alert();
                    alert.setAlertType(sensorType.name());
                    alert.setDescription(String.format("Sensor %s on pillar %s reported value %f %s, outside threshold %f - %f", 
                            sensorType.getDescription(), pillar.getPillarCode(), value, unit, threshold.getMinValue(), threshold.getMaxValue()));
                    alert.setStatus(swp490.greeenslot.entity.EAlertStatus.PENDING);
                    alert.setThresholdValue((threshold.getMaxValue() - threshold.getMinValue()) / 2 + threshold.getMinValue());
                    alert.setActualValue(value);
                    alert.setSensorType(sensorType.name());
                    alert.setPillar(pillar);
                    alert.setCreatedAt(LocalDateTime.now());
                    alertService.createAlert(alert);
                    
                    // Alert location managers
                    if (pillar.getLocation() != null) {
                        String managerTitle = "IoT Sensor Threshold Alert";
                        String managerBody = String.format("Pillar %s: Sensor %s exceeded threshold. Value: %f %s (Range: %f - %f)",
                                pillar.getPillarCode(), sensorType.getDescription(), value, unit, threshold.getMinValue(), threshold.getMaxValue());
                        
                        firebaseMessagingService.sendPushNotificationToLocation(
                                pillar.getLocation().getId(), 
                                managerTitle, 
                                managerBody, 
                                "ROLE_LOCATION_MANAGER"
                        );
                        
                        // Also notify general managers
                        firebaseMessagingService.sendPushNotificationToLocation(
                                pillar.getLocation().getId(), 
                                managerTitle, 
                                managerBody, 
                                "ROLE_MANAGER"
                        );
                    }
                    
                    List<GardenSlot> slots = gardenSlotRepository.findByPillarId(pillar.getId());
                    for (GardenSlot slot : slots) {
                        List<SlotRental> activeRentals = slotRentalRepository.findActiveRentals(slot.getId(), LocalDateTime.now());
                        for (SlotRental rental : activeRentals) {
                            User customer = rental.getUser();
                            // Save notification for customer
                            notificationService.createNotification(
                                    customer.getId(),
                                    "IoT Sensor Warning Alert",
                                    String.format("Alert: Sensor %s on slot %s is reporting %f %s, which is outside the set threshold boundaries of %f to %f.",
                                            sensorType.getDescription(), slot.getSlotNumber(), value, unit, threshold.getMinValue(), threshold.getMaxValue()),
                                    "IOT_ALERT"
                            );
                            
                            // Send push notification to customer
                            firebaseMessagingService.sendPushNotification(
                                    customer.getId(),
                                    "IoT Sensor Warning",
                                    String.format("Slot %s: Sensor %s reading %f %s is outside normal range", 
                                            slot.getSlotNumber(), sensorType.getDescription(), value, unit)
                            );

                            // Save notification for assigned staff member(s)
                            List<User> staffList = gardeningTaskRepository.findAssignedStaffBySlotId(slot.getId());
                            for (User staff : staffList) {
                                notificationService.createNotification(
                                        staff.getId(),
                                        "IoT Sensor Warning Alert (Staff Action Required)",
                                        String.format("Alert: Sensor %s on slot %s is reporting %f %s, which is outside the set threshold boundaries of %f to %f. Assigned Staff Action Required.",
                                                sensorType.getDescription(), slot.getSlotNumber(), value, unit, threshold.getMinValue(), threshold.getMaxValue()),
                                        "IOT_ALERT"
                                );
                                
                                // Send push notification to staff
                                firebaseMessagingService.sendPushNotification(
                                        staff.getId(),
                                        "Action Required: IoT Sensor Alert",
                                        String.format("Slot %s: Sensor %s requires attention. Value: %f %s", 
                                                slot.getSlotNumber(), sensorType.getDescription(), value, unit)
                                );
                            }

                            // Automatically spawn emergency MAINTENANCE GardeningTask if not already present
                            String emergencyTaskName = "Emergency: IoT Sensor Warning - " + sensorType.getDescription();
                            boolean taskExists = gardeningTaskRepository.existsByTargetSlotIdAndTaskNameAndStatus(
                                    slot.getId(), emergencyTaskName, swp490.greeenslot.entity.ETaskStatus.PENDING);
                            
                            if (!taskExists) {
                                swp490.greeenslot.entity.GardeningTask emergencyTask = new swp490.greeenslot.entity.GardeningTask();
                                emergencyTask.setTaskName(emergencyTaskName);
                                emergencyTask.setDescription(String.format("Emergency check requested for Slot %s. Sensor %s reported value %f %s (Threshold: %f - %f).", 
                                        slot.getSlotNumber(), sensorType.getDescription(), value, unit, threshold.getMinValue(), threshold.getMaxValue()));
                                emergencyTask.setStatus(swp490.greeenslot.entity.ETaskStatus.PENDING);
                                emergencyTask.setTaskType(swp490.greeenslot.entity.ETaskType.MAINTENANCE);
                                emergencyTask.setTargetSlot(slot);
                                emergencyTask.setCreatedAt(LocalDateTime.now());
                                gardeningTaskRepository.save(emergencyTask);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SensorReadingResponseDTO> getLatestReadings(String deviceId) {
        return Arrays.stream(ESensorType.values())
                .map(type -> sensorReadingRepository
                        .findFirstByDeviceIdAndSensorTypeOrderByRecordedAtDesc(deviceId.trim(), type))
                .filter(Optional::isPresent)
                .map(latest -> SensorReadingResponseDTO.fromEntity(latest.get()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SensorReadingResponseDTO> getHistory(String deviceId, ESensorType sensorType, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<SensorReading> readings;

        if (sensorType != null) {
            readings = sensorReadingRepository.findByDeviceIdAndSensorTypeOrderByRecordedAtDesc(
                    deviceId.trim(), sensorType, PageRequest.of(0, safeLimit));
        } else {
            readings = sensorReadingRepository.findByDeviceIdOrderByRecordedAtDesc(
                    deviceId.trim(), PageRequest.of(0, safeLimit));
        }

        return readings.stream()
                .map(SensorReadingResponseDTO::fromEntity)
                .toList();
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || !iotApiKey.equals(apiKey.trim())) {
            throw new IllegalArgumentException("Invalid IoT API key.");
        }
    }

    /**
     * THEM_CAM_BIEN_MOI: them rule kiem tra khoang gia tri hop le tai day.
     */
    private void validateReading(ESensorType sensorType, Double value) {
        if (value == null) {
            throw new IllegalArgumentException(sensorType.name() + " value is required.");
        }

        switch (sensorType) {
            case SOIL_MOISTURE -> {
                if (value < 0 || value > 100) {
                    throw new IllegalArgumentException("SOIL_MOISTURE must be between 0 and 100 (%).");
                }
            }
            case PH -> {
                if (value < 0 || value > 14) {
                    throw new IllegalArgumentException("PH must be between 0 and 14.");
                }
            }
            default -> {
                // THEM_CAM_BIEN_MOI: them case validate cho cam bien moi
            }
        }
    }

    @Override
    public List<SensorAggregateDTO> getHourlyAggregates(Long pillarId, ESensorType sensorType, int hoursBack) {
        Instant now = Instant.now();
        Instant startTime = now.minusSeconds((long) hoursBack * 3600);
        
        List<Object[]> results = sensorReadingRepository.findHourlyAggregatesByPillar(
                pillarId, sensorType, startTime, now);
        
        return results.stream().map(row -> SensorAggregateDTO.builder()
                .timestamp(Instant.ofEpochMilli(((Number) row[4]).longValue()).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .sensorType(sensorType.name())
                .avgValue(((Number) row[0]).doubleValue())
                .minValue(((Number) row[1]).doubleValue())
                .maxValue(((Number) row[2]).doubleValue())
                .readingCount(((Number) row[3]).longValue())
                .build()).toList();
    }

    @Override
    public List<SensorAggregateDTO> getDailyAggregates(Long pillarId, ESensorType sensorType, int daysBack) {
        Instant now = Instant.now();
        Instant startTime = now.minusSeconds((long) daysBack * 86400);
        
        List<Object[]> results = sensorReadingRepository.findDailyAggregatesByPillar(
                pillarId, sensorType, startTime, now);
        
        return results.stream().map(row -> SensorAggregateDTO.builder()
                .timestamp(Instant.ofEpochMilli(((Number) row[4]).longValue()).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .sensorType(sensorType.name())
                .avgValue(((Number) row[0]).doubleValue())
                .minValue(((Number) row[1]).doubleValue())
                .maxValue(((Number) row[2]).doubleValue())
                .readingCount(((Number) row[3]).longValue())
                .build()).toList();
    }

    @Override
    public List<SensorAggregateDTO> getWeeklyAggregates(Long pillarId, ESensorType sensorType, int weeksBack) {
        Instant now = Instant.now();
        Instant startTime = now.minusSeconds((long) weeksBack * 604800);
        
        List<Object[]> results = sensorReadingRepository.findWeeklyAggregatesByPillar(
                pillarId, sensorType, startTime, now);
        
        return results.stream().map(row -> SensorAggregateDTO.builder()
                .timestamp(Instant.ofEpochMilli(((Number) row[4]).longValue()).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .sensorType(sensorType.name())
                .avgValue(((Number) row[0]).doubleValue())
                .minValue(((Number) row[1]).doubleValue())
                .maxValue(((Number) row[2]).doubleValue())
                .readingCount(((Number) row[3]).longValue())
                .build()).toList();
    }
}
