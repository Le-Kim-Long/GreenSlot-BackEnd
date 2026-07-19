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
import swp490.greeenslot.entity.Tree;
import swp490.greeenslot.entity.Equipment;
import swp490.greeenslot.entity.EEquipmentStatus;
import swp490.greeenslot.entity.Alert;
import swp490.greeenslot.entity.EAlertStatus;
import swp490.greeenslot.entity.StaffSchedule;
import swp490.greeenslot.entity.TreePlantingRequest;
import swp490.greeenslot.entity.EPlantingRequestStatus;
import swp490.greeenslot.entity.ETreeStatus;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.ERentalStatus;
import swp490.greeenslot.repository.LocationRepository;
import swp490.greeenslot.repository.PillarRepository;
import swp490.greeenslot.repository.GardenSlotRepository;
import swp490.greeenslot.repository.TreeRepository;
import swp490.greeenslot.repository.EquipmentRepository;
import swp490.greeenslot.repository.AlertRepository;
import swp490.greeenslot.repository.StaffScheduleRepository;
import swp490.greeenslot.repository.TreePlantingRequestRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

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
            GardenSlotRepository gardenSlotRepository,
            TreeRepository treeRepository,
            EquipmentRepository equipmentRepository,
            AlertRepository alertRepository,
            StaffScheduleRepository staffScheduleRepository,
            TreePlantingRequestRepository treePlantingRequestRepository,
            SlotRentalRepository slotRentalRepository) {
        return args -> {

            // 1. Tạo các Role nếu chưa tồn tại
            for (ERole eRole : ERole.values()) {
                if (roleRepository.findByName(eRole).isEmpty()) {
                    roleRepository.save(new Role(eRole));
                }
            }

            // 1.1. Tự động chuyển đổi/tích hợp dữ liệu cũ: ROLE_FARMER -> ROLE_GARDEN_STAFF
            Role farmerRole = roleRepository.findByName(ERole.ROLE_FARMER).orElse(null);
            Role staffRole = roleRepository.findByName(ERole.ROLE_GARDEN_STAFF).orElse(null);
            if (farmerRole != null && staffRole != null) {
                userRepository.findAllWithRoles().forEach(user -> {
                    if (user.getRoles().contains(farmerRole)) {
                        user.getRoles().remove(farmerRole);
                        user.getRoles().add(staffRole);
                        userRepository.save(user);
                        System.out.println("[DataInitializer] Migrated legacy ROLE_FARMER to ROLE_GARDEN_STAFF for user: " + user.getUsername());
                    }
                });
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

            // 4. Khởi tạo dữ liệu mẫu cho Tree
            if (treeRepository.count() == 0) {
                Tree tree1 = new Tree();
                tree1.setTreeName("Cây Tràm");
                tree1.setScientificName("Melaleuca cajuputi");
                tree1.setDescription("Cây tràm chịu hạn tốt, phù hợp trồng ở vùng đất khô");
                tree1.setHarvestDays(365);
                tree1.setMinRentalDays(90);
                tree1.setPrice(BigDecimal.valueOf(500000));
                tree1.setSoilMoistureMin(30.0);
                tree1.setSoilMoistureMax(70.0);
                tree1.setLightMin(6.0);
                tree1.setLightMax(10.0);
                tree1.setPhMin(5.5);
                tree1.setPhMax(7.0);
                tree1.setCompensationPercentage(50);
                tree1.setCareInstructions("Tưới nước 2 lần/tuần, bón phân mỗi tháng");
                tree1.setIsActive(true);
                treeRepository.save(tree1);

                Tree tree2 = new Tree();
                tree2.setTreeName("Cây Mai");
                tree2.setScientificName("Ochna integerrima");
                tree2.setDescription("Cây mai vàng, biểu tượng của Tết Việt Nam");
                tree2.setHarvestDays(180);
                tree2.setMinRentalDays(60);
                tree2.setPrice(BigDecimal.valueOf(800000));
                tree2.setSoilMoistureMin(40.0);
                tree2.setSoilMoistureMax(80.0);
                tree2.setLightMin(5.0);
                tree2.setLightMax(8.0);
                tree2.setPhMin(6.0);
                tree2.setPhMax(7.5);
                tree2.setCompensationPercentage(60);
                tree2.setCareInstructions("Tưới nước hàng ngày, cần ánh sáng tốt");
                tree2.setIsActive(true);
                treeRepository.save(tree2);

                System.out.println("[DataInitializer] Created sample Trees.");
            }

            // 5. Khởi tạo dữ liệu mẫu cho Equipment
            if (equipmentRepository.count() == 0) {
                Pillar pillar1 = pillarRepository.findByPillarCode("P-Q1-01").orElse(null);
                if (pillar1 != null) {
                    Equipment equipment1 = new Equipment();
                    equipment1.setEquipmentName("Máy tưới nước tự động");
                    equipment1.setSerialNumber("EQ-2024-001");
                    equipment1.setDescription("Máy tưới nước tự động cho vườn cây");
                    equipment1.setStatus(EEquipmentStatus.AVAILABLE);
                    equipment1.setPillar(pillar1);
                    equipment1.setPurchaseDate(LocalDateTime.now().minusMonths(6));
                    equipment1.setImageUrl("https://example.com/equipment1.jpg");
                    equipmentRepository.save(equipment1);

                    Equipment equipment2 = new Equipment();
                    equipment2.setEquipmentName("Máy cắt cỏ");
                    equipment2.setSerialNumber("EQ-2024-002");
                    equipment2.setDescription("Máy cắt cỏ công suất cao");
                    equipment2.setStatus(EEquipmentStatus.IN_USE);
                    equipment2.setPillar(pillar1);
                    equipment2.setPurchaseDate(LocalDateTime.now().minusMonths(3));
                    equipment2.setLastMaintenanceDate(LocalDateTime.now().minusWeeks(2));
                    equipment2.setImageUrl("https://example.com/equipment2.jpg");
                    equipmentRepository.save(equipment2);
                }

                System.out.println("[DataInitializer] Created sample Equipment.");
            }

            // 6. Khởi tạo dữ liệu mẫu cho Alert
            if (alertRepository.count() == 0) {
                Pillar pillar1 = pillarRepository.findByPillarCode("P-Q1-01").orElse(null);
                GardenSlot slot1 = gardenSlotRepository.findBySlotNumber("S-Q1-01-A").orElse(null);
                Tree tree1 = treeRepository.findByTreeName("Cây Tràm").orElse(null);

                if (pillar1 != null && slot1 != null && tree1 != null) {
                    Alert alert1 = new Alert();
                    alert1.setAlertType("LOW_MOISTURE");
                    alert1.setDescription("Độ ẩm đất thấp dưới ngưỡng cho phép");
                    alert1.setStatus(EAlertStatus.PENDING);
                    alert1.setThresholdValue(30.0);
                    alert1.setActualValue(25.5);
                    alert1.setSensorType("SOIL_MOISTURE");
                    alert1.setPillar(pillar1);
                    alert1.setGardenSlot(slot1);
                    alert1.setTree(tree1);
                    alert1.setCreatedAt(LocalDateTime.now().minusHours(2));
                    alertRepository.save(alert1);

                    Alert alert2 = new Alert();
                    alert2.setAlertType("HIGH_TEMPERATURE");
                    alert2.setDescription("Nhiệt độ môi trường cao");
                    alert2.setStatus(EAlertStatus.PENDING);
                    alert2.setThresholdValue(35.0);
                    alert2.setActualValue(38.2);
                    alert2.setSensorType("TEMPERATURE");
                    alert2.setPillar(pillar1);
                    alert2.setGardenSlot(slot1);
                    alert2.setTree(tree1);
                    alert2.setCreatedAt(LocalDateTime.now().minusHours(5));
                    alertRepository.save(alert2);
                }

                System.out.println("[DataInitializer] Created sample Alerts.");
            }

            // 7. Khởi tạo dữ liệu mẫu cho StaffSchedule
            if (staffScheduleRepository.count() == 0) {
                Location location1 = locationRepository.findByName("Cơ sở Quận 1").orElse(null);
                User staffUser = userRepository.findByUsername("garden_staff").orElse(null);

                if (location1 != null && staffUser != null) {
                    StaffSchedule schedule1 = new StaffSchedule();
                    schedule1.setStaff(staffUser);
                    schedule1.setLocation(location1);
                    schedule1.setScheduleDate(LocalDate.now().plusDays(1));
                    schedule1.setStartTime(LocalTime.of(8, 0));
                    schedule1.setEndTime(LocalTime.of(17, 0));
                    schedule1.setNotes("Chăm sóc các cây ở khu vực A");
                    schedule1.setIsActive(true);
                    staffScheduleRepository.save(schedule1);

                    StaffSchedule schedule2 = new StaffSchedule();
                    schedule2.setStaff(staffUser);
                    schedule2.setLocation(location1);
                    schedule2.setScheduleDate(LocalDate.now().plusDays(2));
                    schedule2.setStartTime(LocalTime.of(7, 30));
                    schedule2.setEndTime(LocalTime.of(16, 30));
                    schedule2.setNotes("Kiểm tra hệ thống tưới nước");
                    schedule2.setIsActive(true);
                    staffScheduleRepository.save(schedule2);
                }

                System.out.println("[DataInitializer] Created sample StaffSchedules.");
            }

            // 8. Khởi tạo dữ liệu mẫu cho TreePlantingRequest
            if (treePlantingRequestRepository.count() == 0) {
                User customerUser = userRepository.findByUsername("customer").orElse(null);
                GardenSlot slot1 = gardenSlotRepository.findBySlotNumber("S-Q1-01-A").orElse(null);
                Tree tree2 = treeRepository.findByTreeName("Cây Mai").orElse(null);
                Tree tree1 = treeRepository.findByTreeName("Cây Tràm").orElse(null);

                if (customerUser != null && slot1 != null && tree2 != null && tree1 != null) {
                    // Tạo slot rental mẫu trước
                    SlotRental rental1 = new SlotRental();
                    rental1.setUser(customerUser);
                    rental1.setGardenSlot(slot1);
                    rental1.setStartTime(LocalDateTime.now().minusDays(30));
                    rental1.setEndTime(LocalDateTime.now().plusDays(60));
                    rental1.setStatus(ERentalStatus.ACTIVE);
                    rental1.setTree(tree1);
                    rental1.setTreeStatus(ETreeStatus.SICK);
                    rental1.setTreeNotes("Cây đang có dấu hiệu bệnh");
                    slotRentalRepository.save(rental1);

                    TreePlantingRequest request1 = new TreePlantingRequest();
                    request1.setRental(rental1);
                    request1.setNewTree(tree2);
                    request1.setRequestedBy(customerUser);
                    request1.setStatus(EPlantingRequestStatus.PENDING);
                    request1.setReason("Cây hiện tại đã bệnh, muốn trồng cây mới");
                    request1.setNotes("Yêu cầu trồng cây Mai thay thế cây Tràm");
                    request1.setRequestedAt(LocalDateTime.now().minusDays(1));
                    treePlantingRequestRepository.save(request1);

                    TreePlantingRequest request2 = new TreePlantingRequest();
                    request2.setRental(rental1);
                    request2.setNewTree(tree1);
                    request2.setRequestedBy(customerUser);
                    request2.setStatus(EPlantingRequestStatus.APPROVED);
                    request2.setReason("Muốn thay đổi loại cây");
                    request2.setNotes("Yêu cầu trồng lại cây Tràm");
                    request2.setRequestedAt(LocalDateTime.now().minusDays(3));
                    request2.setProcessedAt(LocalDateTime.now().minusDays(2));
                    request2.setProcessedBy(userRepository.findByUsername("location_manager").orElse(null));
                    treePlantingRequestRepository.save(request2);
                }

                System.out.println("[DataInitializer] Created sample TreePlantingRequests.");
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
