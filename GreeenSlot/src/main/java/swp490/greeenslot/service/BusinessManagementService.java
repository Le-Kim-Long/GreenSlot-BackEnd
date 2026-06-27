package swp490.greeenslot.service;

import swp490.greeenslot.dto.*;
import java.time.LocalDateTime;
import java.util.List;

public interface BusinessManagementService {
    // Location CRUD
    LocationDTO createLocation(LocationDTO dto);
    LocationDTO updateLocation(Long id, LocationDTO dto);
    List<LocationDTO> getAllLocations();
    LocationDTO getLocationById(Long id);

    // Pillar CRUD
    PillarDTO createPillar(PillarDTO dto);
    PillarDTO updatePillar(Long id, PillarDTO dto);
    List<PillarDTO> getAllPillars();
    PillarDTO getPillarById(Long id);

    // GardenSlot CRUD & Pricing
    GardenSlotDTO createSlot(GardenSlotDTO dto);
    GardenSlotDTO updateSlot(Long id, GardenSlotDTO dto);
    List<GardenSlotDTO> getAllSlots();
    GardenSlotDTO getSlotById(Long id);

    // ServiceCategory CRUD
    ServiceCategoryDTO createCategory(ServiceCategoryDTO dto);
    ServiceCategoryDTO updateCategory(Long id, ServiceCategoryDTO dto);
    List<ServiceCategoryDTO> getAllCategories();
    ServiceCategoryDTO getCategoryById(Long id);

    // ServiceType CRUD & Pricing
    ServiceTypeDTO createServiceType(ServiceTypeDTO dto);
    ServiceTypeDTO updateServiceType(Long id, ServiceTypeDTO dto);
    List<ServiceTypeDTO> getAllServiceTypes();
    ServiceTypeDTO getServiceTypeById(Long id);

    // Operational Dashboard
    List<ActiveRentalDTO> getActiveRentals();

    // Financial Analytics
    RevenueAnalyticsResponseDTO getRevenueAnalytics(LocalDateTime start, LocalDateTime end);

    // Delete Infrastructure
    void deleteLocation(Long id);
    void deletePillar(Long id);
    void deleteSlot(Long id);
}
