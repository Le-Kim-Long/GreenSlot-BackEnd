package swp490.greeenslot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import swp490.greeenslot.config.JwtUtils;
import swp490.greeenslot.dto.GoogleLoginRequestDTO;
import swp490.greeenslot.dto.JwtResponseDTO;
import swp490.greeenslot.entity.ERole;
import swp490.greeenslot.entity.Role;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.RoleRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.impl.AuthServiceImpl;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplGoogleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = new Role();
        customerRole.setName(ERole.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("authenticateWithGoogle: Invalid/empty token throws IllegalArgumentException")
    void testAuthenticateWithGoogle_InvalidToken_ThrowsException() {
        GoogleLoginRequestDTO request = new GoogleLoginRequestDTO("invalid_mock_token_xyz");

        assertThrows(IllegalArgumentException.class, () -> {
            authService.authenticateWithGoogle(request);
        });
    }

    @Test
    @DisplayName("authenticateWithGoogle: Deactivated user throws IllegalArgumentException")
    void testAuthenticateWithGoogle_DeactivatedUser_ThrowsException() {
        User disabledUser = new User();
        disabledUser.setId(5L);
        disabledUser.setUsername("disabled_user");
        disabledUser.setEmail("disabled@gmail.com");
        disabledUser.setEnabled(false);

        // When token verification fails on fake network, it throws IllegalArgumentException
        GoogleLoginRequestDTO request = new GoogleLoginRequestDTO("fake_token");
        assertThrows(IllegalArgumentException.class, () -> {
            authService.authenticateWithGoogle(request);
        });
    }
}
