package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.BusinessManagementService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BusinessManagementServiceImpl implements BusinessManagementService {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    // ==========================================
    // Location CRUD
    // ==========================================

    @Override
    @Transactional
    public LocationDTO createLocation(LocationDTO dto) {
        Location location = new Location();
        location.setName(dto.getName());
        location.setAddress(dto.getAddress());
        location.setContactPhone(dto.getContactPhone());
        location.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        location.setArea(dto.getArea());
        Location saved = locationRepository.save(location);
        return mapToLocationDTO(saved);
    }

    @Override
    @Transactional
    public LocationDTO updateLocation(Long id, LocationDTO dto) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID " + id));
        location.setName(dto.getName());
        location.setAddress(dto.getAddress());
        location.setContactPhone(dto.getContactPhone());
        if (dto.getStatus() != null) {
            location.setStatus(dto.getStatus());
        }
        location.setArea(dto.getArea());
        Location saved = locationRepository.save(location);
        return mapToLocationDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationDTO> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(this::mapToLocationDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LocationDTO getLocationById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID " + id));
        return mapToLocationDTO(location);
    }

    private LocationDTO mapToLocationDTO(Location l) {
        return new LocationDTO(l.getId(), l.getName(), l.getAddress(), l.getContactPhone(), l.getStatus(), l.getArea());
    }

    // ==========================================
    // Pillar CRUD
    // ==========================================

    @Override
    @Transactional
    public PillarDTO createPillar(PillarDTO dto) {
        Location location = locationRepository.findById(dto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID " + dto.getLocationId()));

        Pillar pillar = new Pillar();
        pillar.setPillarCode(dto.getPillarCode());
        
        EPillarStatus status;
        try {
            status = dto.getStatus() != null ? EPillarStatus.valueOf(dto.getStatus().toUpperCase()) : EPillarStatus.ACTIVE;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid pillar status. Must be ACTIVE or MAINTENANCE");
        }
        pillar.setStatus(status);
        pillar.setLocation(location);

        Pillar saved = pillarRepository.save(pillar);
        return mapToPillarDTO(saved);
    }

    @Override
    @Transactional
    public PillarDTO updatePillar(Long id, PillarDTO dto) {
        Pillar pillar = pillarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pillar not found with ID " + id));
        
        Location location = locationRepository.findById(dto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID " + dto.getLocationId()));

        pillar.setPillarCode(dto.getPillarCode());
        if (dto.getStatus() != null) {
            try {
                pillar.setStatus(EPillarStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid pillar status. Must be ACTIVE or MAINTENANCE");
            }
        }
        pillar.setLocation(location);

        Pillar saved = pillarRepository.save(pillar);
        return mapToPillarDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PillarDTO> getAllPillars() {
        return pillarRepository.findAll().stream()
                .map(this::mapToPillarDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PillarDTO getPillarById(Long id) {
        Pillar pillar = pillarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pillar not found with ID " + id));
        return mapToPillarDTO(pillar);
    }

    private PillarDTO mapToPillarDTO(Pillar p) {
        return new PillarDTO(p.getId(), p.getPillarCode(), p.getStatus().name(), p.getLocation().getId());
    }

    // ==========================================
    // GardenSlot CRUD
    // ==========================================

    @Override
    @Transactional
    public GardenSlotDTO createSlot(GardenSlotDTO dto) {
        Pillar pillar = pillarRepository.findById(dto.getPillarId())
                .orElseThrow(() -> new IllegalArgumentException("Pillar not found with ID " + dto.getPillarId()));

        GardenSlot slot = new GardenSlot();
        slot.setSlotNumber(dto.getSlotNumber());
        
        ESlotStatus status;
        try {
            status = dto.getStatus() != null ? ESlotStatus.valueOf(dto.getStatus().toUpperCase()) : ESlotStatus.AVAILABLE;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid slot status. Must be AVAILABLE, RENTED, or MAINTENANCE");
        }
        slot.setStatus(status);
        slot.setPrice(dto.getPrice());
        slot.setPillar(pillar);

        GardenSlot saved = gardenSlotRepository.save(slot);
        return mapToSlotDTO(saved);
    }

    @Override
    @Transactional
    public GardenSlotDTO updateSlot(Long id, GardenSlotDTO dto) {
        GardenSlot slot = gardenSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID " + id));

        Pillar pillar = pillarRepository.findById(dto.getPillarId())
                .orElseThrow(() -> new IllegalArgumentException("Pillar not found with ID " + dto.getPillarId()));

        slot.setSlotNumber(dto.getSlotNumber());
        if (dto.getStatus() != null) {
            try {
                slot.setStatus(ESlotStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid slot status. Must be AVAILABLE, RENTED, or MAINTENANCE");
            }
        }
        slot.setPrice(dto.getPrice());
        slot.setPillar(pillar);

        GardenSlot saved = gardenSlotRepository.save(slot);
        return mapToSlotDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GardenSlotDTO> getAllSlots() {
        return gardenSlotRepository.findAll().stream()
                .map(this::mapToSlotDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GardenSlotDTO getSlotById(Long id) {
        GardenSlot slot = gardenSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID " + id));
        return mapToSlotDTO(slot);
    }

    private GardenSlotDTO mapToSlotDTO(GardenSlot s) {
        return new GardenSlotDTO(s.getId(), s.getSlotNumber(), s.getStatus().name(), s.getPrice(), s.getPillar().getId());
    }

    // ==========================================
    // ServiceCategory CRUD
    // ==========================================

    @Override
    @Transactional
    public ServiceCategoryDTO createCategory(ServiceCategoryDTO dto) {
        ServiceCategory category = new ServiceCategory();
        category.setCategoryName(dto.getCategoryName());
        category.setDescription(dto.getDescription());
        ServiceCategory saved = serviceCategoryRepository.save(category);
        return mapToCategoryDTO(saved);
    }

    @Override
    @Transactional
    public ServiceCategoryDTO updateCategory(Long id, ServiceCategoryDTO dto) {
        ServiceCategory category = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service category not found with ID " + id));
        category.setCategoryName(dto.getCategoryName());
        category.setDescription(dto.getDescription());
        ServiceCategory saved = serviceCategoryRepository.save(category);
        return mapToCategoryDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCategoryDTO> getAllCategories() {
        return serviceCategoryRepository.findAll().stream()
                .map(this::mapToCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceCategoryDTO getCategoryById(Long id) {
        ServiceCategory category = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service category not found with ID " + id));
        return mapToCategoryDTO(category);
    }

    private ServiceCategoryDTO mapToCategoryDTO(ServiceCategory c) {
        return new ServiceCategoryDTO(c.getId(), c.getCategoryName(), c.getDescription());
    }

    // ==========================================
    // ServiceType CRUD
    // ==========================================

    @Override
    @Transactional
    public ServiceTypeDTO createServiceType(ServiceTypeDTO dto) {
        ServiceCategory category = serviceCategoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Service category not found with ID " + dto.getCategoryId()));

        ServiceType serviceType = new ServiceType();
        serviceType.setServiceName(dto.getServiceName());
        serviceType.setPrice(dto.getPrice());
        serviceType.setCategory(category);

        ServiceType saved = serviceTypeRepository.save(serviceType);
        return mapToServiceTypeDTO(saved);
    }

    @Override
    @Transactional
    public ServiceTypeDTO updateServiceType(Long id, ServiceTypeDTO dto) {
        ServiceType serviceType = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service type not found with ID " + id));

        ServiceCategory category = serviceCategoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Service category not found with ID " + dto.getCategoryId()));

        serviceType.setServiceName(dto.getServiceName());
        serviceType.setPrice(dto.getPrice());
        serviceType.setCategory(category);

        ServiceType saved = serviceTypeRepository.save(serviceType);
        return mapToServiceTypeDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceTypeDTO> getAllServiceTypes() {
        return serviceTypeRepository.findAll().stream()
                .map(this::mapToServiceTypeDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceTypeDTO getServiceTypeById(Long id) {
        ServiceType serviceType = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service type not found with ID " + id));
        return mapToServiceTypeDTO(serviceType);
    }

    private ServiceTypeDTO mapToServiceTypeDTO(ServiceType s) {
        return new ServiceTypeDTO(s.getId(), s.getServiceName(), s.getPrice(), s.getCategory().getId());
    }

    // ==========================================
    // Operational Dashboard
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<ActiveRentalDTO> getActiveRentals() {
        List<SlotRental> rentals = slotRentalRepository.findAllActiveRentals();
        return rentals.stream().map(r -> new ActiveRentalDTO(
                r.getId(),
                r.getUser().getUsername(),
                r.getUser().getFullName(),
                r.getGardenSlot().getSlotNumber(),
                r.getGardenSlot().getPillar().getPillarCode(),
                r.getGardenSlot().getPillar().getLocation().getName(),
                r.getStartTime(),
                r.getEndTime(),
                r.getStatus().name()
        )).collect(Collectors.toList());
    }

    // ==========================================
    // Financial Analytics
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public RevenueAnalyticsResponseDTO getRevenueAnalytics(LocalDateTime start, LocalDateTime end) {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findSuccessfulTransactionsBetween(start, end);

        // Compute total revenue
        BigDecimal totalRevenue = transactions.stream()
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group daily breakdowns
        Map<String, BigDecimal> dailyMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPaymentDate().toLocalDate().toString(),
                        Collectors.reducing(BigDecimal.ZERO, PaymentTransaction::getAmount, BigDecimal::add)
                ));

        List<DailyRevenueDTO> dailyBreakdown = dailyMap.entrySet().stream()
                .map(entry -> new DailyRevenueDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DailyRevenueDTO::getDate))
                .collect(Collectors.toList());

        // Map detailed transactions log list
        List<PaymentTransactionDTO> txDtos = transactions.stream().map(t -> new PaymentTransactionDTO(
                t.getId(),
                t.getRental().getId(),
                t.getRental().getGardenSlot().getSlotNumber(),
                t.getRental().getUser().getUsername(),
                t.getAmount(),
                t.getVnpTxnRef(),
                t.getPaymentDate(),
                t.getStatus().name()
        )).collect(Collectors.toList());

        return new RevenueAnalyticsResponseDTO(totalRevenue, dailyBreakdown, txDtos);
    }
}
