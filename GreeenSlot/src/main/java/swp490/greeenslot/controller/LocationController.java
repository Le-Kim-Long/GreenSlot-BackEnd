package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.LocationDTO;
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
}
