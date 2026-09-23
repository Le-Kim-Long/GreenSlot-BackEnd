package swp490.greeenslot.dto;

public class RegisterResponseDTO {
    private String message;
    private String email;
    private String demoOtp;

    public RegisterResponseDTO() {
    }

    public RegisterResponseDTO(String message, String email, String demoOtp) {
        this.message = message;
        this.email = email;
        this.demoOtp = demoOtp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDemoOtp() {
        return demoOtp;
    }

    public void setDemoOtp(String demoOtp) {
        this.demoOtp = demoOtp;
    }
}
