package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp490.greeenslot.dto.SensorAnomalyDTO;
import swp490.greeenslot.entity.ESensorType;
import swp490.greeenslot.entity.SensorReading;
import swp490.greeenslot.entity.SensorThreshold;
import swp490.greeenslot.repository.SensorReadingRepository;
import swp490.greeenslot.repository.SensorThresholdRepository;
import swp490.greeenslot.service.SensorAnomalyDetectionService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SensorAnomalyDetectionServiceImpl implements SensorAnomalyDetectionService {

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private SensorThresholdRepository sensorThresholdRepository;

    @Override
    public List<SensorAnomalyDTO> detectAnomalies(String deviceId) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(3600); // Last hour

        return detectAnomaliesByTimeRange(deviceId, startTime, endTime);
    }

    @Override
    public List<SensorAnomalyDTO> detectAnomaliesByTimeRange(String deviceId, Instant startTime, Instant endTime) {
        List<SensorAnomalyDTO> anomalies = new ArrayList<>();

        // Get all sensor types for this device
        List<SensorReading> readings = sensorReadingRepository.findByDeviceIdAndTimeRange(
                deviceId, startTime, endTime);

        // Group by sensor type and analyze
        for (ESensorType sensorType : ESensorType.values()) {
            List<SensorReading> typeReadings = readings.stream()
                    .filter(r -> r.getSensorType() == sensorType)
                    .toList();

            if (typeReadings.isEmpty()) {
                continue;
            }

            // Calculate expected value (average of recent readings)
            double expectedValue = typeReadings.stream()
                    .mapToDouble(SensorReading::getValue)
                    .average()
                    .orElse(0.0);

            // Find readings with significant deviation
            for (SensorReading reading : typeReadings) {
                double deviation = Math.abs(reading.getValue() - expectedValue);
                double deviationPercent = (deviation / expectedValue) * 100;

                // Consider it an anomaly if deviation > 30%
                if (deviationPercent > 30) {
                    SensorAnomalyDTO anomaly = new SensorAnomalyDTO();
                    anomaly.setDeviceId(deviceId);
                    anomaly.setSensorType(sensorType.name());
                    anomaly.setCurrentValue(reading.getValue());
                    anomaly.setExpectedValue(expectedValue);
                    anomaly.setDeviationPercent(deviationPercent);
                    anomaly.setDetectedAt(reading.getRecordedAt());
                    
                    // Determine severity
                    if (deviationPercent > 100) {
                        anomaly.setSeverity("CRITICAL");
                        anomaly.setRecommendation("Immediate inspection required - sensor may be malfunctioning");
                    } else if (deviationPercent > 70) {
                        anomaly.setSeverity("HIGH");
                        anomaly.setRecommendation("Schedule maintenance check soon");
                    } else if (deviationPercent > 50) {
                        anomaly.setSeverity("MEDIUM");
                        anomaly.setRecommendation("Monitor sensor readings closely");
                    } else {
                        anomaly.setSeverity("LOW");
                        anomaly.setRecommendation("Note for future reference");
                    }

                    anomaly.setDescription(String.format(
                            "Sensor %s reading %.2f deviates %.1f%% from expected %.2f",
                            sensorType.getDescription(), reading.getValue(), deviationPercent, expectedValue
                    ));

                    anomalies.add(anomaly);
                }
            }
        }

        // Sort by severity and time
        anomalies.sort(Comparator.comparing(SensorAnomalyDTO::getSeverity).reversed()
                .thenComparing(SensorAnomalyDTO::getDetectedAt).reversed());

        return anomalies;
    }

    @Override
    public SensorAnomalyDTO checkSensorHealth(String deviceId, String sensorType) {
        // Get recent readings for this sensor type
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(300); // Last 5 minutes

        List<SensorReading> readings = sensorReadingRepository.findByDeviceIdAndSensorTypeAndTimeRange(
                deviceId, ESensorType.fromCode(sensorType), startTime, endTime);

        if (readings.isEmpty()) {
            SensorAnomalyDTO anomaly = new SensorAnomalyDTO();
            anomaly.setDeviceId(deviceId);
            anomaly.setSensorType(sensorType);
            anomaly.setSeverity("UNKNOWN");
            anomaly.setDescription("No recent readings available");
            anomaly.setRecommendation("Check if sensor is transmitting data");
            anomaly.setDetectedAt(Instant.now());
            return anomaly;
        }

        // Check for threshold configuration
        Optional<SensorThreshold> thresholdOpt = sensorThresholdRepository
                .findByDeviceIdAndSensorType(deviceId, sensorType);

        double expectedValue = readings.stream()
                .mapToDouble(SensorReading::getValue)
                .average()
                .orElse(0.0);

        SensorAnomalyDTO healthCheck = new SensorAnomalyDTO();
        healthCheck.setDeviceId(deviceId);
        healthCheck.setSensorType(sensorType);
        healthCheck.setCurrentValue(readings.get(readings.size() - 1).getValue());
        healthCheck.setExpectedValue(expectedValue);
        healthCheck.setDeviationPercent(0.0);
        healthCheck.setDetectedAt(Instant.now());

        // Check if readings are within threshold
        if (thresholdOpt.isPresent()) {
            SensorThreshold threshold = thresholdOpt.get();
            if (healthCheck.getCurrentValue() < threshold.getMinValue() 
                    || healthCheck.getCurrentValue() > threshold.getMaxValue()) {
                healthCheck.setSeverity("HIGH");
                healthCheck.setDescription("Current reading is outside configured threshold");
                healthCheck.setRecommendation("Check sensor calibration and environment");
            } else {
                healthCheck.setSeverity("OK");
                healthCheck.setDescription("Sensor operating within normal parameters");
                healthCheck.setRecommendation("Continue normal monitoring");
            }
        } else {
            // No threshold configured, check for variance
            double variance = readings.stream()
                    .mapToDouble(r -> Math.pow(r.getValue() - expectedValue, 2))
                    .average()
                    .orElse(0.0);
            double stdDev = Math.sqrt(variance);

            if (stdDev > expectedValue * 0.2) {
                healthCheck.setSeverity("MEDIUM");
                healthCheck.setDescription("High variance in readings detected");
                healthCheck.setRecommendation("Monitor for potential sensor issues");
            } else {
                healthCheck.setSeverity("OK");
                healthCheck.setDescription("Sensor readings stable");
                healthCheck.setRecommendation("Continue normal monitoring");
            }
        }

        return healthCheck;
    }
}
