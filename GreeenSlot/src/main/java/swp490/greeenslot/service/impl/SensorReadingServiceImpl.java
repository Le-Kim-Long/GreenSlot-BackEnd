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
            saved.add(sensorReadingRepository.save(reading));
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
        Optional<SensorThreshold> thresholdOpt = sensorThresholdRepository.findByDeviceIdAndSensorType(deviceId, sensorTypeStr);
        if (thresholdOpt.isPresent()) {
            SensorThreshold threshold = thresholdOpt.get();
            if (value < threshold.getMinValue() || value > threshold.getMaxValue()) {
                // Threshold violation detected!
                Optional<Pillar> pillarOpt = pillarRepository.findByPillarCode(deviceId);
                if (pillarOpt.isPresent()) {
                    Pillar pillar = pillarOpt.get();
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
                            }
                        }
                    }
                }
            }
        }

        SensorReadingResponseDTO responseDTO = SensorReadingResponseDTO.fromEntity(savedReading);
        return new ArduinoSensorDataResponseDTO(
                "Device telemetry saved successfully.",
                deviceId,
                1,
                List.of(responseDTO)
        );
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
}
