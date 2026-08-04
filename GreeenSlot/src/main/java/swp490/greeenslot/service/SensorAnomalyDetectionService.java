package swp490.greeenslot.service;

import swp490.greeenslot.dto.SensorAnomalyDTO;

import java.time.Instant;
import java.util.List;

public interface SensorAnomalyDetectionService {
    
    List<SensorAnomalyDTO> detectAnomalies(String deviceId);
    
    List<SensorAnomalyDTO> detectAnomaliesByTimeRange(String deviceId, Instant startTime, Instant endTime);
    
    SensorAnomalyDTO checkSensorHealth(String deviceId, String sensorType);
}
