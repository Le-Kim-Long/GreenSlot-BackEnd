package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.service.BusinessManagementService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/manager")
@Tag(name = "Business Management & Financial Analytics", description = "APIs for Location, Pillar, Slot, Service Categories/Types management, Operational dashboard, and Revenue analytics")
public class BusinessManagerController {

    @Autowired
    private BusinessManagementService businessManagementService;

    // ==========================================
    // Location Management
    // ==========================================

    @PostMapping("/locations")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Create a new location")
    public ResponseEntity<LocationDTO> createLocation(@Valid @RequestBody LocationDTO dto) {
        return ResponseEntity.ok(businessManagementService.createLocation(dto));
    }

    @PutMapping("/locations/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update an existing location")
    public ResponseEntity<LocationDTO> updateLocation(@PathVariable Long id, @Valid @RequestBody LocationDTO dto) {
        return ResponseEntity.ok(businessManagementService.updateLocation(id, dto));
    }

    @GetMapping("/locations")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get list of all locations")
    public ResponseEntity<List<LocationDTO>> getAllLocations() {
        return ResponseEntity.ok(businessManagementService.getAllLocations());
    }

    @GetMapping("/locations/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get location by ID")
    public ResponseEntity<LocationDTO> getLocationById(@PathVariable Long id) {
        return ResponseEntity.ok(businessManagementService.getLocationById(id));
    }

    // ==========================================
    // Pillar Management
    // ==========================================

    @PostMapping("/pillars")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Create a new pillar")
    public ResponseEntity<PillarDTO> createPillar(@Valid @RequestBody PillarDTO dto) {
        return ResponseEntity.ok(businessManagementService.createPillar(dto));
    }

    @PutMapping("/pillars/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update an existing pillar")
    public ResponseEntity<PillarDTO> updatePillar(@PathVariable Long id, @Valid @RequestBody PillarDTO dto) {
        return ResponseEntity.ok(businessManagementService.updatePillar(id, dto));
    }

    @GetMapping("/pillars")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get list of all pillars")
    public ResponseEntity<List<PillarDTO>> getAllPillars() {
        return ResponseEntity.ok(businessManagementService.getAllPillars());
    }

    @GetMapping("/pillars/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get pillar by ID")
    public ResponseEntity<PillarDTO> getPillarById(@PathVariable Long id) {
        return ResponseEntity.ok(businessManagementService.getPillarById(id));
    }

    // ==========================================
    // GardenSlot Management & Pricing
    // ==========================================

    @PostMapping("/slots")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Create a new garden slot")
    public ResponseEntity<GardenSlotDTO> createSlot(@Valid @RequestBody GardenSlotDTO dto) {
        return ResponseEntity.ok(businessManagementService.createSlot(dto));
    }

    @PutMapping("/slots/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update an existing garden slot (including slot price rule)")
    public ResponseEntity<GardenSlotDTO> updateSlot(@PathVariable Long id, @Valid @RequestBody GardenSlotDTO dto) {
        return ResponseEntity.ok(businessManagementService.updateSlot(id, dto));
    }

    @GetMapping("/slots")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get list of all garden slots")
    public ResponseEntity<List<GardenSlotDTO>> getAllSlots() {
        return ResponseEntity.ok(businessManagementService.getAllSlots());
    }

    @GetMapping("/slots/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get garden slot by ID")
    public ResponseEntity<GardenSlotDTO> getSlotById(@PathVariable Long id) {
        return ResponseEntity.ok(businessManagementService.getSlotById(id));
    }

    // ==========================================
    // Service Category Management
    // ==========================================

    @PostMapping("/service-categories")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Create a new service category")
    public ResponseEntity<ServiceCategoryDTO> createCategory(@Valid @RequestBody ServiceCategoryDTO dto) {
        return ResponseEntity.ok(businessManagementService.createCategory(dto));
    }

    @PutMapping("/service-categories/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update an existing service category")
    public ResponseEntity<ServiceCategoryDTO> updateCategory(@PathVariable Long id, @Valid @RequestBody ServiceCategoryDTO dto) {
        return ResponseEntity.ok(businessManagementService.updateCategory(id, dto));
    }

    @GetMapping("/service-categories")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get list of all service categories")
    public ResponseEntity<List<ServiceCategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(businessManagementService.getAllCategories());
    }

    @GetMapping("/service-categories/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get service category by ID")
    public ResponseEntity<ServiceCategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(businessManagementService.getCategoryById(id));
    }

    @DeleteMapping("/service-categories/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Delete service category by ID")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        businessManagementService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Service Type Management & Pricing
    // ==========================================

    @PostMapping("/service-types")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Create a new service type")
    public ResponseEntity<ServiceTypeDTO> createServiceType(@Valid @RequestBody ServiceTypeDTO dto) {
        return ResponseEntity.ok(businessManagementService.createServiceType(dto));
    }

    @PutMapping("/service-types/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update an existing service type (including service price rule)")
    public ResponseEntity<ServiceTypeDTO> updateServiceType(@PathVariable Long id, @Valid @RequestBody ServiceTypeDTO dto) {
        return ResponseEntity.ok(businessManagementService.updateServiceType(id, dto));
    }

    @GetMapping("/service-types")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get list of all service types")
    public ResponseEntity<List<ServiceTypeDTO>> getAllServiceTypes() {
        return ResponseEntity.ok(businessManagementService.getAllServiceTypes());
    }

    @GetMapping("/service-types/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get service type by ID")
    public ResponseEntity<ServiceTypeDTO> getServiceTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(businessManagementService.getServiceTypeById(id));
    }

    @DeleteMapping("/service-types/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Delete service type by ID")
    public ResponseEntity<Void> deleteServiceType(@PathVariable Long id) {
        businessManagementService.deleteServiceType(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Operational Dashboard
    // ==========================================

    @GetMapping("/active-rentals")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Retrieve a list of all currently ACTIVE slot rentals")
    public ResponseEntity<List<ActiveRentalDTO>> getActiveRentals() {
        return ResponseEntity.ok(businessManagementService.getActiveRentals());
    }

    // ==========================================
    // Financial Analytics Dashboard
    // ==========================================

    @GetMapping("/analytics/revenue")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get total financial revenue and transaction breakdowns within date bounds")
    public ResponseEntity<RevenueAnalyticsResponseDTO> getRevenueAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        return ResponseEntity.ok(businessManagementService.getRevenueAnalytics(startDateTime, endDateTime));
    }

    // ==========================================
    // Infrastructure Deletion
    // ==========================================

    @DeleteMapping("/locations/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Delete an existing location")
    public ResponseEntity<MessageResponseDTO> deleteLocation(@PathVariable Long id) {
        try {
            businessManagementService.deleteLocation(id);
            return ResponseEntity.ok(new MessageResponseDTO("Location deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponseDTO(e.getMessage()));
        }
    }

    @DeleteMapping("/pillars/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Delete an existing pillar")
    public ResponseEntity<MessageResponseDTO> deletePillar(@PathVariable Long id) {
        try {
            businessManagementService.deletePillar(id);
            return ResponseEntity.ok(new MessageResponseDTO("Pillar deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponseDTO(e.getMessage()));
        }
    }

    @DeleteMapping("/slots/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Delete an existing garden slot")
    public ResponseEntity<MessageResponseDTO> deleteSlot(@PathVariable Long id) {
        try {
            businessManagementService.deleteSlot(id);
            return ResponseEntity.ok(new MessageResponseDTO("Garden slot deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponseDTO(e.getMessage()));
        }
    }

    @GetMapping("/staffs")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Get list of garden staffs isolated by location")
    public ResponseEntity<List<UserAdminDTO>> getGardenStaffsByLocation(@RequestParam Long locationId) {
        return ResponseEntity.ok(businessManagementService.getGardenStaffsByLocation(locationId));
    }
}
