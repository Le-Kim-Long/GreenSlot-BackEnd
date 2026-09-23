package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import swp490.greeenslot.config.JwtUtils;
import swp490.greeenslot.dto.ForgotPasswordRequestDTO;
import swp490.greeenslot.dto.ForgotPasswordResponseDTO;
import swp490.greeenslot.dto.GoogleLoginRequestDTO;
import swp490.greeenslot.dto.JwtResponseDTO;
import swp490.greeenslot.dto.LoginRequestDTO;
import swp490.greeenslot.dto.RegisterResponseDTO;
import swp490.greeenslot.dto.ResetPasswordRequestDTO;
import swp490.greeenslot.dto.SignupRequestDTO;
import swp490.greeenslot.dto.VerifyOtpRequestDTO;
import swp490.greeenslot.dto.ResendOtpRequestDTO;
import swp490.greeenslot.service.EmailService;
import swp490.greeenslot.entity.ERole;
import swp490.greeenslot.entity.PendingRegistration;
import swp490.greeenslot.entity.Role;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.PendingRegistrationRepository;
import swp490.greeenslot.repository.RoleRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.AuthService;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EmailService emailService;

    @Value("${greeenslot.app.resetTokenExpirationMs:3600000}")
    private long resetTokenExpirationMs;

    @Override
    public JwtResponseDTO authenticateUser(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElse(null);
        Long locationId = (user != null && user.getLocation() != null) ? user.getLocation().getId() : null;
        String locationName = (user != null && user.getLocation() != null) ? user.getLocation().getName() : null;

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponseDTO(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getFullName(),
                roles,
                locationId,
                locationName);
    }

    @Override
    @Transactional
    public JwtResponseDTO authenticateWithGoogle(GoogleLoginRequestDTO googleRequest) {
        String idToken = googleRequest.getIdToken();
        Map<String, Object> googlePayload = null;

        // 1. Try Google OAuth tokeninfo endpoint
        try {
            String googleUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            RestTemplate restTemplate = new RestTemplate();
            googlePayload = restTemplate.getForObject(googleUrl, Map.class);
        } catch (Exception e) {
            // 2. Fallback: Decode Firebase Auth / Google JWT payload
            try {
                String[] parts = idToken.split("\\.");
                if (parts.length >= 2) {
                    byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
                    String payloadJson = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    googlePayload = mapper.readValue(payloadJson, Map.class);
                }
            } catch (Exception parseEx) {
                throw new IllegalArgumentException("Google/Firebase token verification failed: " + parseEx.getMessage());
            }
        }

        if (googlePayload == null || googlePayload.get("email") == null) {
            throw new IllegalArgumentException("Invalid Google authentication response");
        }

        String email = (String) googlePayload.get("email");
        String name = (String) googlePayload.getOrDefault("name", email.split("@")[0]);
        String picture = (String) googlePayload.get("picture");
        String sub = (String) googlePayload.getOrDefault("sub", (String) googlePayload.get("user_id"));

        // Auto-register mode: Always register users if they don't exist, regardless of mode parameter
        java.util.Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            // Auto-register the user - create account automatically
            User newUser = new User();
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
            if (baseUsername.length() < 3) {
                baseUsername = "user_" + baseUsername;
            }
            String generatedUsername = baseUsername;
            if (userRepository.existsByUsername(generatedUsername)) {
                String suffix = sub != null && sub.length() >= 4 ? sub.substring(sub.length() - 4) : UUID.randomUUID().toString().substring(0, 4);
                generatedUsername = baseUsername + "_" + suffix;
            }

            newUser.setUsername(generatedUsername);
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setImageUrl(picture);
            newUser.setPassword(encoder.encode(UUID.randomUUID().toString()));
            newUser.setEnabled(true);

            Set<Role> roles = new HashSet<>();
            Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(customerRole);
            newUser.setRoles(roles);

            user = userRepository.save(newUser);
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("Tài khoản của bạn đang bị vô hiệu hóa hoặc chưa kích hoạt.");
        }

        String jwt = jwtUtils.generateTokenFromUsername(user.getUsername());
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        Long googleLocationId = user.getLocation() != null ? user.getLocation().getId() : null;
        String googleLocationName = user.getLocation() != null ? user.getLocation().getName() : null;

        return new JwtResponseDTO(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles,
                googleLocationId,
                googleLocationName
        );
    }

    @Override
    @Transactional
    public RegisterResponseDTO registerUser(SignupRequestDTO signUpRequest) {
        String username = signUpRequest.getUsername().trim();
        String email = signUpRequest.getEmail().trim();

        // 1. Kiểm tra nếu đã có tài khoản đang hoạt động (enabled = true)
        java.util.Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent() && Boolean.TRUE.equals(existingUserOpt.get().getEnabled())) {
            throw new IllegalArgumentException("Email này đã được sử dụng bởi một tài khoản khác!");
        }

        if (userRepository.existsByUsername(username)) {
            User existingByUsername = userRepository.findByUsername(username).orElse(null);
            if (existingByUsername != null && Boolean.TRUE.equals(existingByUsername.getEnabled())) {
                throw new IllegalArgumentException("Tên đăng nhập (username) này đã tồn tại!");
            }
        }

        // 2. Dọn dẹp bản ghi cũ trong pending_registrations nếu có cùng email hoặc username
        pendingRegistrationRepository.findByEmail(email).ifPresent(p -> pendingRegistrationRepository.delete(p));
        pendingRegistrationRepository.findByUsername(username).ifPresent(p -> pendingRegistrationRepository.delete(p));

        // 3. Tạo mã OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        Instant otpExpiry = Instant.now().plus(10, java.time.temporal.ChronoUnit.MINUTES);

        // 4. Lưu thông tin đăng ký vào bảng tạm PendingRegistration (KHÔNG lưu vào bảng users)
        PendingRegistration pending = new PendingRegistration(
                username,
                email,
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getFullName(),
                signUpRequest.getPhone(),
                signUpRequest.getAddress(),
                otp,
                otpExpiry
        );
        pendingRegistrationRepository.save(pending);

        // 5. Gửi email chứa mã OTP
        emailService.sendRegistrationOtpEmail(email, otp, signUpRequest.getFullName());

        return new RegisterResponseDTO(
                "Mã OTP xác thực đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư!",
                email,
                otp
        );
    }

    @Override
    @Transactional
    public JwtResponseDTO verifyRegistrationOtp(VerifyOtpRequestDTO request) {
        String email = request.getEmail().trim();
        String otp = request.getOtp().trim();

        // 1. Tìm thông tin đăng ký trong bảng tạm PendingRegistration
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đăng ký hoặc phiên đăng ký đã hết hạn. Vui lòng đăng ký lại!"));

        // 2. Kiểm tra mã OTP
        if (pending.getOtp() == null || !pending.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Mã xác thực OTP không chính xác. Vui lòng kiểm tra lại!");
        }

        // 3. Kiểm tra hạn OTP
        if (pending.getOtpExpiry() == null || Instant.now().isAfter(pending.getOtpExpiry())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn (quá 10 phút). Vui lòng bấm 'Gửi lại mã'!");
        }

        // 4. Kiểm tra an toàn xem email hoặc username đã bị tài khoản khác kích hoạt chưa
        User existingUserByEmail = userRepository.findByEmail(email).orElse(null);
        if (existingUserByEmail != null && Boolean.TRUE.equals(existingUserByEmail.getEnabled())) {
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("Email này đã được kích hoạt từ trước. Vui lòng đăng nhập!");
        } else if (existingUserByEmail != null) {
            userRepository.delete(existingUserByEmail);
        }

        User existingUserByUsername = userRepository.findByUsername(pending.getUsername()).orElse(null);
        if (existingUserByUsername != null && Boolean.TRUE.equals(existingUserByUsername.getEnabled())) {
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("Tên đăng nhập này đã được sử dụng. Vui lòng đăng ký lại với tên đăng nhập khác!");
        } else if (existingUserByUsername != null) {
            userRepository.delete(existingUserByUsername);
        }

        // 5. TẠO TÀI KHOẢN CHÍNH THỨC VÀO BẢNG USERS
        User user = new User(
                pending.getUsername(),
                pending.getEmail(),
                pending.getPassword(),
                pending.getFullName(),
                pending.getPhone(),
                pending.getAddress()
        );
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(customerRole);
        user.setRoles(roles);

        user = userRepository.save(user);

        // 6. Xoá bản ghi tạm sau khi tạo tài khoản thành công
        pendingRegistrationRepository.delete(pending);

        // 7. Tạo JWT và trả về thông tin đăng nhập ngay lập tức
        String jwt = jwtUtils.generateTokenFromUsername(user.getUsername());
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        Long otpLocationId = user.getLocation() != null ? user.getLocation().getId() : null;
        String otpLocationName = user.getLocation() != null ? user.getLocation().getName() : null;

        return new JwtResponseDTO(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roleNames,
                otpLocationId,
                otpLocationName
        );
    }

    @Override
    @Transactional
    public RegisterResponseDTO resendRegistrationOtp(String email) {
        String cleanEmail = email.trim();

        // 1. Kiểm tra nếu tài khoản đã được kích hoạt từ trước
        java.util.Optional<User> activeUser = userRepository.findByEmail(cleanEmail);
        if (activeUser.isPresent() && Boolean.TRUE.equals(activeUser.get().getEnabled())) {
            throw new IllegalArgumentException("Tài khoản này đã được kích hoạt. Vui lòng đăng nhập!");
        }

        // 2. Tìm thông tin trong PendingRegistration
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đăng ký chờ xác thực với email: " + cleanEmail + ". Vui lòng đăng ký lại!"));

        // 3. Tạo mã OTP mới
        String newOtp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        pending.setOtp(newOtp);
        pending.setOtpExpiry(Instant.now().plus(10, java.time.temporal.ChronoUnit.MINUTES));
        pendingRegistrationRepository.save(pending);

        // 4. Gửi email
        emailService.sendRegistrationOtpEmail(cleanEmail, newOtp, pending.getFullName());

        return new RegisterResponseDTO(
                "Mã OTP mới đã được gửi đến email của bạn!",
                cleanEmail,
                newOtp
        );
    }

    @Override
    @Transactional
    public ForgotPasswordResponseDTO forgotPassword(ForgotPasswordRequestDTO request) {
        return userRepository.findByEmail(request.getEmail().trim())
                .map(user -> {
                    String token = UUID.randomUUID().toString();
                    user.setResetToken(token);
                    user.setResetTokenExpiry(Instant.now().plusMillis(resetTokenExpirationMs));
                    userRepository.save(user);

                    boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), token);
                    if (!emailSent) {
                        throw new IllegalStateException("Failed to send password reset email. Please try again later.");
                    }
                    return new ForgotPasswordResponseDTO(
                            "If an account with that email exists, a password reset link has been sent.");
                })
                .orElse(new ForgotPasswordResponseDTO(
                        "If an account with that email exists, a password reset link has been sent."));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        String token = request.getToken() == null ? "" : request.getToken().trim();
        if (token.isEmpty() || "string".equalsIgnoreCase(token)) {
            throw new IllegalArgumentException(
                    "Invalid reset token. Call POST /api/auth/forgot-password first, then copy resetToken from the response (not Swagger placeholder 'string').");
        }

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reset token not found. Request a new token via forgot-password (tokens expire after 1 hour)."));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            user.clearResetToken();
            userRepository.save(user);
            throw new IllegalArgumentException(
                    "Reset token has expired. Call POST /api/auth/forgot-password again to get a new token.");
        }

        user.setPassword(encoder.encode(request.getNewPassword()));
        user.clearResetToken();
        userRepository.save(user);
    }
}
