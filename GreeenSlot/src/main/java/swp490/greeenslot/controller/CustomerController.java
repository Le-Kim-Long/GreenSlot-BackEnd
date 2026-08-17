package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.service.CustomerService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@Tag(name = "Customer APIs", description = "APIs for customers to browse and rent garden slots")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/slots/available")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get all available slots")
    public ResponseEntity<List<AvailableSlotDTO>> getAvailableSlots() {
        return ResponseEntity.ok(customerService.getAvailableSlots());
    }

    @GetMapping("/slots/available/location/{locationId}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get available slots by location")
    public ResponseEntity<List<AvailableSlotDTO>> getAvailableSlotsByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(customerService.getAvailableSlotsByLocation(locationId));
    }

    @GetMapping("/slots/available/price-range")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get available slots by price range")
    public ResponseEntity<List<AvailableSlotDTO>> getAvailableSlotsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        return ResponseEntity.ok(customerService.getAvailableSlotsByPriceRange(minPrice, maxPrice));
    }

    @GetMapping("/slots/available/location/{locationId}/price-range")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get available slots by location and price range")
    public ResponseEntity<List<AvailableSlotDTO>> getAvailableSlotsByLocationAndPrice(
            @PathVariable Long locationId,
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        return ResponseEntity.ok(customerService.getAvailableSlotsByLocationAndPrice(locationId, minPrice, maxPrice));
    }

    @GetMapping("/slots/{slotId}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get slot details")
    public ResponseEntity<AvailableSlotDTO> getSlotDetails(@PathVariable Long slotId) {
        return ResponseEntity.ok(customerService.getSlotDetails(slotId));
    }

    @GetMapping("/slots/{slotId}/iot-history")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get IoT sensor history for a slot")
    public ResponseEntity<List<SensorReadingResponseDTO>> getSlotIoTHistory(
            @PathVariable Long slotId,
            @RequestParam(required = false) String sensorType,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(customerService.getSlotIoTHistory(slotId, sensorType, limit));
    }

    @GetMapping("/service-categories")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get available service categories")
    public ResponseEntity<List<ServiceCategoryDTO>> getServiceCategories() {
        return ResponseEntity.ok(customerService.getServiceCategories());
    }

    @GetMapping("/my-service-requests")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get my service requests")
    public ResponseEntity<List<GardeningTaskResponseDTO>> getMyServiceRequests(Authentication authentication) {
        return ResponseEntity.ok(customerService.getMyServiceRequests(authentication.getName()));
    }

    @GetMapping("/rental-history")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get my rental history")
    public ResponseEntity<List<RentalHistoryDTO>> getMyRentalHistory(Authentication authentication) {
        return ResponseEntity.ok(customerService.getMyRentalHistory(authentication.getName()));
    }

    @GetMapping("/active-rental")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get my active rental")
    public ResponseEntity<RentalHistoryDTO> getActiveRental(Authentication authentication) {
        RentalHistoryDTO activeRental = customerService.getActiveRental(authentication.getName());
        if (activeRental == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(activeRental);
    }

    @PostMapping("/rentals/{rentalId}/harvest-decision")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Submit customer harvest decision (SELF or STAFF)")
    public ResponseEntity<Map<String, String>> recordHarvestDecision(
            @PathVariable Long rentalId,
            @Valid @RequestBody HarvestDecisionRequestDTO request,
            Authentication authentication) {
        customerService.recordHarvestDecision(rentalId, request, authentication.getName());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Harvest decision recorded successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/account/deactivate")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Deactivate customer account (soft delete)")
    public ResponseEntity<String> deactivateAccount(Authentication authentication) {
        customerService.deactivateAccount(authentication.getName());
        return ResponseEntity.ok("Account deactivated successfully");
    }
}
