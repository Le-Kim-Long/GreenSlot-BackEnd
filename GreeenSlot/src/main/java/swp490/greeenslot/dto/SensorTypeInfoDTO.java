package swp490.greeenslot.dto;

import swp490.greeenslot.entity.ESensorType;

public class SensorTypeInfoDTO {

    private String name;
    private String code;
    private String unit;
    private String description;

    public SensorTypeInfoDTO() {
    }

    public SensorTypeInfoDTO(String name, String code, String unit, String description) {
        this.name = name;
        this.code = code;
        this.unit = unit;
        this.description = description;
    }

    public static SensorTypeInfoDTO from(ESensorType type) {
        return new SensorTypeInfoDTO(
                type.name(),
                type.getCode(),
                type.getDefaultUnit(),
                type.getDescription());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
