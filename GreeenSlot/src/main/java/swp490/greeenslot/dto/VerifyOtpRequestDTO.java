package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerifyOtpRequestDTO {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Schema(description = "Customer email address", example = "customer@gmail.com")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Schema(description = "6-digit OTP code sent to email", example = "123456")
    private String otp;

    public VerifyOtpRequestDTO() {
    }

    public VerifyOtpRequestDTO(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
