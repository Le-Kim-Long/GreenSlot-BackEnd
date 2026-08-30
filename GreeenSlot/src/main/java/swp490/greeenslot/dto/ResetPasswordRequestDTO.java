package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequestDTO {

    @NotBlank(message = "Mã xác thực đặt lại mật khẩu không được để trống")
    @Schema(
            description = "UUID from forgot-password response (resetToken field) or email — NOT the new password",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    )
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @Schema(description = "New password (min 6 characters)", example = "NewPass@123")
    private String newPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
