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

import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.entity.EPillarStatus;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.entity.ESlotStatus;
import swp490.greeenslot.repository.LocationRepository;
import swp490.greeenslot.repository.PillarRepository;
import swp490.greeenslot.repository.GardenSlotRepository;
import java.math.BigDecimal;

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
            PasswordEncoder passwordEncoder,
            LocationRepository locationRepository,
            PillarRepository pillarRepository,
            GardenSlotRepository gardenSlotRepository) {
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
                    "location_manager", "location@greenslot.vn", "Location@123", "Quản lý Cơ sở", "0900000003", ERole.ROLE_LOCATION_MANAGER);

            createDefaultUser(userRepository, roleRepository, passwordEncoder,
                    "garden_staff", "staff@greenslot.vn", "Staff@123", "Nhân viên Vườn", "0900000004", ERole.ROLE_GARDEN_STAFF);

            createDefaultUser(userRepository, roleRepository, passwordEncoder,
                    "customer", "customer@greenslot.vn", "Customer@123", "Khách hàng mẫu", "0900000005",
                    ERole.ROLE_CUSTOMER);

            // 3. Khởi tạo dữ liệu mẫu cho Location, Pillar, GardenSlot
            if (locationRepository.count() == 0) {
                Location location1 = new Location();
                location1.setName("Cơ sở Quận 1");
                location1.setAddress("123 Nguyễn Huệ, Quận 1, TP.HCM");
                location1.setContactPhone("0901234567");
                location1.setStatus("ACTIVE");
                location1.setArea(1000.0);
                locationRepository.save(location1);

                // Assign default location to default staff users
                userRepository.findByUsername("location_manager").ifPresent(u -> {
                    u.setLocation(location1);
                    userRepository.save(u);
                });
                userRepository.findByUsername("garden_staff").ifPresent(u -> {
                    u.setLocation(location1);
                    userRepository.save(u);
                });

                Pillar pillar1 = new Pillar();
                pillar1.setPillarCode("P-Q1-01");
                pillar1.setStatus(EPillarStatus.ACTIVE);
                pillar1.setLocation(location1);
                pillarRepository.save(pillar1);

                GardenSlot slot1 = new GardenSlot();
                slot1.setSlotNumber("S-Q1-01-A");
                slot1.setStatus(ESlotStatus.AVAILABLE);
                slot1.setPrice(BigDecimal.valueOf(500000));
                slot1.setPillar(pillar1);
                gardenSlotRepository.save(slot1);

                GardenSlot slot2 = new GardenSlot();
                slot2.setSlotNumber("S-Q1-01-B");
                slot2.setStatus(ESlotStatus.AVAILABLE);
                slot2.setPrice(BigDecimal.valueOf(500000));
                slot2.setPillar(pillar1);
                gardenSlotRepository.save(slot2);

                System.out.println("[DataInitializer] Created sample Location, Pillar, and GardenSlots.");
            }
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
        // SECURITY FIX: Never log raw passwords. Only log username and role.
        System.out.println("[DataInitializer] Created account: " + username + " | role: " + eRole.name());
    }
}
