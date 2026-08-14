package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// PumpStatusDto.java - Dùng để trả kết quả về cho Python Bridge
public class PumpStatusDTO {

    @Schema(description = "Trạng thái của máy bơm", example = "ON", allowableValues = {"ON", "OFF"})
    private String status;

    // Default Constructor
    public PumpStatusDTO() {}

    public PumpStatusDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}