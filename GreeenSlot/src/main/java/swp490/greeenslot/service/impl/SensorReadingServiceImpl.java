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
    private EquipmentRepository equipmentRepository; // THÊM REPOSITORY NÀY

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
        String serialNumber = request.getDeviceId().trim();
        String deviceId = request.getDeviceId().trim();
        Instant recordedAt = Instant.now();
        List<SensorReading> saved = new ArrayList<>();
        // 1. TÌM THIẾT BỊ ĐANG GẮN Ở TRỤ NÀO
        Equipment equipment = equipmentRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị với Serial: " + serialNumber));

        Pillar currentPillar = equipment.getPillar();
        if (currentPillar == null) {
            throw new IllegalArgumentException("Thiết bị " + serialNumber + " hiện chưa được gắn vào Trụ nào. Không thể lưu dữ liệu.");
        }

// 2. LƯU DỮ LIỆU CẢM BIẾN VỚI PILLAR_ID
        for (SensorReadingItemDTO item : request.getReadings()) {
            ESensorType sensorType = ESensorType.fromCode(item.getSensorType());
            validateReading(sensorType, item.getValue());

            String unit = item.getUnit() != null && !item.getUnit().isBlank()
                    ? item.getUnit().trim()
                    : sensorType.getDefaultUnit();

            SensorReading reading = new SensorReading();
            reading.setDeviceId(serialNumber);
            reading.setPillarId(currentPillar.getId()); // ĐÓNG DẤU ID CỦA TRỤ "P-Q1-02B" VÀO DATA
            reading.setSensorType(sensorType);
            reading.setValue(item.getValue());
            reading.setUnit(unit);
            reading.setRecordedAt(recordedAt);

            SensorReading savedReading = sensorReadingRepository.save(reading);
            saved.add(savedReading);

            // Bạn có thể truyền thẳng currentPillar vào hàm evaluateThresholds để đỡ phải query lại!
            evaluateThresholds(serialNumber, sensorType, item.getValue(), unit);
        }

        List<SensorReadingResponseDTO> responseReadings = saved.stream()
                .map(SensorReadingResponseDTO::fromEntity)
                .toList();

        return new ArduinoSensorDataResponseDTO("Đã lưu dữ liệu cảm biến cho trụ " + currentPillar.getPillarCode(), serialNumber, responseReadings.size(), responseReadings);
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
        Optional<Pillar> pillarOpt = pillarRepository.findByPillarCode(deviceId);
        if (pillarOpt.isEmpty()) {
            // Fallback: nếu deviceId chưa trùng khớp chính xác mã trụ (vd: arduino-greenhouse-01),
            // tìm trụ P-Q1-01 hoặc trụ đầu tiên trong hệ thống
            pillarOpt = pillarRepository.findByPillarCode("P-Q1-01");
            if (pillarOpt.isEmpty()) {
                pillarOpt = pillarRepository.findAll().stream().findFirst();
            }
        }
        Pillar pillar = pillarOpt.orElse(null);

        // Lấy ngưỡng mặc định của thiết bị / trụ
        Optional<SensorThreshold> thresholdOpt = sensorThresholdRepository.findByDeviceIdAndSensorType(deviceId, sensorType.name());
        if (thresholdOpt.isEmpty()) {
            thresholdOpt = sensorThresholdRepository.findByDeviceIdAndSensorType(deviceId, sensorType.getCode());
        }

        double defaultMin;
        double defaultMax;
        if (thresholdOpt.isPresent()) {
            defaultMin = thresholdOpt.get().getMinValue() != null ? thresholdOpt.get().getMinValue() : 0.0;
            defaultMax = thresholdOpt.get().getMaxValue() != null ? thresholdOpt.get().getMaxValue() : 100.0;
        } else {
            switch (sensorType) {
                case SOIL_MOISTURE -> { defaultMin = 35.0; defaultMax = 80.0; }
                case PH -> { defaultMin = 5.5; defaultMax = 7.5; }
                case LIGHT_INTENSITY -> { defaultMin = 500.0; defaultMax = 50000.0; }
                default -> { defaultMin = 0.0; defaultMax = 100.0; }
            }
        }

        List<GardenSlot> slots = new java.util.ArrayList<>();
        if (pillar != null && pillar.getGardenSlot() != null) {
            slots.add(pillar.getGardenSlot());
        }
        boolean hasActiveRentals = false;
        boolean autoSprayTriggered = false;

        for (GardenSlot slot : slots) {
            List<SlotRental> activeRentals = slotRentalRepository.findActiveRentals(slot.getId(), LocalDateTime.now());
            for (SlotRental rental : activeRentals) {
                hasActiveRentals = true;
                User customer = rental.getUser();
                Tree tree = rental.getTree();

                // Xác định ngưỡng tối ưu riêng theo CÂY TRỒNG (nếu có) hoặc dùng ngưỡng mặc định của trụ
                double effectiveMin = defaultMin;
                double effectiveMax = defaultMax;

                if (tree != null) {
                    if (sensorType == ESensorType.SOIL_MOISTURE && tree.getSoilMoistureMin() != null && tree.getSoilMoistureMax() != null) {
                        effectiveMin = tree.getSoilMoistureMin();
                        effectiveMax = tree.getSoilMoistureMax();
                    } else if (sensorType == ESensorType.PH && tree.getPhMin() != null && tree.getPhMax() != null) {
                        effectiveMin = tree.getPhMin();
                        effectiveMax = tree.getPhMax();
                    } else if (sensorType == ESensorType.LIGHT_INTENSITY && tree.getLightMin() != null && tree.getLightMax() != null) {
                        effectiveMin = tree.getLightMin();
                        effectiveMax = tree.getLightMax();
                    }
                }

                // Kiểm tra vượt ngưỡng
                if (value < effectiveMin || value > effectiveMax) {
                    String treeName = tree != null ? tree.getTreeName() : "Chưa xác định";
                    String treePrefix = tree != null ? ("cây " + tree.getTreeName() + " tại ") : "";

                    // 1. Tạo bản ghi Alert gắn chặt với CÂY TRỒNG, Ô ĐẤT và TRỤ IOT
                    swp490.greeenslot.entity.Alert alert = new swp490.greeenslot.entity.Alert();
                    alert.setAlertType(sensorType.name());
                    alert.setDescription(String.format("Cảm biến %s cho %sô %s (Trụ %s) ghi nhận giá trị %.2f %s, nằm ngoài ngưỡng sinh trưởng (%.2f - %.2f)",
                            sensorType.getDescription(), treePrefix, slot.getSlotNumber(), pillar.getPillarCode(), value, unit, effectiveMin, effectiveMax));
                    alert.setStatus(swp490.greeenslot.entity.EAlertStatus.PENDING);
                    alert.setThresholdValue((effectiveMin + effectiveMax) / 2.0);
                    alert.setActualValue(value);
                    alert.setSensorType(sensorType.name());
                    alert.setPillar(pillar);
                    alert.setGardenSlot(slot);
                    alert.setTree(tree);
                    alert.setCreatedAt(LocalDateTime.now());
                    Alert savedAlert = alertService.createAlert(alert);

                    // 2. Gửi thông báo cho Quản lý chi nhánh
                    if (pillar.getLocation() != null) {
                        String managerTitle = "Cảnh báo chỉ số cảm biến cây trồng";
                        String managerBody = String.format("Ô %s (%s - Trụ %s): Cảm biến %s vượt ngưỡng. Giá trị: %.2f %s (Ngưỡng: %.2f - %.2f)",
                                slot.getSlotNumber(), treeName, pillar.getPillarCode(), sensorType.getDescription(), value, unit, effectiveMin, effectiveMax);

                        firebaseMessagingService.sendPushNotificationToLocation(pillar.getLocation().getId(), managerTitle, managerBody, "ROLE_LOCATION_MANAGER");
                        firebaseMessagingService.sendPushNotificationToLocation(pillar.getLocation().getId(), managerTitle, managerBody, "ROLE_MANAGER");

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
                                        "/dashboard/staff/alert-processing"
                                );
                            }
                        }
                    }

                    // 3. Gửi thông báo cho Khách hàng sở hữu cây
                    if (customer != null) {
                        if (notificationService != null) {
                            notificationService.createNotification(
                                    customer.getId(),
                                    "Cảnh báo chỉ số cây trồng của bạn",
                                    String.format("Cảnh báo: Cảm biến %s tại ô %s (%s) ghi nhận %.2f %s, nằm ngoài ngưỡng sinh trưởng (%.2f - %.2f).",
                                            sensorType.getDescription(), slot.getSlotNumber(), treeName, value, unit, effectiveMin, effectiveMax),
                                    "IOT_ALERT",
                                    slot.getId(),
                                    "/dashboard/customer/monitoring"
                            );
                        }
                        firebaseMessagingService.sendPushNotification(
                                customer.getId(),
                                "Cảnh báo cảm biến cây trồng",
                                String.format("Ô %s (%s): Cảm biến %s đạt %.2f %s, ngoài ngưỡng sinh trưởng.",
                                        slot.getSlotNumber(), treeName, sensorType.getDescription(), value, unit)
                        );
                    }

                    // 4. Gửi thông báo cho Nhân viên chăm sóc
                    List<User> staffList = gardeningTaskRepository.findAssignedStaffBySlotId(slot.getId());
                    for (User staff : staffList) {
                        if (notificationService != null) {
                            notificationService.createNotification(
                                    staff.getId(),
                                    "Cảnh báo chỉ số cảm biến (Cần xử lý)",
                                    String.format("Cảnh báo: Cảm biến %s tại ô %s (%s) ghi nhận %.2f %s, ngoài ngưỡng (%.2f - %.2f). Yêu cầu kiểm tra.",
                                            sensorType.getDescription(), slot.getSlotNumber(), treeName, value, unit, effectiveMin, effectiveMax),
                                    "IOT_ALERT",
                                    slot.getId(),
                                    "/dashboard/garden-staff/alerts"
                            );
                        }
                        firebaseMessagingService.sendPushNotification(
                                staff.getId(),
                                "Cần kiểm tra: Cảnh báo cảm biến",
                                String.format("Ô %s (%s): Cảm biến %s bất thường (%.2f %s)",
                                        slot.getSlotNumber(), treeName, sensorType.getDescription(), value, unit)
                        );
                    }

                    // 5. Tự động tạo nhiệm vụ khẩn cấp cho nhân viên nếu chưa có
                    String emergencyTaskName = "Khẩn cấp: Cảnh báo cảm biến - " + sensorType.getDescription();
                    boolean taskExists = gardeningTaskRepository.existsByTargetSlotIdAndTaskNameAndStatus(
                            slot.getId(), emergencyTaskName, swp490.greeenslot.entity.ETaskStatus.PENDING);
                    if (!taskExists) {
                        swp490.greeenslot.entity.GardeningTask emergencyTask = new swp490.greeenslot.entity.GardeningTask();
                        emergencyTask.setTaskName(emergencyTaskName);
                        emergencyTask.setDescription(String.format("Kiểm tra khẩn cấp ô %s (%s). Cảm biến %s ghi nhận %.2f %s (Ngưỡng: %.2f - %.2f).",
                                slot.getSlotNumber(), treeName, sensorType.getDescription(), value, unit, effectiveMin, effectiveMax));
                        emergencyTask.setStatus(swp490.greeenslot.entity.ETaskStatus.PENDING);
                        emergencyTask.setTaskType(swp490.greeenslot.entity.ETaskType.MAINTENANCE);
                        emergencyTask.setTargetSlot(slot);
                        emergencyTask.setCreatedAt(LocalDateTime.now());
                        gardeningTaskRepository.save(emergencyTask);
                    }

                    // 6. Tự động kích hoạt bơm xịt nước nếu độ ẩm đất < ngưỡng tối thiểu của cây
                    if (sensorType == ESensorType.SOIL_MOISTURE && value < effectiveMin && !autoSprayTriggered) {
                        String autoReason = String.format("Tự động tưới: Độ ẩm đất %.2f%% < ngưỡng tối thiểu %.2f%% của %s tại ô %s (Trụ %s)",
                                value, effectiveMin, treePrefix, slot.getSlotNumber(), pillar.getPillarCode());
                        boolean autoSprayed = pumpService.triggerAutoSpray(autoReason);
                        if (autoSprayed) {
                            autoSprayTriggered = true;
                            if (notificationService != null && customer != null) {
                                notificationService.createNotification(
                                        customer.getId(),
                                        "Hệ thống tự động tưới cây (Smart Irrigation)",
                                        String.format("Hệ thống IoT vừa tự động kích hoạt máy bơm xịt nước cho ô %s (%s) do độ ẩm đất giảm thấp (%.2f%% < %.2f%%).",
                                                slot.getSlotNumber(), treeName, value, effectiveMin),
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

        // Trường hợp không có hợp đồng thuê nào đang hoạt động nhưng số đo vượt ngưỡng của trụ/thiết bị
        if (!hasActiveRentals && (value < defaultMin || value > defaultMax)) {
            swp490.greeenslot.entity.Alert alert = new swp490.greeenslot.entity.Alert();
            alert.setAlertType(sensorType.name());
            String pillarCode = pillar != null ? pillar.getPillarCode() : deviceId;
            alert.setDescription(String.format("Cảm biến %s trên thiết bị/trụ %s ghi nhận giá trị %.2f %s, nằm ngoài ngưỡng cho phép (%.2f - %.2f)",
                    sensorType.getDescription(), pillarCode, value, unit, defaultMin, defaultMax));
            alert.setStatus(swp490.greeenslot.entity.EAlertStatus.PENDING);
            alert.setThresholdValue((defaultMin + defaultMax) / 2.0);
            alert.setActualValue(value);
            alert.setSensorType(sensorType.name());
            alert.setPillar(pillar);
            alert.setCreatedAt(LocalDateTime.now());
            Alert savedAlert = alertService.createAlert(alert);

            // Gửi thông báo cho Quản lý
            if (notificationService != null) {
                Long locId = (pillar != null && pillar.getLocation() != null) ? pillar.getLocation().getId() : null;
                List<User> managers = locId != null
                        ? userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, locId)
                        : userRepository.findByRoleName(ERole.ROLE_MANAGER);
                if (managers.isEmpty()) {
                    managers = userRepository.findByRoleName(ERole.ROLE_MANAGER);
                }
                for (User manager : managers) {
                    notificationService.createNotification(
                            manager.getId(),
                            "Cảnh báo chỉ số cảm biến (" + sensorType.getDescription() + ")",
                            String.format("Thiết bị/Trụ %s: Cảm biến %s ghi nhận %.2f %s, ngoài ngưỡng (%.2f - %.2f).",
                                    pillarCode, sensorType.getDescription(), value, unit, defaultMin, defaultMax),
                            "IOT_ALERT",
                            savedAlert != null ? savedAlert.getId() : null,
                            "/dashboard/staff/alert-processing"
                    );
                }
            }

            // Tự động kích hoạt bơm xịt nước nếu độ ẩm đất < ngưỡng tối thiểu
            if (sensorType == ESensorType.SOIL_MOISTURE && value < defaultMin) {
                String autoReason = String.format("Tự động tưới: Độ ẩm đất %.2f%% < ngưỡng tối thiểu %.2f%% tại thiết bị/trụ %s",
                        value, defaultMin, pillarCode);
                pumpService.triggerAutoSpray(autoReason);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SensorReadingResponseDTO> getLatestReadings(String identifier) {
        // identifier mà Frontend (React) truyền lên là PillarCode (vd: "P-Q1-02B")
        String targetCode = (identifier != null && !identifier.isBlank()) ? identifier.trim() : "arduino-greenhouse-01";

        Optional<Pillar> pillarOpt = pillarRepository.findByPillarCode(targetCode);
        if (pillarOpt.isEmpty()) {
            return new ArrayList<>(); // Nếu không tìm thấy trụ, trả về mảng rỗng
        }

        Long pillarId = pillarOpt.get().getId();

        return Arrays.stream(ESensorType.values())
                .map(type -> sensorReadingRepository.findFirstByPillarIdAndSensorTypeOrderByRecordedAtDesc(pillarId, type))
                .filter(Optional::isPresent)
                .map(latest -> SensorReadingResponseDTO.fromEntity(latest.get()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SensorReadingResponseDTO> getHistory(String identifier, ESensorType sensorType, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        String targetCode = (identifier != null && !identifier.isBlank()) ? identifier.trim() : "arduino-greenhouse-01";

        Optional<Pillar> pillarOpt = pillarRepository.findByPillarCode(targetCode);
        if (pillarOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Long pillarId = pillarOpt.get().getId();
        List<SensorReading> readings;

        if (sensorType != null) {
            readings = sensorReadingRepository.findByPillarIdAndSensorTypeOrderByRecordedAtDesc(pillarId, sensorType, PageRequest.of(0, safeLimit));
        } else {
            readings = sensorReadingRepository.findByPillarIdOrderByRecordedAtDesc(pillarId, PageRequest.of(0, safeLimit));
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
