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
import swp490.greeenslot.dto.JwtResponseDTO;
import swp490.greeenslot.dto.SignupRequestDTO;
import swp490.greeenslot.dto.VerifyOtpRequestDTO;
import swp490.greeenslot.entity.ERole;
import swp490.greeenslot.entity.Role;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.RoleRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.impl.AuthServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplOtpTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = new Role();
        customerRole.setName(ERole.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("registerUser: Creates user with enabled=false and sends OTP email")
    void testRegisterUser_SetsEnabledFalse_SendsOtp() {
        SignupRequestDTO req = new SignupRequestDTO();
        req.setUsername("newcustomer");
        req.setEmail("newcustomer@gmail.com");
        req.setPassword("Password123!");
        req.setFullName("Nguyen Van New");

        when(userRepository.findByEmail("newcustomer@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("newcustomer")).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));

        authService.registerUser(req);

        verify(userRepository, times(1)).save(argThat(u -> 
            !u.isEnabled() &&
            u.getVerificationOtp() != null &&
            u.getVerificationOtp().length() == 6 &&
            u.getOtpExpiry() != null &&
            u.getOtpExpiry().isAfter(java.time.Instant.now())
        ));
        verify(emailService, times(1)).sendRegistrationOtpEmail(eq("newcustomer@gmail.com"), anyString(), eq("Nguyen Van New"));
    }

    @Test
    @DisplayName("verifyRegistrationOtp: Successfully activates user with valid OTP and returns JWT token")
    void testVerifyRegistrationOtp_Success() {
        User pendingUser = new User();
        pendingUser.setId(10L);
        pendingUser.setUsername("pendinguser");
        pendingUser.setEmail("pending@gmail.com");
        pendingUser.setEnabled(false);
        pendingUser.setVerificationOtp("123456");
        pendingUser.setOtpExpiry(java.time.Instant.now().plus(java.time.Duration.ofMinutes(5)));
        pendingUser.setRoles(Set.of(customerRole));

        when(userRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.of(pendingUser));
        when(jwtUtils.generateTokenFromUsername("pendinguser")).thenReturn("mock_jwt_token");

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO("pending@gmail.com", "123456");
        JwtResponseDTO res = authService.verifyRegistrationOtp(dto);

        assertNotNull(res);
        assertEquals("mock_jwt_token", res.getToken());
        assertTrue(pendingUser.isEnabled());
        assertNull(pendingUser.getVerificationOtp());
        assertNull(pendingUser.getOtpExpiry());
        verify(userRepository, times(1)).save(pendingUser);
    }

    @Test
    @DisplayName("verifyRegistrationOtp: Throws exception when OTP is incorrect")
    void testVerifyRegistrationOtp_InvalidOtp_ThrowsException() {
        User pendingUser = new User();
        pendingUser.setId(10L);
        pendingUser.setUsername("pendinguser");
        pendingUser.setEmail("pending@gmail.com");
        pendingUser.setEnabled(false);
        pendingUser.setVerificationOtp("654321");
        pendingUser.setOtpExpiry(java.time.Instant.now().plus(java.time.Duration.ofMinutes(5)));

        when(userRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.of(pendingUser));

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO("pending@gmail.com", "000000");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyRegistrationOtp(dto);
        });

        assertEquals("Mã xác thực OTP không chính xác. Vui lòng kiểm tra lại!", ex.getMessage());
    }

    @Test
    @DisplayName("verifyRegistrationOtp: Throws exception when OTP is expired")
    void testVerifyRegistrationOtp_ExpiredOtp_ThrowsException() {
        User pendingUser = new User();
        pendingUser.setId(10L);
        pendingUser.setUsername("pendinguser");
        pendingUser.setEmail("pending@gmail.com");
        pendingUser.setEnabled(false);
        pendingUser.setVerificationOtp("123456");
        pendingUser.setOtpExpiry(java.time.Instant.now().minus(java.time.Duration.ofMinutes(1))); // Expired

        when(userRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.of(pendingUser));

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO("pending@gmail.com", "123456");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyRegistrationOtp(dto);
        });

        assertEquals("Mã OTP đã hết hạn (quá 10 phút). Vui lòng bấm 'Gửi lại mã'!", ex.getMessage());
    }

    @Test
    @DisplayName("resendRegistrationOtp: Successfully generates new OTP and sends email")
    void testResendRegistrationOtp_Success() {
        User pendingUser = new User();
        pendingUser.setId(10L);
        pendingUser.setUsername("pendinguser");
        pendingUser.setEmail("pending@gmail.com");
        pendingUser.setFullName("Nguyen Pending");
        pendingUser.setEnabled(false);
        pendingUser.setVerificationOtp("111111");

        when(userRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.of(pendingUser));

        authService.resendRegistrationOtp("pending@gmail.com");

        verify(userRepository, times(1)).save(argThat(u -> 
            !u.isEnabled() &&
            u.getVerificationOtp() != null &&
            !u.getVerificationOtp().equals("111111")
        ));
        verify(emailService, times(1)).sendRegistrationOtpEmail(eq("pending@gmail.com"), anyString(), eq("Nguyen Pending"));
    }
}
