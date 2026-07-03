package swp490.greeenslot.service;

import swp490.greeenslot.dto.ArduinoSensorDataRequestDTO;
import swp490.greeenslot.dto.ArduinoSensorDataResponseDTO;
import swp490.greeenslot.dto.SensorReadingResponseDTO;
import swp490.greeenslot.entity.ESensorType;

import swp490.greeenslot.dto.DeviceTelemetryRequestDTO;

import java.util.List;

public interface SensorReadingService {

    ArduinoSensorDataResponseDTO saveArduinoData(String apiKey, ArduinoSensorDataRequestDTO request);

    ArduinoSensorDataResponseDTO saveDeviceTelemetry(String apiKey, DeviceTelemetryRequestDTO request);

    List<SensorReadingResponseDTO> getLatestReadings(String deviceId);

    List<SensorReadingResponseDTO> getHistory(String deviceId, ESensorType sensorType, int limit);
}
