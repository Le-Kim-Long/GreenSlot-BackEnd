package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class GoogleLoginRequestDTO {

    @NotBlank(message = "Google ID token is required")
    @Schema(description = "Google OAuth2 ID Token received from Google Sign-In or Firebase Auth. Auto-registration is enabled - new users will be created automatically.", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
    private String idToken;

    public GoogleLoginRequestDTO() {
    }

    public GoogleLoginRequestDTO(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
