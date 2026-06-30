package swp490.greeenslot.dto;

public class ForgotPasswordResponseDTO {

    private String message;

    public ForgotPasswordResponseDTO(String message) {
        this.message = message;
    }

    public ForgotPasswordResponseDTO(String message, String resetToken) {
        // Keep 2-arg constructor for backwards compatibility, ignoring token
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
