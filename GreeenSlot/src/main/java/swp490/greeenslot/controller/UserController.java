package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.MessageResponseDTO;
import swp490.greeenslot.dto.ProfileResponseDTO;
import swp490.greeenslot.dto.UserProfileUpdateDTO;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.service.UserService;

import java.security.Principal;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profile", description = "APIs for user profile management")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile", description = "Retrieves the safe identity profile of the currently authenticated user.")
    public ResponseEntity<?> getProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(new MessageResponseDTO("Unauthorized"));
        }
        try {
            ProfileResponseDTO profile = userService.getProfile(principal.getName());
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponseDTO(e.getMessage()));
        }
    }

    @PatchMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update user profile", description = "Updates full name, phone, and address of the currently authenticated user.")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserProfileUpdateDTO dto, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(new MessageResponseDTO("Unauthorized"));
        }
        try {
            User updatedUser = userService.updateProfile(principal.getName(), dto);
            return ResponseEntity.ok(new MessageResponseDTO("Profile updated successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponseDTO(e.getMessage()));
        }
    }
}
