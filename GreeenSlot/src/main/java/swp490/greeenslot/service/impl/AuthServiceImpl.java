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
import swp490.greeenslot.dto.ResetPasswordRequestDTO;
import swp490.greeenslot.dto.SignupRequestDTO;
import swp490.greeenslot.service.EmailService;
import swp490.greeenslot.entity.ERole;
import swp490.greeenslot.entity.Role;
import swp490.greeenslot.entity.User;
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
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponseDTO(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getFullName(),
                roles);
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

        User user = userRepository.findByEmail(email).orElseGet(() -> {
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

            return userRepository.save(newUser);
        });

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("Your account has been deactivated. Please contact support.");
        }

        String jwt = jwtUtils.generateTokenFromUsername(user.getUsername());
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        return new JwtResponseDTO(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles
        );
    }

    @Override
    @Transactional
    public void registerUser(SignupRequestDTO signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new IllegalArgumentException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getFullName(),
                signUpRequest.getPhone(),
                signUpRequest.getAddress());

        // SECURITY FIX: Always assign ROLE_CUSTOMER for self-registration.
        // Admin/Manager roles must be assigned by an existing admin via a separate admin API.
        Set<Role> roles = new HashSet<>();
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(customerRole);

        user.setRoles(roles);
        userRepository.save(user);
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
