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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private StaffScheduleRepository staffScheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private swp490.greeenslot.service.FirebaseMessagingService firebaseMessagingService;

    @Autowired
    private swp490.greeenslot.service.AlertService alertService;

    @Autowired
    private swp490.greeenslot.repository.AlertRepository alertRepository;

    @Autowired
    private swp490.greeenslot.service.PumpService pumpService;

    @Value("${greeenslot.iot.api-key:GreenSlot-IoT-Dev-Key}")
    private String iotApiKey;

    @Override
    @Transactional
    public ArduinoSensorDataResponseDTO saveArduinoData(String apiKey, ArduinoSensorDataRequestDTO request) {
        validateApiKey(apiKey);
        String serialNumber = request.getDeviceId().trim();
        Instant recordedAt = Instant.now();
        List<SensorReading> saved = new ArrayList<>();

        // 1. TÌM THIẾT BỊ HOẶC TRỤ
        Pillar currentPillar = equipmentRepository.findBySerialNumber(serialNumber)
                .map(Equipment::getPillar)
                .orElseGet(() -> pillarRepository.findByPillarCode(serialNumber)
                        .orElseGet(() -> gardenSlotRepository.findBySlotNumber(serialNumber)
                                .map(GardenSlot::getPillar)
                                .orElse(null)));

        if (currentPillar == null) {
            throw new IllegalArgumentException("Không xác định được Trụ tương ứng với mã/Serial: " + serialNumber);
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
            reading.setPillarId(currentPillar.getId());
            reading.setSensorType(sensorType);
            reading.setValue(item.getValue());
            reading.setUnit(unit);
            reading.setRecordedAt(recordedAt);

            SensorReading savedReading = sensorReadingRepository.save(reading);
            saved.add(savedReading);

            evaluateThresholds(currentPillar, serialNumber, sensorType, item.getValue(), unit);
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

        Pillar pillar = pillarRepository.findByPillarCode(deviceId)
                .orElseGet(() -> equipmentRepository.findBySerialNumber(deviceId)
                        .map(Equipment::getPillar)
                        .orElseGet(() -> gardenSlotRepository.findBySlotNumber(deviceId)
                                .map(GardenSlot::getPillar)
                                .orElse(null)));

        SensorReading reading = new SensorReading(
                deviceId,
                sensorType,
                value,
                unit,
                Instant.now()
        );
        if (pillar != null) {
            reading.setPillarId(pillar.getId());
        }
        SensorReading savedReading = sensorReadingRepository.save(reading);

        // Evaluate thresholds
        evaluateThresholds(pillar, deviceId, sensorType, value, unit);

        SensorReadingResponseDTO responseDTO = SensorReadingResponseDTO.fromEntity(savedReading);
        return new ArduinoSensorDataResponseDTO(
                "Device telemetry saved successfully.",
                deviceId,
                1,
                List.of(responseDTO)
        );
    }

    private void evaluateThresholds(Pillar pillar, String deviceId, ESensorType sensorType, Double value, String unit) {
        if (pillar == null) {
            pillar = pillarRepository.findByPillarCode(deviceId)
                    .orElseGet(() -> equipmentRepository.findBySerialNumber(deviceId)
                            .map(Equipment::getPillar)
                            .orElseGet(() -> gardenSlotRepository.findBySlotNumber(deviceId)
                                    .map(GardenSlot::getPillar)
                                    .orElse(null)));
        }

        // Lấy ngưỡng mặc định của thiết bị / trụ
        String thresholdKey = pillar != null ? pillar.getPillarCode() : deviceId;
        Optional<SensorThreshold> thresholdOpt = sensorThresholdRepository.findByDeviceIdAndSensorType(thresholdKey, sensorType.name());
        if (thresholdOpt.isEmpty()) {
            thresholdOpt = sensorThresholdRepository.findByDeviceIdAndSensorType(thresholdKey, sensorType.getCode());
        }
        if (thresholdOpt.isEmpty() && !thresholdKey.equals(deviceId)) {
            thresholdOpt = sensorThresholdRepository.findByDeviceIdAndSensorType(deviceId, sensorType.name());
            if (thresholdOpt.isEmpty()) {
                thresholdOpt = sensorThresholdRepository.findByDeviceIdAndSensorType(deviceId, sensorType.getCode());
            }
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
        } else if (pillar != null) {
            final Long targetPillarId = pillar.getId();
            gardenSlotRepository.findAll().stream()
                    .filter(s -> (s.getPillar() != null && s.getPillar().getId().equals(targetPillarId))
                            || (s.getPillars() != null && s.getPillars().stream().anyMatch(p -> p.getId().equals(targetPillarId))))
                    .findFirst().ifPresent(slots::add);
        }

        // Tập hợp active rentals
        List<SlotRental> activeRentals = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (GardenSlot slot : slots) {
            activeRentals.addAll(slotRentalRepository.findActiveRentals(slot.getId(), now));
        }
        if (pillar != null) {
            List<SlotRental> pillarRentals = slotRentalRepository.findActiveRentalsByPillarIds(List.of(pillar.getId()), now);
            for (SlotRental pr : pillarRentals) {
                if (!activeRentals.contains(pr)) {
                    activeRentals.add(pr);
                    if (pr.getGardenSlot() != null && !slots.contains(pr.getGardenSlot())) {
                        slots.add(pr.getGardenSlot());
                    }
                }
            }
        }
        if (activeRentals.isEmpty()) {
            for (GardenSlot slot : slots) {
                List<SlotRental> allInSlot = slotRentalRepository.findByGardenSlotId(slot.getId());
                for (SlotRental r : allInSlot) {
                    if (r.getStatus() == ERentalStatus.ACTIVE && (r.getEndTime() == null || r.getEndTime().isAfter(now))) {
                        activeRentals.add(r);
                    }
                }
            }
        }

        boolean hasActiveRentals = false;
        boolean autoSprayTriggered = false;

        for (SlotRental rental : activeRentals) {
            hasActiveRentals = true;
            GardenSlot slot = rental.getGardenSlot() != null ? rental.getGardenSlot() : (slots.isEmpty() ? null : slots.get(0));
            User customer = rental.getUser();
            Tree tree = rental.getTree() != null ? rental.getTree() : (pillar != null ? pillar.getDefaultTree() : null);

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
                String pillarCodeStr = pillar != null ? pillar.getPillarCode() : deviceId;
                String slotNumberStr = slot != null ? slot.getSlotNumber() : "N/A";
                Long slotIdVal = slot != null ? slot.getId() : null;

                // 1. Xác định nhân viên trực ca / phụ trách theo lịch trực trong ngày để giao Task và gửi thông báo
                java.util.Map<Long, User> targetStaffMap = new java.util.HashMap<>();
                User assignedStaff = null;
                Long locId = (slot != null && slot.getLocation() != null) ? slot.getLocation().getId()
                        : (pillar != null && pillar.getLocation() != null ? pillar.getLocation().getId() : null);

                if (staffScheduleRepository != null) {
                    List<StaffSchedule> activeToday = staffScheduleRepository.findActiveSchedulesOnDate(java.time.LocalDate.now());
                    // Ưu tiên 1: Nhân viên có ca trực hôm nay được gán đúng ô vườn này
                    if (slotIdVal != null) {
                        for (StaffSchedule sch : activeToday) {
                            if (sch.getGardenSlot() != null && sch.getGardenSlot().getId().equals(slotIdVal) && sch.getStaff() != null) {
                                targetStaffMap.put(sch.getStaff().getId(), sch.getStaff());
                                if (assignedStaff == null) {
                                    assignedStaff = sch.getStaff();
                                }
                            }
                        }
                    }
                    // Ưu tiên 2: Nhân viên trực ca hôm nay thuộc cơ sở này
                    if (assignedStaff == null && locId != null) {
                        for (StaffSchedule sch : activeToday) {
                            if (sch.getLocation() != null && sch.getLocation().getId().equals(locId) && sch.getStaff() != null) {
                                targetStaffMap.put(sch.getStaff().getId(), sch.getStaff());
                                if (assignedStaff == null) {
                                    assignedStaff = sch.getStaff();
                                }
                            }
                        }
                    }
                }

                // Ưu tiên 3: Nếu hôm nay chưa có staff trong lịch trực, tìm nhân viên ROLE_GARDEN_STAFF thuộc cơ sở này
                if (assignedStaff == null && locId != null) {
                    List<User> locationStaff = userRepository.findByRoleNameAndLocation(ERole.ROLE_GARDEN_STAFF, locId);
                    if (!locationStaff.isEmpty()) {
                        for (User st : locationStaff) {
                            targetStaffMap.put(st.getId(), st);
                        }
                        assignedStaff = locationStaff.get(0);
                    }
                }

                // Ưu tiên 4: Toàn bộ ROLE_GARDEN_STAFF trong hệ thống
                if (assignedStaff == null) {
                    List<User> allStaff = userRepository.findByRoleName(ERole.ROLE_GARDEN_STAFF);
                    if (!allStaff.isEmpty()) {
                        for (User st : allStaff) {
                            targetStaffMap.put(st.getId(), st);
                        }
                        assignedStaff = allStaff.get(0);
                    }
                }

                if (assignedStaff != null) {
                    targetStaffMap.put(assignedStaff.getId(), assignedStaff);
                }

                // 2. Cooldown / Anti-Spam Check (5 phút):
                LocalDateTime cooldownCutoff = LocalDateTime.now().minusMinutes(5);
                List<Alert> recentAlerts = alertRepository.findActiveOrRecentAlerts(
                        pillar != null ? pillar.getId() : null,
                        slotIdVal,
                        sensorType.name(),
                        List.of(EAlertStatus.PENDING, EAlertStatus.IN_PROGRESS),
                        cooldownCutoff
                );

                if (!recentAlerts.isEmpty()) {
                    Alert existingAlert = recentAlerts.get(0);
                    existingAlert.setActualValue(value);
                    alertRepository.save(existingAlert);

                    // Kiểm tra xem đã có GardeningTask khẩn cấp cho trụ/ô này chưa
                    if (slot != null) {
                        List<GardeningTask> pendingTasks = gardeningTaskRepository.findByTargetSlotIdAndTaskTypeOrderByCreatedAtDesc(slot.getId(), ETaskType.MAINTENANCE);
                        GardeningTask existingTask = pendingTasks.stream()
                                .filter(t -> t.getStatus() == ETaskStatus.PENDING && t.getTaskName() != null && t.getTaskName().contains(pillarCodeStr))
                                .findFirst().orElse(null);

                        if (existingTask == null) {
                            createEmergencyTask(slot, pillar, pillarCodeStr, sensorType, value, unit, effectiveMin, effectiveMax, treeName, assignedStaff);
                            sendStaffAlertNotifications(targetStaffMap, pillarCodeStr, slot, treeName, sensorType, value, unit, effectiveMin, effectiveMax);
                        } else if (existingTask.getAssignedStaff() == null && assignedStaff != null) {
                            existingTask.setAssignedStaff(assignedStaff);
                            gardeningTaskRepository.save(existingTask);
                            sendStaffAlertNotifications(targetStaffMap, pillarCodeStr, slot, treeName, sensorType, value, unit, effectiveMin, effectiveMax);
                        }
                    }

                    // Vẫn hỗ trợ tự động tưới nước nếu độ ẩm đất < ngưỡng tối thiểu
                    if (sensorType == ESensorType.SOIL_MOISTURE && value < effectiveMin && !autoSprayTriggered) {
                        String autoReason = String.format("Tự động tưới: Độ ẩm đất %.2f%% < ngưỡng tối thiểu %.2f%% của %s tại ô %s (Trụ %s)",
                                value, effectiveMin, treePrefix, slotNumberStr, pillarCodeStr);
                        boolean autoSprayed = pumpService.triggerAutoSpray(autoReason);
                        if (autoSprayed) {
                            autoSprayTriggered = true;
                        }
                    }
                    continue;
                }

                // 3. Tạo bản ghi Alert mới gắn chặt với CÂY TRỒNG, Ô ĐẤT và TRỤ IOT
                Alert alert = new Alert();
                alert.setAlertType(sensorType.name());
                alert.setDescription(String.format("Cảm biến %s cho %sô %s (Trụ %s) ghi nhận giá trị %.2f %s, nằm ngoài ngưỡng sinh trưởng (%.2f - %.2f)",
                        sensorType.getDescription(), treePrefix, slotNumberStr, pillarCodeStr, value, unit, effectiveMin, effectiveMax));
                alert.setStatus(EAlertStatus.PENDING);
                alert.setThresholdValue((effectiveMin + effectiveMax) / 2.0);
                alert.setActualValue(value);
                alert.setSensorType(sensorType.name());
                alert.setPillar(pillar);
                alert.setGardenSlot(slot);
                alert.setTree(tree);
                alert.setCreatedAt(LocalDateTime.now());
                Alert savedAlert = alertService.createAlert(alert);

                // 4. Tự động tạo nhiệm vụ khẩn cấp cho nhân viên
                if (slot != null) {
                    createEmergencyTask(slot, pillar, pillarCodeStr, sensorType, value, unit, effectiveMin, effectiveMax, treeName, assignedStaff);
                }

                // 5. Gửi thông báo cho Quản lý chi nhánh (Location Manager) và Quản lý chung (Manager)
                String managerTitle = "Cảnh báo chỉ số cảm biến cây trồng";
                String managerBody = String.format("Ô %s (%s - Trụ %s): Cảm biến %s vượt ngưỡng. Giá trị: %.2f %s (Ngưỡng: %.2f - %.2f)",
                        slotNumberStr, treeName, pillarCodeStr, sensorType.getDescription(), value, unit, effectiveMin, effectiveMax);

                if (locId != null && firebaseMessagingService != null) {
                    firebaseMessagingService.sendPushNotificationToLocation(locId, managerTitle, managerBody, "ROLE_LOCATION_MANAGER");
                    firebaseMessagingService.sendPushNotificationToLocation(locId, managerTitle, managerBody, "ROLE_MANAGER");
                }

                java.util.Set<User> managersToNotify = new java.util.LinkedHashSet<>();
                if (locId != null) {
                    managersToNotify.addAll(userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, locId));
                }
                managersToNotify.addAll(userRepository.findByRoleName(ERole.ROLE_MANAGER));

                for (User manager : managersToNotify) {
                    if (notificationService != null) {
                        notificationService.createNotification(
                                manager.getId(),
                                managerTitle,
                                managerBody,
                                "IOT_ALERT",
                                savedAlert != null ? savedAlert.getId() : null,
                                "/dashboard/staff/alert-processing"
                        );
                    }
                    if (firebaseMessagingService != null && locId == null) {
                        firebaseMessagingService.sendPushNotification(manager.getId(), managerTitle, managerBody);
                    }
                }

                // 6. Gửi thông báo cho Khách hàng sở hữu cây
                if (customer != null) {
                    if (notificationService != null) {
                        notificationService.createNotification(
                                customer.getId(),
                                "Cảnh báo chỉ số cây trồng của bạn",
                                String.format("Cảnh báo: Cảm biến %s tại ô %s (%s) ghi nhận %.2f %s, nằm ngoài ngưỡng sinh trưởng (%.2f - %.2f).",
                                        sensorType.getDescription(), slotNumberStr, treeName, value, unit, effectiveMin, effectiveMax),
                                "IOT_ALERT",
                                slotIdVal,
                                "/dashboard/customer/monitoring"
                        );
                    }
                    firebaseMessagingService.sendPushNotification(
                            customer.getId(),
                            "Cảnh báo cảm biến cây trồng",
                            String.format("Ô %s (%s): Cảm biến %s đạt %.2f %s, ngoài ngưỡng sinh trưởng.",
                                    slotNumberStr, treeName, sensorType.getDescription(), value, unit)
                    );
                }

                // 7. Gửi thông báo cho Nhân viên chăm sóc
                sendStaffAlertNotifications(targetStaffMap, pillarCodeStr, slot, treeName, sensorType, value, unit, effectiveMin, effectiveMax);

                // 8. Tự động kích hoạt bơm xịt nước nếu độ ẩm đất < ngưỡng tối thiểu của cây
                if (sensorType == ESensorType.SOIL_MOISTURE && value < effectiveMin && !autoSprayTriggered) {
                    String autoReason = String.format("Tự động tưới: Độ ẩm đất %.2f%% < ngưỡng tối thiểu %.2f%% của %s tại ô %s (Trụ %s)",
                            value, effectiveMin, treePrefix, slotNumberStr, pillarCodeStr);
                    boolean autoSprayed = pumpService.triggerAutoSpray(autoReason);
                    if (autoSprayed) {
                        autoSprayTriggered = true;
                        if (notificationService != null) {
                            if (customer != null) {
                                notificationService.createNotification(
                                        customer.getId(),
                                        "Hệ thống tự động tưới cây (Smart Irrigation)",
                                        String.format("Hệ thống IoT vừa tự động kích hoạt máy bơm xịt nước cho ô %s (%s) do độ ẩm đất giảm thấp (%.2f%% < %.2f%%).",
                                                slotNumberStr, treeName, value, effectiveMin),
                                        "IOT_AUTO_WATERING",
                                        slotIdVal,
                                        "/dashboard/customer/monitoring"
                                );
                            }
                            for (User staff : targetStaffMap.values()) {
                                notificationService.createNotification(
                                        staff.getId(),
                                        "Hệ thống vừa tự động tưới nước",
                                        String.format("Máy bơm Trụ %s (Ô %s - %s) vừa được hệ thống tự động kích hoạt tưới 5 giây do độ ẩm %.2f%% < %.2f%%.",
                                                pillarCodeStr, slotNumberStr, treeName, value, effectiveMin),
                                        "IOT_AUTO_WATERING",
                                        slotIdVal,
                                        String.format("/dashboard/garden-staff/pump-control?slotId=%d&pillarCode=%s", slotIdVal != null ? slotIdVal : 0L, pillarCodeStr)
                                );
                            }
                        }
                    }
                }
            }
        }

        // Trường hợp không có hợp đồng thuê nào đang hoạt động nhưng số đo vượt ngưỡng của trụ/thiết bị
        if (!hasActiveRentals && (value < defaultMin || value > defaultMax)) {
            String pillarCode = pillar != null ? pillar.getPillarCode() : deviceId;
            GardenSlot slot = pillar != null ? pillar.getGardenSlot() : null;
            Long locId = (pillar != null && pillar.getLocation() != null) ? pillar.getLocation().getId()
                    : (slot != null && slot.getLocation() != null ? slot.getLocation().getId() : null);

            // Cooldown check (5 phút)
            LocalDateTime cooldownCutoff = LocalDateTime.now().minusMinutes(5);
            List<Alert> recentAlerts = alertRepository.findActiveOrRecentAlerts(
                    pillar != null ? pillar.getId() : null,
                    slot != null ? slot.getId() : null,
                    sensorType.name(),
                    List.of(EAlertStatus.PENDING, EAlertStatus.IN_PROGRESS),
                    cooldownCutoff
            );

            if (recentAlerts.isEmpty()) {
                Alert alert = new Alert();
                alert.setAlertType(sensorType.name());
                alert.setDescription(String.format("Cảm biến %s trên thiết bị/trụ %s ghi nhận giá trị %.2f %s, nằm ngoài ngưỡng cho phép (%.2f - %.2f)",
                        sensorType.getDescription(), pillarCode, value, unit, defaultMin, defaultMax));
                alert.setStatus(EAlertStatus.PENDING);
                alert.setThresholdValue((defaultMin + defaultMax) / 2.0);
                alert.setActualValue(value);
                alert.setSensorType(sensorType.name());
                alert.setPillar(pillar);
                alert.setGardenSlot(slot);
                alert.setCreatedAt(LocalDateTime.now());
                Alert savedAlert = alertService.createAlert(alert);

                // Xác định staff và giao task
                java.util.Map<Long, User> targetStaffMap = new java.util.HashMap<>();
                User assignedStaff = null;
                if (locId != null) {
                    List<StaffSchedule> activeToday = staffScheduleRepository != null
                            ? staffScheduleRepository.findActiveSchedulesOnDate(LocalDate.now())
                            : List.of();
                    for (StaffSchedule sch : activeToday) {
                        if (sch.getLocation() != null && sch.getLocation().getId().equals(locId) && sch.getStaff() != null) {
                            targetStaffMap.put(sch.getStaff().getId(), sch.getStaff());
                            if (assignedStaff == null) {
                                assignedStaff = sch.getStaff();
                            }
                        }
                    }
                    if (assignedStaff == null) {
                        List<User> locStaff = userRepository.findByRoleNameAndLocation(ERole.ROLE_GARDEN_STAFF, locId);
                        if (!locStaff.isEmpty()) {
                            assignedStaff = locStaff.get(0);
                            for (User st : locStaff) targetStaffMap.put(st.getId(), st);
                        }
                    }
                }
                if (assignedStaff == null) {
                    List<User> allStaff = userRepository.findByRoleName(ERole.ROLE_GARDEN_STAFF);
                    if (!allStaff.isEmpty()) {
                        assignedStaff = allStaff.get(0);
                        for (User st : allStaff) targetStaffMap.put(st.getId(), st);
                    }
                }

                if (slot != null) {
                    createEmergencyTask(slot, pillar, pillarCode, sensorType, value, unit, defaultMin, defaultMax, "Chưa thuê", assignedStaff);
                    sendStaffAlertNotifications(targetStaffMap, pillarCode, slot, "Chưa thuê", sensorType, value, unit, defaultMin, defaultMax);
                }

                // Gửi thông báo cho Quản lý chi nhánh và Quản lý chung
                if (notificationService != null) {
                    java.util.Set<User> managersToNotify = new java.util.LinkedHashSet<>();
                    if (locId != null) {
                        managersToNotify.addAll(userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, locId));
                    }
                    managersToNotify.addAll(userRepository.findByRoleName(ERole.ROLE_MANAGER));
                    for (User manager : managersToNotify) {
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
            }

            // Tự động kích hoạt bơm xịt nước nếu độ ẩm đất < ngưỡng tối thiểu
            if (sensorType == ESensorType.SOIL_MOISTURE && value < defaultMin) {
                String autoReason = String.format("Tự động tưới: Độ ẩm đất %.2f%% < ngưỡng tối thiểu %.2f%% tại thiết bị/trụ %s",
                        value, defaultMin, pillarCode);
                pumpService.triggerAutoSpray(autoReason);
            }
        }
    }

    private GardeningTask createEmergencyTask(GardenSlot slot, Pillar pillar, String pillarCodeStr, ESensorType sensorType,
                                              Double value, String unit, double min, double max, String treeName, User assignedStaff) {
        String emergencyTaskName = String.format("Khẩn cấp: Xử lý cảnh báo %s - Trụ %s", sensorType.getDescription(), pillarCodeStr);
        GardeningTask emergencyTask = new GardeningTask();
        emergencyTask.setTaskName(emergencyTaskName);
        emergencyTask.setDescription(String.format("Kiểm tra khẩn cấp ô %s (Trụ %s, %s). Cảm biến %s ghi nhận %.2f %s (Ngưỡng: %.2f - %.2f). Yêu cầu kiểm tra xử lý.",
                slot != null ? slot.getSlotNumber() : "N/A", pillarCodeStr, treeName, sensorType.getDescription(), value, unit, min, max));
        emergencyTask.setStatus(ETaskStatus.PENDING);
        emergencyTask.setTaskType(ETaskType.MAINTENANCE);
        emergencyTask.setTargetSlot(slot);
        emergencyTask.setPillarCodes(pillarCodeStr);
        emergencyTask.setAssignedStaff(assignedStaff);
        if (slotRentalRepository != null) {
            LocalDateTime nowTs = LocalDateTime.now();
            List<SlotRental> activeRentals = new java.util.ArrayList<>();
            if (pillar != null && pillar.getId() != null) {
                activeRentals.addAll(slotRentalRepository.findActiveRentalsByPillarIds(List.of(pillar.getId()), nowTs));
            }
            if (activeRentals.isEmpty() && slot != null && slot.getId() != null) {
                activeRentals.addAll(slotRentalRepository.findActiveRentals(slot.getId(), nowTs));
            }
            activeRentals.stream()
                    .filter(r -> r.getStatus() == ERentalStatus.ACTIVE && r.getUser() != null)
                    .findFirst()
                    .ifPresent(r -> emergencyTask.setRequestedBy(r.getUser()));
        }
        emergencyTask.setCreatedAt(LocalDateTime.now());
        return gardeningTaskRepository.save(emergencyTask);
    }

    private void sendStaffAlertNotifications(Map<Long, User> targetStaffMap, String pillarCodeStr, GardenSlot slot,
                                            String treeName, ESensorType sensorType, Double value, String unit,
                                            double effectiveMin, double effectiveMax) {
        boolean isSoilMoistureLow = (sensorType == ESensorType.SOIL_MOISTURE && value < effectiveMin);
        String slotNumber = slot != null ? slot.getSlotNumber() : "N/A";
        Long slotId = slot != null ? slot.getId() : null;

        for (User staff : targetStaffMap.values()) {
            if (notificationService != null) {
                if (isSoilMoistureLow) {
                    String wateringTitle = String.format("Yêu cầu tưới nước: Trụ %s (Ô %s)", pillarCodeStr, slotNumber);
                    String wateringBody = String.format("Độ ẩm đất tại Trụ %s (Ô %s - %s) giảm còn %.2f%%, dưới ngưỡng an toàn %.2f%%. Vui lòng kiểm tra và kích hoạt tưới nước.",
                            pillarCodeStr, slotNumber, treeName, value, effectiveMin);
                    String actionUrl = String.format("/dashboard/garden-staff/pump-control?slotId=%d&pillarCode=%s", slotId != null ? slotId : 0L, pillarCodeStr);
                    notificationService.createNotification(
                            staff.getId(),
                            wateringTitle,
                            wateringBody,
                            "WATERING_REQUIRED",
                            slotId,
                            actionUrl
                    );
                } else {
                    notificationService.createNotification(
                            staff.getId(),
                            "Cảnh báo chỉ số cảm biến (Cần xử lý)",
                            String.format("Cảnh báo: Cảm biến %s tại ô %s (%s) ghi nhận %.2f %s, ngoài ngưỡng (%.2f - %.2f). Đã tự động tạo task khẩn cấp.",
                                    sensorType.getDescription(), slotNumber, treeName, value, unit, effectiveMin, effectiveMax),
                            "IOT_ALERT",
                            slotId,
                            "/dashboard/garden-staff"
                    );
                }
            }
            if (firebaseMessagingService != null) {
                String pushTitle = isSoilMoistureLow
                        ? String.format("Cần tưới nước: Trụ %s (Ô %s)", pillarCodeStr, slotNumber)
                        : "Cần kiểm tra: Cảnh báo cảm biến";
                String pushBody = isSoilMoistureLow
                        ? String.format("Độ ẩm đất Trụ %s (Ô %s) thấp (%.2f%% < %.2f%%). Nhấn để kích hoạt bơm.", pillarCodeStr, slotNumber, value, effectiveMin)
                        : String.format("Ô %s (%s): Cảm biến %s bất thường (%.2f %s)", slotNumber, treeName, sensorType.getDescription(), value, unit);
                firebaseMessagingService.sendPushNotification(staff.getId(), pushTitle, pushBody);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SensorReadingResponseDTO> getLatestReadings(String identifier) {
        String targetCode = (identifier != null && !identifier.isBlank()) ? identifier.trim() : "arduino-greenhouse-01";

        Optional<Pillar> pillarOpt = pillarRepository.findByPillarCode(targetCode);
        if (pillarOpt.isEmpty()) {
            pillarOpt = equipmentRepository.findBySerialNumber(targetCode).map(Equipment::getPillar);
        }
        if (pillarOpt.isEmpty()) {
            pillarOpt = gardenSlotRepository.findBySlotNumber(targetCode).map(GardenSlot::getPillar);
        }

        if (pillarOpt.isEmpty()) {
            List<SensorReading> recentByDevice = sensorReadingRepository.findByDeviceIdOrderByRecordedAtDesc(targetCode, PageRequest.of(0, 50));
            if (recentByDevice.isEmpty()) {
                return new ArrayList<>();
            }
            Map<ESensorType, SensorReading> latestByType = new LinkedHashMap<>();
            for (SensorReading r : recentByDevice) {
                if (r.getSensorType() != null) {
                    latestByType.putIfAbsent(r.getSensorType(), r);
                }
            }
            return latestByType.values().stream().map(SensorReadingResponseDTO::fromEntity).toList();
        }

        Long pillarId = pillarOpt.get().getId();

        // 1-query optimization: Lay toi da 50 ban ghi moi nhat cua tru va gom nhom theo loai cam bien
        List<SensorReading> recent = sensorReadingRepository.findTop50ByPillarIdOrderByRecordedAtDesc(pillarId);
        Map<ESensorType, SensorReading> latestByType = new LinkedHashMap<>();
        for (SensorReading r : recent) {
            if (r.getSensorType() != null) {
                latestByType.putIfAbsent(r.getSensorType(), r);
            }
        }

        // Neu van con loai cam bien chua xuat hien trong 50 ban ghi gan nhat, fallback rieng cho loai do
        if (latestByType.size() < ESensorType.values().length) {
            for (ESensorType type : ESensorType.values()) {
                if (!latestByType.containsKey(type)) {
                    sensorReadingRepository.findFirstByPillarIdAndSensorTypeOrderByRecordedAtDesc(pillarId, type)
                            .ifPresent(r -> latestByType.put(type, r));
                }
            }
        }

        return latestByType.values().stream()
                .map(SensorReadingResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SensorReadingResponseDTO> getHistory(String identifier, ESensorType sensorType, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        String targetCode = (identifier != null && !identifier.isBlank()) ? identifier.trim() : "arduino-greenhouse-01";

        Optional<Pillar> pillarOpt = pillarRepository.findByPillarCode(targetCode);
        if (pillarOpt.isEmpty()) {
            pillarOpt = equipmentRepository.findBySerialNumber(targetCode).map(Equipment::getPillar);
        }
        if (pillarOpt.isEmpty()) {
            pillarOpt = gardenSlotRepository.findBySlotNumber(targetCode).map(GardenSlot::getPillar);
        }

        if (pillarOpt.isEmpty()) {
            List<SensorReading> readings;
            if (sensorType != null) {
                readings = sensorReadingRepository.findByDeviceIdAndSensorTypeOrderByRecordedAtDesc(targetCode, sensorType, PageRequest.of(0, safeLimit));
            } else {
                readings = sensorReadingRepository.findByDeviceIdOrderByRecordedAtDesc(targetCode, PageRequest.of(0, safeLimit));
            }
            return readings.stream()
                    .map(SensorReadingResponseDTO::fromEntity)
                    .toList();
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
