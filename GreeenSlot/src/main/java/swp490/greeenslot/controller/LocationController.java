package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.LocationDTO;
import swp490.greeenslot.dto.LocationOperatingHoursDTO;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.repository.LocationRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Location Management", description = "APIs for managing garden locations")
public class LocationController {

    private final LocationRepository locationRepository;

    @GetMapping
    @Operation(summary = "Get all locations", description = "Retrieve a list of all locations for dropdowns")
    public ResponseEntity<List<LocationDTO>> getAllLocations() {
        List<LocationDTO> locations = locationRepository.findAll().stream()
                .map(l -> new LocationDTO(
                        l.getId(),
                        l.getName(),
                        l.getAddress(),
                        l.getContactPhone(),
                        l.getStatus(),
                        l.getArea(),
                        l.getImageUrl()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get location by ID")
    public ResponseEntity<LocationDTO> getLocationById(@PathVariable Long id) {
        return locationRepository.findById(id)
                .map(l -> new LocationDTO(
                        l.getId(),
                        l.getName(),
                        l.getAddress(),
                        l.getContactPhone(),
                        l.getStatus(),
                        l.getArea(),
                        l.getImageUrl()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/operating-hours")
    @Operation(summary = "Get location operating hours")
    public ResponseEntity<LocationOperatingHoursDTO> getOperatingHours(@PathVariable Long id) {
        return locationRepository.findById(id)
                .map(l -> new LocationOperatingHoursDTO(
                        l.getId(),
                        l.getOpenTime(),
                        l.getCloseTime(),
                        l.getClosedDays()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/operating-hours")
    @PreAuthorize("hasAnyRole('ROLE_LOCATION_MANAGER', 'ROLE_ADMIN')")
    @Operation(summary = "Update location operating hours")
    public ResponseEntity<LocationOperatingHoursDTO> updateOperatingHours(
            @PathVariable Long id,
            @RequestBody LocationOperatingHoursDTO dto) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + id));
        
        location.setOpenTime(dto.getOpenTime());
        location.setCloseTime(dto.getCloseTime());
        location.setClosedDays(dto.getClosedDays());
        
        Location updated = locationRepository.save(location);
        
        return ResponseEntity.ok(new LocationOperatingHoursDTO(
                updated.getId(),
                updated.getOpenTime(),
                updated.getCloseTime(),
                updated.getClosedDays()
        ));
    }
}
