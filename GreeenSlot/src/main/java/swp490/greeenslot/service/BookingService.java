package swp490.greeenslot.service;

import swp490.greeenslot.dto.BookingRequestDTO;
import swp490.greeenslot.dto.BookingResponseDTO;
import swp490.greeenslot.dto.ExtensionRequestDTO;
import swp490.greeenslot.dto.RentalHistoryDTO;
import swp490.greeenslot.entity.GardenSlot;

import java.util.List;
import java.util.Map;

public interface BookingService {
    List<GardenSlot> getAvailableSlots(Long locationId);
    BookingResponseDTO createBooking(BookingRequestDTO request, String username, String ipAddress);
    BookingResponseDTO extendRental(ExtensionRequestDTO request, String username, String ipAddress);
    Map<String, String> processIpn(Map<String, String> params);
    List<RentalHistoryDTO> getRentalHistory(String username);
    void cancelPendingBooking(Long rentalId, String username);
    BookingResponseDTO getOrRegeneratePaymentUrl(Long rentalId, String username, String ipAddress);
}
