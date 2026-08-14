package swp490.greeenslot.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.ForgotPasswordRequestDTO;
import swp490.greeenslot.dto.ForgotPasswordResponseDTO;
import swp490.greeenslot.dto.JwtResponseDTO;
import swp490.greeenslot.dto.LoginRequestDTO;
import swp490.greeenslot.dto.MessageResponseDTO;
import swp490.greeenslot.dto.ResetPasswordRequestDTO;
import swp490.greeenslot.dto.SignupRequestDTO;
import swp490.greeenslot.service.AuthService;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;
    
    @Autowired
    swp490.greeenslot.service.RateLimiterService rateLimiterService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest, jakarta.servlet.http.HttpServletRequest request) {
        if (!rateLimiterService.isAllowed(request.getRemoteAddr())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponseDTO("Too many login attempts. Please try again later."));
        }
        JwtResponseDTO jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponseDTO> register(@Valid @RequestBody SignupRequestDTO signUpRequest) {
        authService.registerUser(signUpRequest);
        return ResponseEntity.ok(new MessageResponseDTO("User registered successfully!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO passwordRequest, jakarta.servlet.http.HttpServletRequest request) {
        if (!rateLimiterService.isAllowed(request.getRemoteAddr())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponseDTO("Too many requests. Please try again later."));
        }
        return ResponseEntity.ok(authService.forgotPassword(passwordRequest));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponseDTO("Password has been reset successfully!"));
    }
}
