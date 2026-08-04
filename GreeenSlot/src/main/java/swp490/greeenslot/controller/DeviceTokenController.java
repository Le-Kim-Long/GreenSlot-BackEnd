package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.DeviceTokenDTO;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.UserRepository;

import java.security.Principal;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/device-token")
@Tag(name = "Device Token Management", description = "APIs for managing FCM device tokens for push notifications")
public class DeviceTokenController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Register or update device token for push notifications")
    public ResponseEntity<String> registerDeviceToken(
            @Valid @RequestBody DeviceTokenDTO request,
            Principal principal) {
        
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + principal.getName()));
        
        user.setDeviceToken(request.getDeviceToken());
        userRepository.save(user);
        
        return ResponseEntity.ok("Device token registered successfully");
    }

    @DeleteMapping("/unregister")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unregister device token (disable push notifications)")
    public ResponseEntity<String> unregisterDeviceToken(Principal principal) {
        
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + principal.getName()));
        
        user.setDeviceToken(null);
        userRepository.save(user);
        
        return ResponseEntity.ok("Device token unregistered successfully");
    }
}
