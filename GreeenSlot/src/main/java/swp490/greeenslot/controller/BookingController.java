package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.service.BookingService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Slot Booking", description = "Apis for Garden Slot Booking, extensions, and rental history")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private swp490.greeenslot.repository.PillarRepository pillarRepository;

    @GetMapping("/available")
    @Operation(summary = "Browse available garden slots", description = "Filters slots by Location ID if provided. Otherwise returns all available slots.")
    public ResponseEntity<List<AvailableSlotResponseDTO>> getAvailableSlots(@RequestParam(required = false) Long locationId) {
        List<GardenSlot> slots = bookingService.getAvailableSlots(locationId);
        List<AvailableSlotResponseDTO> dtoList = slots.stream().map(s -> {
            Location loc = s.getLocation() != null ? s.getLocation() : (s.getPillar() != null ? s.getPillar().getLocation() : null);
            List<Pillar> slotPillars = (s.getPillars() != null && !s.getPillars().isEmpty())
                    ? s.getPillars()
                    : (s.getPillar() != null ? List.of(s.getPillar()) : (loc != null ? pillarRepository.findByLocationId(loc.getId()) : Collections.emptyList()));
            
            List<String> codes = slotPillars.stream().map(Pillar::getPillarCode).collect(Collectors.toList());
            String primaryCode = !codes.isEmpty() ? String.join(", ", codes) : "N/A";
            
            List<PillarDetailDTO> pillarDetails = slotPillars.stream()
                    .map(PillarDetailDTO::fromEntity)
                    .collect(Collectors.toList());

            int totalHoles = slotPillars.stream().mapToInt(Pillar::getEffectiveHoles).sum();
            BigDecimal calculatedPillarsPrice = slotPillars.stream()
                    .filter(p -> p.getStatus() == swp490.greeenslot.entity.EPillarStatus.ACTIVE)
                    .map(Pillar::getEffectivePrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal calculatedTreesPrice = slotPillars.stream()
                    .filter(p -> p.getStatus() == swp490.greeenslot.entity.EPillarStatus.ACTIVE)
                    .map(p -> (p.getDefaultTree() != null && p.getDefaultTree().getPrice() != null) 
                            ? p.getDefaultTree().getPrice().multiply(BigDecimal.valueOf(p.getEffectiveHoles() / 24.0)) 
                            : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            AvailableSlotResponseDTO dto = new AvailableSlotResponseDTO(
                    s.getId(),
                    s.getSlotNumber(),
                    calculatedPillarsPrice,
                    s.getStatus().name(),
                    primaryCode,
                    loc != null ? loc.getName() : null,
                    s.getImageUrl(),
                    loc != null ? loc.getId() : null,
                    loc != null ? loc.getAddress() : null
            );
            dto.setPillarCodes(codes);
            dto.setPillarCount(codes.size());
            dto.setArea(s.getArea());
            dto.setMaxPillars(s.getMaxPillars() != null ? s.getMaxPillars() : s.calculateMaxPillars());
            dto.setPillars(pillarDetails);
            dto.setTotalHoles(totalHoles);
            dto.setCalculatedPillarsPrice(calculatedPillarsPrice);
            dto.setCalculatedTreesPrice(calculatedTreesPrice);
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/book")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Create slot booking and generate VNPay URL", description = "Checks availability, creates PENDING rental/transaction, and generates VNPay redirection URL.")
    public ResponseEntity<BookingResponseDTO> bookSlot(
            @Valid @RequestBody BookingRequestDTO request,
            Principal principal,
            HttpServletRequest httpServletRequest) {

        String ipAddress = httpServletRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = httpServletRequest.getRemoteAddr();
        }

        BookingResponseDTO response = bookingService.createBooking(request, principal.getName(), ipAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/extend")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Extend an active rental contract", description = "Calculates extension price, creates PENDING transaction, and generates VNPay redirection URL.")
    public ResponseEntity<BookingResponseDTO> extendRental(
            @Valid @RequestBody ExtensionRequestDTO request,
            Principal principal,
            HttpServletRequest httpServletRequest) {

        String ipAddress = httpServletRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = httpServletRequest.getRemoteAddr();
        }

        BookingResponseDTO response = bookingService.extendRental(request, principal.getName(), ipAddress);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "View personal rental history", description = "Retrieves all slot rentals and payment history for the authenticated Customer.")
    public ResponseEntity<List<RentalHistoryDTO>> getRentalHistory(Principal principal) {
        List<RentalHistoryDTO> history = bookingService.getRentalHistory(principal.getName());
        return ResponseEntity.ok(history);
    }

    @PatchMapping("/{rentalId}/cancel")
    @DeleteMapping({"/{rentalId}", "/{rentalId}/cancel"})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Manually cancel or delete a pending slot rental booking",
               description = "Allows contract owner or administrative authorities (ADMIN/MANAGER) to actively cancel a pending booking with pessimistic locking, cascading task revocation, and exclusive slot release.")
    public ResponseEntity<java.util.Map<String, String>> cancelBooking(@PathVariable Long rentalId, Principal principal) {
        bookingService.cancelPendingBooking(rentalId, principal.getName());
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Booking cancelled successfully, tasks revoked, and slot released back to AVAILABLE");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rentalId}/harvest-decision")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Record the customer's harvest decision", description = "After staff notifies that the crop is ready, the customer picks SELF (they'll harvest it themselves) or STAFF (staff harvests for them).")
    public ResponseEntity<java.util.Map<String, String>> recordHarvestDecision(
            @PathVariable Long rentalId,
            @RequestBody java.util.Map<String, String> body,
            Principal principal) {
        bookingService.recordHarvestDecision(rentalId, body.get("decision"), principal.getName());
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Harvest decision recorded");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rentalId}/pay")
    @PostMapping("/{rentalId}/pay")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get or regenerate payment URL for pending booking",
               description = "Allows contract owner to retrieve or regenerate the VNPay payment URL for an existing pending slot rental.")
    public ResponseEntity<BookingResponseDTO> repayBooking(
            @PathVariable Long rentalId,
            @RequestParam(required = false, defaultValue = "false") boolean isMobile,
            @RequestParam(required = false) String customMobileRedirectUrl,
            Principal principal,
            HttpServletRequest httpServletRequest) {
        String ipAddress = httpServletRequest.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = httpServletRequest.getRemoteAddr();
        }
        BookingResponseDTO response = bookingService.getOrRegeneratePaymentUrl(rentalId, principal.getName(), ipAddress, isMobile, customMobileRedirectUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rentalId}/confirm-payment")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm payment and activate rental",
               description = "Allows contract owner to confirm payment callback success and immediately activate pending rental.")
    public ResponseEntity<java.util.Map<String, String>> confirmPayment(
            @PathVariable Long rentalId,
            Principal principal) {
        bookingService.confirmPayment(rentalId, principal.getName());
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Payment confirmed and rental activated successfully");
        return ResponseEntity.ok(response);
    }
}
