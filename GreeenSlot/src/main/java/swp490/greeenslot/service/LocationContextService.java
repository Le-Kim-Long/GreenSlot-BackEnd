package swp490.greeenslot.service;

import swp490.greeenslot.entity.User;

public interface LocationContextService {

    /**
     * Retrieves the currently authenticated User entity from the database.
     *
     * @return Current User or null if unauthenticated.
     */
    User getCurrentUser();

    /**
     * Checks if current user is a Location Manager (and not a global Admin/Manager).
     */
    boolean isLocationManager();

    /**
     * Checks if current user is a Garden Staff.
     */
    boolean isGardenStaff();

    /**
     * Checks if current user is a global Admin or Manager.
     */
    boolean isGlobalManagerOrAdmin();

    /**
     * Gets the Location ID assigned to the current user.
     *
     * @return assigned location ID or null.
     */
    Long getCurrentUserLocationId();

    /**
     * Safely resolves the target location ID based on user roles and requested parameter.
     * For ROLE_LOCATION_MANAGER, enforces the user's assigned location.
     * For ROLE_ADMIN / ROLE_MANAGER, uses requestedLocationId if provided.
     *
     * @param requestedLocationId location ID passed by client
     * @return verified location ID
     */
    Long resolveTargetLocationId(Long requestedLocationId);

    /**
     * Validates if the current user is authorized to view or modify the given locationId.
     * Throws AccessDeniedException if unauthorized.
     *
     * @param targetLocationId location ID being accessed
     */
    void validateLocationAccess(Long targetLocationId);
}
