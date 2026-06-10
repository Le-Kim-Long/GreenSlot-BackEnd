package swp490.greeenslot.dto;

import java.util.List;

public class ArduinoSensorDataResponseDTO {

    private String message;
    private String deviceId;
    private int savedCount;
    private List<SensorReadingResponseDTO> readings;

    public ArduinoSensorDataResponseDTO(String message, String deviceId, int savedCount,
                                        List<SensorReadingResponseDTO> readings) {
        this.message = message;
        this.deviceId = deviceId;
        this.savedCount = savedCount;
        this.readings = readings;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getSavedCount() {
        return savedCount;
    }

    public void setSavedCount(int savedCount) {
        this.savedCount = savedCount;
    }

    public List<SensorReadingResponseDTO> getReadings() {
        return readings;
    }

    public void setReadings(List<SensorReadingResponseDTO> readings) {
        this.readings = readings;
    }
}
