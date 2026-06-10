package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ForgotPasswordResponseDTO {

    private String message;

    @Schema(description = "Only returned when email is not configured (local dev). Copy this into reset-password.")
    private String resetToken;

    public ForgotPasswordResponseDTO(String message, String resetToken) {
        this.message = message;
        this.resetToken = resetToken;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }
}
