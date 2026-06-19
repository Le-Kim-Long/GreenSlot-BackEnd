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
import swp490.greeenslot.service.BookingService;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Slot Booking", description = "Apis for Garden Slot Booking, extensions, and rental history")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/available")
    @Operation(summary = "Browse available garden slots", description = "Filters slots by Location ID if provided. Otherwise returns all available slots.")
    public ResponseEntity<List<AvailableSlotResponseDTO>> getAvailableSlots(@RequestParam(required = false) Long locationId) {
        List<GardenSlot> slots = bookingService.getAvailableSlots(locationId);
        List<AvailableSlotResponseDTO> dtoList = slots.stream().map(s -> new AvailableSlotResponseDTO(
                s.getId(),
                s.getSlotNumber(),
                s.getPrice(),
                s.getStatus().name(),
                s.getPillar().getPillarCode(),
                s.getPillar().getLocation().getName()
        )).collect(Collectors.toList());
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
}
