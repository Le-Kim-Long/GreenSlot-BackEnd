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
import swp490.greeenslot.dto.RegisterResponseDTO;
import swp490.greeenslot.dto.SignupRequestDTO;
import swp490.greeenslot.dto.VerifyOtpRequestDTO;
import swp490.greeenslot.entity.ERole;
import swp490.greeenslot.entity.PendingRegistration;
import swp490.greeenslot.entity.Role;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.PendingRegistrationRepository;
import swp490.greeenslot.repository.RoleRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.impl.AuthServiceImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplOtpTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PendingRegistrationRepository pendingRegistrationRepository;

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
    @DisplayName("registerUser: Saves pending registration and sends OTP email")
    void testRegisterUser_SetsEnabledFalse_SendsOtp() {
        SignupRequestDTO req = new SignupRequestDTO();
        req.setUsername("newcustomer");
        req.setEmail("newcustomer@gmail.com");
        req.setPassword("Password123!");
        req.setFullName("Nguyen Van New");

        when(userRepository.findByEmail("newcustomer@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("newcustomer")).thenReturn(false);
        when(pendingRegistrationRepository.findByEmail("newcustomer@gmail.com")).thenReturn(Optional.empty());
        when(pendingRegistrationRepository.findByUsername("newcustomer")).thenReturn(Optional.empty());
        when(encoder.encode("Password123!")).thenReturn("encoded_pass");

        RegisterResponseDTO res = authService.registerUser(req);

        assertNotNull(res);
        assertEquals("newcustomer@gmail.com", res.getEmail());
        verify(pendingRegistrationRepository, times(1)).save(any(PendingRegistration.class));
        verify(emailService, times(1)).sendRegistrationOtpEmail(eq("newcustomer@gmail.com"), anyString(), eq("Nguyen Van New"));
    }

    @Test
    @DisplayName("verifyRegistrationOtp: Successfully activates user with valid OTP and returns JWT token")
    void testVerifyRegistrationOtp_Success() {
        PendingRegistration pending = new PendingRegistration(
                "pendinguser",
                "pending@gmail.com",
                "encoded_pass",
                "Nguyen Pending",
                "0901234567",
                "HCMC",
                "123456",
                Instant.now().plus(5, ChronoUnit.MINUTES)
        );

        when(pendingRegistrationRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.of(pending));
        when(roleRepository.findByName(ERole.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtUtils.generateTokenFromUsername("pendinguser")).thenReturn("mock_jwt_token");

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO("pending@gmail.com", "123456");
        JwtResponseDTO res = authService.verifyRegistrationOtp(dto);

        assertNotNull(res);
        assertEquals("mock_jwt_token", res.getToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(pendingRegistrationRepository, times(1)).delete(pending);
    }

    @Test
    @DisplayName("verifyRegistrationOtp: Throws exception when OTP is incorrect")
    void testVerifyRegistrationOtp_InvalidOtp_ThrowsException() {
        PendingRegistration pending = new PendingRegistration(
                "pendinguser",
                "pending@gmail.com",
                "encoded_pass",
                "Nguyen Pending",
                "0901234567",
                "HCMC",
                "654321",
                Instant.now().plus(5, ChronoUnit.MINUTES)
        );

        when(pendingRegistrationRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.of(pending));

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO("pending@gmail.com", "000000");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyRegistrationOtp(dto);
        });

        assertEquals("Mã xác thực OTP không chính xác. Vui lòng kiểm tra lại!", ex.getMessage());
    }

    @Test
    @DisplayName("verifyRegistrationOtp: Throws exception when OTP is expired")
    void testVerifyRegistrationOtp_ExpiredOtp_ThrowsException() {
        PendingRegistration pending = new PendingRegistration(
                "pendinguser",
                "pending@gmail.com",
                "encoded_pass",
                "Nguyen Pending",
                "0901234567",
                "HCMC",
                "123456",
                Instant.now().minus(1, ChronoUnit.MINUTES) // Expired
        );

        when(pendingRegistrationRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.of(pending));

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO("pending@gmail.com", "123456");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyRegistrationOtp(dto);
        });

        assertEquals("Mã OTP đã hết hạn (quá 10 phút). Vui lòng bấm 'Gửi lại mã'!", ex.getMessage());
    }

    @Test
    @DisplayName("resendRegistrationOtp: Successfully generates new OTP and sends email")
    void testResendRegistrationOtp_Success() {
        PendingRegistration pending = new PendingRegistration(
                "pendinguser",
                "pending@gmail.com",
                "encoded_pass",
                "Nguyen Pending",
                "0901234567",
                "HCMC",
                "111111",
                Instant.now().plus(5, ChronoUnit.MINUTES)
        );

        when(userRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.empty());
        when(pendingRegistrationRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.of(pending));

        authService.resendRegistrationOtp("pending@gmail.com");

        verify(pendingRegistrationRepository, times(1)).save(argThat(p ->
                p.getOtp() != null && !p.getOtp().equals("111111")
        ));
        verify(emailService, times(1)).sendRegistrationOtpEmail(eq("pending@gmail.com"), anyString(), eq("Nguyen Pending"));
    }
}
