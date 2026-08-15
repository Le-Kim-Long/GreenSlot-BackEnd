package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class GoogleLoginRequestDTO {

    @NotBlank(message = "Google ID token is required")
    @Schema(description = "Google OAuth2 ID Token received from Google Sign-In", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
    private String idToken;

    @Schema(description = "Auth mode: 'login' (only allows existing registered users) or 'register' (creates new user if not exists)", example = "login")
    private String mode = "login";

    public GoogleLoginRequestDTO() {
    }

    public GoogleLoginRequestDTO(String idToken) {
        this.idToken = idToken;
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
