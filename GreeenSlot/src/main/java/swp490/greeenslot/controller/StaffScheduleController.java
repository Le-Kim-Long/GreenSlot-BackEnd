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

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/staff-schedules")
@Tag(name = "Staff Schedule Management", description = "APIs for managing staff work schedules")
public class StaffScheduleController {

    @Autowired
    private StaffScheduleService staffScheduleService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get all schedules")
    public ResponseEntity<List<StaffScheduleDTO>> getAllSchedules() {
        return ResponseEntity.ok(staffScheduleService.getAllSchedules());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<StaffScheduleDTO> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(staffScheduleService.getScheduleById(id));
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get schedules by staff")
    public ResponseEntity<List<StaffScheduleDTO>> getSchedulesByStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(staffScheduleService.getSchedulesByStaff(staffId));
    }

    @GetMapping("/location/{locationId}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get schedules by location")
    public ResponseEntity<List<StaffScheduleDTO>> getSchedulesByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(staffScheduleService.getSchedulesByLocation(locationId));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get schedules by date")
    public ResponseEntity<List<StaffScheduleDTO>> getSchedulesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(staffScheduleService.getSchedulesByDate(date));
    }

    @GetMapping("/location/{locationId}/date/{date}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get schedules by location and date")
    public ResponseEntity<List<StaffScheduleDTO>> getSchedulesByLocationAndDate(
            @PathVariable Long locationId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(staffScheduleService.getSchedulesByLocationAndDate(locationId, date));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Create a new schedule")
    public ResponseEntity<StaffScheduleDTO> createSchedule(@Valid @RequestBody StaffScheduleDTO dto) {
        return ResponseEntity.ok(staffScheduleService.createSchedule(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update a schedule")
    public ResponseEntity<StaffScheduleDTO> updateSchedule(@PathVariable Long id, @Valid @RequestBody StaffScheduleDTO dto) {
        return ResponseEntity.ok(staffScheduleService.updateSchedule(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Delete a schedule")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        staffScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
