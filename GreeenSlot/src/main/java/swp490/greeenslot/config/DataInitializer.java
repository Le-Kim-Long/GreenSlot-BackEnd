package swp490.greeenslot.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import swp490.greeenslot.entity.ERole;
import swp490.greeenslot.entity.Role;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.RoleRepository;
import swp490.greeenslot.repository.UserRepository;

import java.util.Set;

/**
 * Khởi tạo dữ liệu mặc định khi ứng dụng chạy lần đầu:
 * - Tạo các Role (ADMIN, MANAGER, FARMER, CUSTOMER) nếu chưa có
 * - Tạo tài khoản mặc định cho từng role nếu chưa có
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Tạo các Role nếu chưa tồn tại
            for (ERole eRole : ERole.values()) {
                if (roleRepository.findByName(eRole).isEmpty()) {
                    roleRepository.save(new Role(eRole));
                }
            }

            // 2. Tạo tài khoản mặc định cho từng role nếu chưa tồn tại
            createDefaultUser(userRepository, roleRepository, passwordEncoder,
                    "admin", "admin@greenslot.vn", "Admin@123", "Quản trị viên", "0900000001", ERole.ROLE_ADMIN);
            createDefaultUser(userRepository, roleRepository, passwordEncoder,
                    "manager", "manager@greenslot.vn", "Manager@123", "Quản lý", "0900000002", ERole.ROLE_MANAGER);

            createDefaultUser(userRepository, roleRepository, passwordEncoder,
                    "farmer", "farmer@greenslot.vn", "Farmer@123", "Nông dân mẫu", "0900000003", ERole.ROLE_FARMER);

            createDefaultUser(userRepository, roleRepository, passwordEncoder,
                    "customer", "customer@greenslot.vn", "Customer@123", "Khách hàng mẫu", "0900000004",
                    ERole.ROLE_CUSTOMER);
        };
    }

    private void createDefaultUser(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            String username, String email, String rawPassword,
            String fullName, String phone, ERole eRole) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        Role role = roleRepository.findByName(eRole)
                .orElseThrow(() -> new RuntimeException("Role chưa được khởi tạo: " + eRole.name()));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress("GreenSlot HQ");
        user.setRoles(Set.of(role));

        userRepository.save(user);
        System.out.println("[DataInitializer] Tao tai khoan: " + username + " | password: " + rawPassword + " | role: "
                + eRole.name());
    }
}
