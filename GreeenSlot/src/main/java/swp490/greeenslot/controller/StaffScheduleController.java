package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.StaffScheduleDTO;
import swp490.greeenslot.service.StaffScheduleService;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/staff-schedules")
@Tag(name = "Staff Schedule Management", description = "APIs for managing staff work schedules")
public class StaffScheduleController {

    @Autowired
    private StaffScheduleService staffScheduleService;

    @Autowired
    private swp490.greeenslot.service.LocationContextService locationContextService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get all schedules (scoped to current staff for Garden Staff, or location for Location Managers)")
    public ResponseEntity<List<StaffScheduleDTO>> getAllSchedules() {
        if (locationContextService.isGardenStaff()) {
            swp490.greeenslot.entity.User currentUser = locationContextService.getCurrentUser();
            if (currentUser != null) {
                return ResponseEntity.ok(staffScheduleService.getSchedulesByStaff(currentUser.getId()));
            }
        }
        if (locationContextService.isLocationManager()) {
            Long managerLocationId = locationContextService.getCurrentUserLocationId();
            if (managerLocationId != null) {
                return ResponseEntity.ok(staffScheduleService.getSchedulesByLocation(managerLocationId));
            }
        }
        return ResponseEntity.ok(staffScheduleService.getAllSchedules());
    }

    @GetMapping("/my-schedules")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get personal work schedule for current staff")
    public ResponseEntity<List<StaffScheduleDTO>> getMySchedules() {
        swp490.greeenslot.entity.User currentUser = locationContextService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        return ResponseEntity.ok(staffScheduleService.getSchedulesByStaff(currentUser.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<StaffScheduleDTO> getScheduleById(@PathVariable Long id) {
        StaffScheduleDTO dto = staffScheduleService.getScheduleById(id);
        if (dto != null && dto.getLocationId() != null) {
            locationContextService.validateLocationAccess(dto.getLocationId());
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get schedules by staff")
    public ResponseEntity<List<StaffScheduleDTO>> getSchedulesByStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(staffScheduleService.getSchedulesByStaff(staffId));
    }

    @GetMapping("/location/{locationId}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get schedules by location")
    public ResponseEntity<List<StaffScheduleDTO>> getSchedulesByLocation(@PathVariable Long locationId) {
        locationContextService.validateLocationAccess(locationId);
        return ResponseEntity.ok(staffScheduleService.getSchedulesByLocation(locationId));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get schedules by date (scoped for Location Managers)")
    public ResponseEntity<List<StaffScheduleDTO>> getSchedulesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (locationContextService.isLocationManager()) {
            Long managerLocationId = locationContextService.getCurrentUserLocationId();
            if (managerLocationId != null) {
                return ResponseEntity.ok(staffScheduleService.getSchedulesByLocationAndDate(managerLocationId, date));
            }
        }
        return ResponseEntity.ok(staffScheduleService.getSchedulesByDate(date));
    }

    @GetMapping("/location/{locationId}/date/{date}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get schedules by location and date")
    public ResponseEntity<List<StaffScheduleDTO>> getSchedulesByLocationAndDate(
            @PathVariable Long locationId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        locationContextService.validateLocationAccess(locationId);
        return ResponseEntity.ok(staffScheduleService.getSchedulesByLocationAndDate(locationId, date));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create a new schedule")
    public ResponseEntity<StaffScheduleDTO> createSchedule(@Valid @RequestBody StaffScheduleDTO dto) {
        if (locationContextService.isLocationManager()) {
            Long managerLocationId = locationContextService.getCurrentUserLocationId();
            if (managerLocationId != null) {
                dto.setLocationId(managerLocationId);
            }
        }
        return ResponseEntity.ok(staffScheduleService.createSchedule(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update a schedule")
    public ResponseEntity<StaffScheduleDTO> updateSchedule(@PathVariable Long id, @Valid @RequestBody StaffScheduleDTO dto) {
        if (locationContextService.isLocationManager()) {
            Long managerLocationId = locationContextService.getCurrentUserLocationId();
            if (managerLocationId != null) {
                dto.setLocationId(managerLocationId);
            }
        }
        return ResponseEntity.ok(staffScheduleService.updateSchedule(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Delete a schedule")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        staffScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
