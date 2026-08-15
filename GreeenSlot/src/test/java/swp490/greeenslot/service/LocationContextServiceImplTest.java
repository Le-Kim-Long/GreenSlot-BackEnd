package swp490.greeenslot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import swp490.greeenslot.entity.ERole;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.Role;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.impl.LocationContextServiceImpl;
import swp490.greeenslot.service.impl.UserDetailsImpl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationContextServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LocationContextServiceImpl locationContextService;

    private User locationManagerUser;
    private User adminUser;
    private Location location1;
    private Location location2;

    @BeforeEach
    void setUp() {
        location1 = new Location();
        location1.setId(101L);
        location1.setName("GreenSlot Thu Duc Branch");

        location2 = new Location();
        location2.setId(202L);
        location2.setName("GreenSlot District 7 Branch");

        Role locMgrRole = new Role();
        locMgrRole.setName(ERole.ROLE_LOCATION_MANAGER);

        Role adminRole = new Role();
        adminRole.setName(ERole.ROLE_ADMIN);

        locationManagerUser = new User();
        locationManagerUser.setId(10L);
        locationManagerUser.setUsername("loc_manager_1");
        locationManagerUser.setRoles(Set.of(locMgrRole));
        locationManagerUser.setLocation(location1);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("super_admin");
        adminUser.setRoles(Set.of(adminRole));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(Long userId, String username, String role) {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                userId, username, username + "@greenslot.com", "pass", "Full Name", true,
                List.of(new SimpleGrantedAuthority(role))
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("ROLE_LOCATION_MANAGER: resolveTargetLocationId ignores client param and enforces assigned location")
    void testResolveTargetLocationId_LocationManager_EnforcesAssignedLocation() {
        mockSecurityContext(10L, "loc_manager_1", "ROLE_LOCATION_MANAGER");
        when(userRepository.findById(10L)).thenReturn(Optional.of(locationManagerUser));

        Long resolved = locationContextService.resolveTargetLocationId(999L);

        assertEquals(101L, resolved, "Must strictly return assigned location ID (101), not requested (999)");
    }

    @Test
    @DisplayName("ROLE_LOCATION_MANAGER: Unassigned location throws AccessDeniedException")
    void testResolveTargetLocationId_LocationManagerWithoutLocation_ThrowsAccessDenied() {
        locationManagerUser.setLocation(null);
        mockSecurityContext(10L, "loc_manager_1", "ROLE_LOCATION_MANAGER");
        when(userRepository.findById(10L)).thenReturn(Optional.of(locationManagerUser));

        assertThrows(AccessDeniedException.class, () -> {
            locationContextService.resolveTargetLocationId(101L);
        });
    }

    @Test
    @DisplayName("ROLE_LOCATION_MANAGER: validateLocationAccess succeeds for assigned location")
    void testValidateLocationAccess_LocationManager_AssignedLocation_Success() {
        mockSecurityContext(10L, "loc_manager_1", "ROLE_LOCATION_MANAGER");
        when(userRepository.findById(10L)).thenReturn(Optional.of(locationManagerUser));

        assertDoesNotThrow(() -> {
            locationContextService.validateLocationAccess(101L);
        });
    }

    @Test
    @DisplayName("ROLE_LOCATION_MANAGER: validateLocationAccess throws AccessDeniedException for foreign location")
    void testValidateLocationAccess_LocationManager_ForeignLocation_ThrowsAccessDenied() {
        mockSecurityContext(10L, "loc_manager_1", "ROLE_LOCATION_MANAGER");
        when(userRepository.findById(10L)).thenReturn(Optional.of(locationManagerUser));

        assertThrows(AccessDeniedException.class, () -> {
            locationContextService.validateLocationAccess(202L);
        });
    }

    @Test
    @DisplayName("ROLE_ADMIN: resolveTargetLocationId respects requested location ID")
    void testResolveTargetLocationId_Admin_UsesRequestedLocation() {
        mockSecurityContext(1L, "super_admin", "ROLE_ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        Long resolved = locationContextService.resolveTargetLocationId(202L);

        assertEquals(202L, resolved);
    }

    @Test
    @DisplayName("ROLE_ADMIN: validateLocationAccess permits access to any location")
    void testValidateLocationAccess_Admin_AnyLocation_Success() {
        mockSecurityContext(1L, "super_admin", "ROLE_ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertDoesNotThrow(() -> {
            locationContextService.validateLocationAccess(999L);
        });
    }
}
