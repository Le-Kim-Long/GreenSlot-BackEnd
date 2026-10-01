package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    default Optional<User> findByUsernameOrEmail(String identifier) {
        if (identifier == null || identifier.isBlank()) return Optional.empty();
        String trimmed = identifier.trim();
        return findByUsername(trimmed).or(() -> findByEmail(trimmed));
    }

    Optional<User> findByResetToken(String resetToken);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    java.util.List<User> findByRoleName(
        @org.springframework.data.repository.query.Param("roleName") swp490.greeenslot.entity.ERole roleName
    );

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    java.util.List<User> findByRoleNames(
        @org.springframework.data.repository.query.Param("roleNames") java.util.Collection<swp490.greeenslot.entity.ERole> roleNames
    );

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.location.id = :locationId")
    java.util.List<User> findByRoleNameAndLocation(
        @org.springframework.data.repository.query.Param("roleName") swp490.greeenslot.entity.ERole roleName,
        @org.springframework.data.repository.query.Param("locationId") Long locationId
    );

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE (r.name = :roleName AND u.location.id = :locationId) OR r.name = 'ROLE_MANAGER' OR r.name = 'ROLE_ADMIN'")
    java.util.List<User> findManagersForLocation(
        @org.springframework.data.repository.query.Param("roleName") swp490.greeenslot.entity.ERole roleName,
        @org.springframework.data.repository.query.Param("locationId") Long locationId
    );

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles")
    java.util.List<User> findAllWithRoles();
}
