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
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
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
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private swp490.greeenslot.service.FirebaseMessagingService firebaseMessagingService;

    @Autowired
    private swp490.greeenslot.service.AlertService alertService;

    @Autowired
    private swp490.greeenslot.service.PumpService pumpService;

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

        SensorThreshold threshold;
        if (thresholdOpt.isPresent()) {
            threshold = thresholdOpt.get();
        } else {
            // Smart agricultural default thresholds if not yet explicitly configured per device
            threshold = new SensorThreshold();
            threshold.setDeviceId(deviceId);
            threshold.setSensorType(sensorType.name());
            switch (sensorType) {
                case SOIL_MOISTURE -> {
                    threshold.setMinValue(35.0); // Ngưỡng tối thiểu độ ẩm đất: 35%
                    threshold.setMaxValue(80.0); // Ngưỡng tối đa độ ẩm đất: 80%
                }
                case PH -> {
                    threshold.setMinValue(5.5);  // Ngưỡng tối thiểu pH: 5.5
                    threshold.setMaxValue(7.5);  // Ngưỡng tối đa pH: 7.5
                }
                case LIGHT_INTENSITY -> {
                    threshold.setMinValue(500.0);   // Ngưỡng tối thiểu ánh sáng: 500 Lux
                    threshold.setMaxValue(50000.0); // Ngưỡng tối đa ánh sáng: 50,000 Lux
                }
                default -> {
                    threshold.setMinValue(0.0);
                    threshold.setMaxValue(100.0);
                }
            }
        }

        if (value < threshold.getMinValue() || value > threshold.getMaxValue()) {
            // Threshold violation detected!
            Optional<Pillar> pillarOpt = pillarRepository.findByPillarCode(deviceId);
            if (pillarOpt.isPresent()) {
                Pillar pillar = pillarOpt.get();
                    
                    // Create Alert record for managers
                    swp490.greeenslot.entity.Alert alert = new swp490.greeenslot.entity.Alert();
                    alert.setAlertType(sensorType.name());
                    alert.setDescription(String.format("Cảm biến %s trên trụ %s ghi nhận giá trị %.2f %s, nằm ngoài ngưỡng (%.2f - %.2f)", 
                            sensorType.getDescription(), pillar.getPillarCode(), value, unit, threshold.getMinValue(), threshold.getMaxValue()));
                    alert.setStatus(swp490.greeenslot.entity.EAlertStatus.PENDING);
                    alert.setThresholdValue((threshold.getMaxValue() - threshold.getMinValue()) / 2 + threshold.getMinValue());
                    alert.setActualValue(value);
                    alert.setSensorType(sensorType.name());
                    alert.setPillar(pillar);
                    alert.setCreatedAt(LocalDateTime.now());
                    Alert savedAlert = alertService.createAlert(alert);
                    
                    // Alert location managers
                    if (pillar.getLocation() != null) {
                        String managerTitle = "Cảnh báo chỉ số cảm biến IoT";
                        String managerBody = String.format("Trụ %s: Cảm biến %s vượt ngưỡng. Giá trị: %.2f %s (Ngưỡng: %.2f - %.2f)",
                                pillar.getPillarCode(), sensorType.getDescription(), value, unit, threshold.getMinValue(), threshold.getMaxValue());
                        
                        firebaseMessagingService.sendPushNotificationToLocation(
                                pillar.getLocation().getId(), 
                                managerTitle, 
                                managerBody, 
                                "ROLE_LOCATION_MANAGER"
                        );
                        
                        firebaseMessagingService.sendPushNotificationToLocation(
                                pillar.getLocation().getId(), 
                                managerTitle, 
                                managerBody, 
                                "ROLE_MANAGER"
                        );

                        if (notificationService != null) {
                            List<User> managers = userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, pillar.getLocation().getId());
                            if (managers.isEmpty()) {
                                managers = userRepository.findByRoleName(ERole.ROLE_MANAGER);
                            }
                            for (User manager : managers) {
                                notificationService.createNotification(
                                        manager.getId(),
                                        managerTitle,
                                        managerBody,
                                        "IOT_ALERT",
                                        savedAlert != null ? savedAlert.getId() : null,
                                        "/dashboard/manager/alerts"
                                );
                            }
                        }
                    }
                    
                    List<GardenSlot> slots = gardenSlotRepository.findByPillarId(pillar.getId());
                    for (GardenSlot slot : slots) {
                        List<SlotRental> activeRentals = slotRentalRepository.findActiveRentals(slot.getId(), LocalDateTime.now());
                        for (SlotRental rental : activeRentals) {
                            User customer = rental.getUser();
                            // Save notification for customer
                            if (notificationService != null) {
                                notificationService.createNotification(
                                        customer.getId(),
                                        "Cảnh báo chỉ số cảm biến",
                                        String.format("Cảnh báo: Cảm biến %s tại ô đất %s ghi nhận %.2f %s, nằm ngoài ngưỡng cho phép (%.2f - %.2f).",
                                                sensorType.getDescription(), slot.getSlotNumber(), value, unit, threshold.getMinValue(), threshold.getMaxValue()),
                                        "IOT_ALERT",
                                        slot.getId(),
                                        "/dashboard/customer/iot"
                                );
                            }
                            
                            // Send push notification to customer
                            firebaseMessagingService.sendPushNotification(
                                    customer.getId(),
                                    "Cảnh báo cảm biến IoT",
                                    String.format("Ô %s: Cảm biến %s đạt %.2f %s, nằm ngoài khoảng bình thường", 
                                            slot.getSlotNumber(), sensorType.getDescription(), value, unit)
                            );

                            // Save notification for assigned staff member(s)
                            List<User> staffList = gardeningTaskRepository.findAssignedStaffBySlotId(slot.getId());
                            for (User staff : staffList) {
                                if (notificationService != null) {
                                    notificationService.createNotification(
                                            staff.getId(),
                                            "Cảnh báo chỉ số cảm biến (Cần xử lý)",
                                            String.format("Cảnh báo: Cảm biến %s tại ô %s ghi nhận %.2f %s, nằm ngoài ngưỡng (%.2f - %.2f). Yêu cầu nhân viên kiểm tra.",
                                                    sensorType.getDescription(), slot.getSlotNumber(), value, unit, threshold.getMinValue(), threshold.getMaxValue()),
                                            "IOT_ALERT",
                                            slot.getId(),
                                            "/dashboard/staff/tasks"
                                    );
                                }
                                
                                // Send push notification to staff
                                firebaseMessagingService.sendPushNotification(
                                        staff.getId(),
                                        "Cần kiểm tra: Cảnh báo cảm biến",
                                        String.format("Ô %s: Cảm biến %s bất thường (%.2f %s)", 
                                                slot.getSlotNumber(), sensorType.getDescription(), value, unit)
                                );
                            }

                            // Automatically spawn emergency MAINTENANCE GardeningTask if not already present
                            String emergencyTaskName = "Khẩn cấp: Cảnh báo cảm biến - " + sensorType.getDescription();
                            boolean taskExists = gardeningTaskRepository.existsByTargetSlotIdAndTaskNameAndStatus(
                                    slot.getId(), emergencyTaskName, swp490.greeenslot.entity.ETaskStatus.PENDING);
                            
                            if (!taskExists) {
                                swp490.greeenslot.entity.GardeningTask emergencyTask = new swp490.greeenslot.entity.GardeningTask();
                                emergencyTask.setTaskName(emergencyTaskName);
                                emergencyTask.setDescription(String.format("Kiểm tra khẩn cấp ô %s. Cảm biến %s ghi nhận %.2f %s (Ngưỡng: %.2f - %.2f).", 
                                        slot.getSlotNumber(), sensorType.getDescription(), value, unit, threshold.getMinValue(), threshold.getMaxValue()));
                                emergencyTask.setStatus(swp490.greeenslot.entity.ETaskStatus.PENDING);
                                emergencyTask.setTaskType(swp490.greeenslot.entity.ETaskType.MAINTENANCE);
                                emergencyTask.setTargetSlot(slot);
                                emergencyTask.setCreatedAt(LocalDateTime.now());
                                gardeningTaskRepository.save(emergencyTask);
                            }
                        }
                    }

                    // TỰ ĐỘNG BẬT MÁY BƠM/XỊT NƯỚC: Nếu độ ẩm đất thấp hơn ngưỡng tối thiểu
                    if (sensorType == ESensorType.SOIL_MOISTURE && value < threshold.getMinValue()) {
                        String autoReason = String.format("Tự động tưới: Độ ẩm đất %.2f%% thấp hơn ngưỡng min %.2f%% tại trụ %s", 
                                value, threshold.getMinValue(), pillar.getPillarCode());
                        boolean autoSprayed = pumpService.triggerAutoSpray(autoReason);
                        if (autoSprayed && notificationService != null) {
                            for (GardenSlot slot : slots) {
                                List<SlotRental> activeRentals = slotRentalRepository.findActiveRentals(slot.getId(), LocalDateTime.now());
                                for (SlotRental rental : activeRentals) {
                                    User customer = rental.getUser();
                                    notificationService.createNotification(
                                            customer.getId(),
                                            "Hệ thống tự động tưới cây (Smart Irrigation)",
                                            String.format("Hệ thống IoT vừa tự động kích hoạt máy bơm xịt nước cho ô %s do độ ẩm đất giảm thấp (%.2f%% < %.2f%%).",
                                                    slot.getSlotNumber(), value, threshold.getMinValue()),
                                            "IOT_AUTO_WATERING",
                                            slot.getId(),
                                            "/dashboard/customer/monitoring"
                                    );
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

    /** Cot bucket la DATETIME2 native tra ve tu SQL Server, driver map thanh java.sql.Timestamp. */
    private static java.time.LocalDateTime toLocalDateTime(Object bucketColumn) {
        if (bucketColumn instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (bucketColumn instanceof java.time.LocalDateTime ldt) {
            return ldt;
        }
        throw new IllegalStateException("Unexpected bucket column type: " + bucketColumn.getClass());
    }

    @Override
    public List<SensorAggregateDTO> getHourlyAggregates(Long pillarId, ESensorType sensorType, int hoursBack) {
        Instant now = Instant.now();
        Instant startTime = now.minusSeconds((long) hoursBack * 3600);

        List<Object[]> results = sensorReadingRepository.findHourlyAggregatesByPillar(
                pillarId, sensorType.name(), startTime, now);

        return results.stream().map(row -> SensorAggregateDTO.builder()
                .timestamp(toLocalDateTime(row[4]))
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
                pillarId, sensorType.name(), startTime, now);

        return results.stream().map(row -> SensorAggregateDTO.builder()
                .timestamp(toLocalDateTime(row[4]))
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
                pillarId, sensorType.name(), startTime, now);

        return results.stream().map(row -> SensorAggregateDTO.builder()
                .timestamp(toLocalDateTime(row[4]))
                .sensorType(sensorType.name())
                .avgValue(((Number) row[0]).doubleValue())
                .minValue(((Number) row[1]).doubleValue())
                .maxValue(((Number) row[2]).doubleValue())
                .readingCount(((Number) row[3]).longValue())
                .build()).toList();
    }
}
