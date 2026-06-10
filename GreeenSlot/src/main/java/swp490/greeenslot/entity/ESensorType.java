package swp490.greeenslot.entity;

/**
 * Danh sach loai cam bien ho tro.
 *
 * THEM_CAM_BIEN_MOI:
 * 1. Them enum value tai day (vd: TEMPERATURE, HUMIDITY, LIGHT_INTENSITY)
 * 2. Cap nhat ArduinoSensorDataRequestDTO neu can alias JSON ngan cho Arduino
 * 3. (Tuy chon) them rule validate trong SensorReadingServiceImpl.validateReading()
 */
public enum ESensorType {

    SOIL_MOISTURE("soil_moisture", "%", "Do am dat"),
    PH("ph", "pH", "Do pH dat");

    private final String code;
    private final String defaultUnit;
    private final String description;

    ESensorType(String code, String defaultUnit, String description) {
        this.code = code;
        this.defaultUnit = defaultUnit;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public String getDescription() {
        return description;
    }

    public static ESensorType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Sensor type is required.");
        }
        String normalized = code.trim().toUpperCase().replace('-', '_');
        for (ESensorType type : values()) {
            if (type.name().equals(normalized) || type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported sensor type: " + code
                + ". Add it to ESensorType.java first.");
    }
}
