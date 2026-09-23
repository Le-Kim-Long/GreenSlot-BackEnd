package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class GoogleLoginRequestDTO {

    @NotBlank(message = "Google ID Token không được để trống")
    @Schema(description = "Google OAuth2 ID Token received from Google Sign-In or Firebase Auth. Auto-registration is enabled - new users will be created automatically.", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
    private String idToken;

    @Schema(description = "Chế độ: 'login' (mặc định) hoặc 'register'", example = "login")
    private String mode = "login";

    public GoogleLoginRequestDTO() {
    }

    public GoogleLoginRequestDTO(String idToken) {
        this.idToken = idToken;
        this.mode = "login";
    }

    public GoogleLoginRequestDTO(String idToken, String mode) {
        this.idToken = idToken;
        this.mode = mode;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
