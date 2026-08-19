package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.entity.ERole;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.LocationContextService;

@Service
public class LocationContextServiceImpl implements LocationContextService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLocationManager() {
        User user = getCurrentUser();
        if (user == null || user.getRoles() == null) {
            return false;
        }
        boolean hasLocationManager = user.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_LOCATION_MANAGER);
        boolean hasGlobalAdminOrManager = user.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_ADMIN || r.getName() == ERole.ROLE_MANAGER);

        return hasLocationManager && !hasGlobalAdminOrManager;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGlobalManagerOrAdmin() {
        User user = getCurrentUser();
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_ADMIN || r.getName() == ERole.ROLE_MANAGER);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCurrentUserLocationId() {
        User user = getCurrentUser();
        if (user != null && user.getLocation() != null) {
            return user.getLocation().getId();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Long resolveTargetLocationId(Long requestedLocationId) {
        if (isLocationManager()) {
            User user = getCurrentUser();
            if (user == null || user.getLocation() == null) {
                throw new AccessDeniedException("Location Manager is not assigned to any location. Please contact system administrator.");
            }
            return user.getLocation().getId();
        }

        if (requestedLocationId != null && requestedLocationId > 0) {
            return requestedLocationId;
        }

        return getCurrentUserLocationId();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateLocationAccess(Long targetLocationId) {
        if (isLocationManager()) {
            User user = getCurrentUser();
            if (user == null || user.getLocation() == null) {
                throw new AccessDeniedException("Location Manager is not assigned to any location. Please contact system administrator.");
            }
            if (targetLocationId == null || !user.getLocation().getId().equals(targetLocationId)) {
                throw new AccessDeniedException("Access denied: You are not authorized to view or manage data from location ID: " + targetLocationId);
            }
        }
    }
}
