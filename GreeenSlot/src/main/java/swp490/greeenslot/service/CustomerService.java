package swp490.greeenslot.service;

import swp490.greeenslot.dto.AvailableSlotDTO;
import swp490.greeenslot.dto.GardeningTaskResponseDTO;
import swp490.greeenslot.dto.RentalHistoryDTO;
import swp490.greeenslot.dto.SensorReadingResponseDTO;
import swp490.greeenslot.dto.ServiceCategoryDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerService {
    
    List<AvailableSlotDTO> getAvailableSlots();
    
    List<AvailableSlotDTO> getAvailableSlotsByLocation(Long locationId);
    
    List<AvailableSlotDTO> getAvailableSlotsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
    
    List<AvailableSlotDTO> getAvailableSlotsByLocationAndPrice(Long locationId, BigDecimal minPrice, BigDecimal maxPrice);
    
    AvailableSlotDTO getSlotDetails(Long slotId);
    
    List<SensorReadingResponseDTO> getSlotIoTHistory(Long slotId, String sensorType, int limit);
    
    List<ServiceCategoryDTO> getServiceCategories();
    
    List<GardeningTaskResponseDTO> getMyServiceRequests(String username);
    
    List<RentalHistoryDTO> getMyRentalHistory(String username);
    
    RentalHistoryDTO getActiveRental(String username);
}
