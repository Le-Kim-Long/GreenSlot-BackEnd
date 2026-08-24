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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import swp490.greeenslot.service.impl.UserDetailsImpl;


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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Autowired
    private TreeRepository treeRepository;

    @Autowired
    private swp490.greeenslot.service.LocationContextService locationContextService;

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
        location.setImageUrl(dto.getImageUrl());
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
        location.setImageUrl(dto.getImageUrl());
        Location saved = locationRepository.save(location);
        return mapToLocationDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationDTO> getAllLocations() {
        if (locationContextService.isLocationManager()) {
            User currentUser = locationContextService.getCurrentUser();
            if (currentUser != null && currentUser.getLocation() != null) {
                return List.of(mapToLocationDTO(currentUser.getLocation()));
            } else {
                return List.of();
            }
        }
        
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
        return new LocationDTO(l.getId(), l.getName(), l.getAddress(), l.getContactPhone(), l.getStatus(), l.getArea(), l.getImageUrl());
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

        EPillarType pillarType = EPillarType.MEDIUM;
        if (dto.getPillarType() != null && !dto.getPillarType().trim().isEmpty()) {
            try {
                pillarType = EPillarType.valueOf(dto.getPillarType().trim().toUpperCase());
            } catch (Exception e) {
                pillarType = EPillarType.MEDIUM;
            }
        }
        pillar.setPillarType(pillarType);
        pillar.setCapacityHoles(dto.getCapacityHoles() != null && dto.getCapacityHoles() > 0 ? dto.getCapacityHoles() : pillarType.getDefaultHoles());
        pillar.setPrice(dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) > 0 ? dto.getPrice() : pillarType.getDefaultPrice());

        if (dto.getDefaultTreeId() != null && dto.getDefaultTreeId() > 0) {
            Tree tree = treeRepository.findById(dto.getDefaultTreeId()).orElse(null);
            pillar.setDefaultTree(tree);
        } else {
            pillar.setDefaultTree(null);
        }

        if (dto.getSlotId() != null) {
            if (dto.getSlotId() > 0) {
                GardenSlot slot = gardenSlotRepository.findById(dto.getSlotId()).orElse(null);
                pillar.setGardenSlot(slot);
            } else {
                pillar.setGardenSlot(null);
            }
        }

        pillar.setLocation(location);
        pillar.setImageUrl(dto.getImageUrl());

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
        if (dto.getPillarType() != null && !dto.getPillarType().trim().isEmpty()) {
            try {
                pillar.setPillarType(EPillarType.valueOf(dto.getPillarType().trim().toUpperCase()));
            } catch (Exception e) {
                // Keep current if invalid
            }
        }
        if (dto.getCapacityHoles() != null && dto.getCapacityHoles() > 0) {
            pillar.setCapacityHoles(dto.getCapacityHoles());
        }
        if (dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            pillar.setPrice(dto.getPrice());
        }
        if (dto.getDefaultTreeId() != null) {
            if (dto.getDefaultTreeId() > 0) {
                Tree tree = treeRepository.findById(dto.getDefaultTreeId()).orElse(null);
                pillar.setDefaultTree(tree);
            } else {
                pillar.setDefaultTree(null);
            }
        }

        if (dto.getSlotId() != null) {
            if (dto.getSlotId() > 0) {
                GardenSlot slot = gardenSlotRepository.findById(dto.getSlotId()).orElse(null);
                pillar.setGardenSlot(slot);
            } else {
                pillar.setGardenSlot(null);
            }
        }

        pillar.setLocation(location);
        pillar.setImageUrl(dto.getImageUrl());

        Pillar saved = pillarRepository.save(pillar);
        return mapToPillarDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PillarDTO> getAllPillars() {
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        return pillarRepository.findAll().stream()
                .filter(p -> targetLocationId == null || (p.getLocation() != null && targetLocationId.equals(p.getLocation().getId())))
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
        PillarDTO dto = new PillarDTO(p.getId(), p.getPillarCode(), p.getStatus().name(), p.getLocation() != null ? p.getLocation().getId() : null, p.getImageUrl());
        dto.setPillarType(p.getEffectivePillarType().name());
        dto.setPillarTypeName(p.getEffectivePillarType().getDisplayName());
        dto.setCapacityHoles(p.getEffectiveHoles());
        dto.setPrice(p.getEffectivePrice());
        dto.setRequiredArea(p.getEffectiveArea());
        if (p.getDefaultTree() != null) {
            dto.setDefaultTreeId(p.getDefaultTree().getId());
            dto.setDefaultTreeName(p.getDefaultTree().getTreeName());
            dto.setDefaultTreePrice(p.getDefaultTree().getPrice());
        }
        if (p.getGardenSlot() != null) {
            dto.setSlotId(p.getGardenSlot().getId());
            dto.setSlotNumber(p.getGardenSlot().getSlotNumber());
        }
        return dto;
    }

    // ==========================================
    // GardenSlot CRUD
    // ==========================================

    @Override
    @Transactional
    public GardenSlotDTO createSlot(GardenSlotDTO dto) {
        Long targetLocId = dto.getLocationId() != null 
                ? dto.getLocationId() 
                : locationContextService.resolveTargetLocationId(null);

        Location location = null;
        if (targetLocId != null) {
            location = locationRepository.findById(targetLocId)
                    .orElseThrow(() -> new IllegalArgumentException("Location not found with ID " + targetLocId));
        }

        // Validate area & calculate capacity
        Double area = dto.getArea() != null && dto.getArea() > 0 ? dto.getArea() : 3.0;
        int maxPillars = Math.max(1, (int) Math.floor(area / 1.5));

        // Determine list of selected pillar IDs
        List<Long> selectedPillarIds = new ArrayList<>();
        if (dto.getPillarIds() != null && !dto.getPillarIds().isEmpty()) {
            selectedPillarIds.addAll(dto.getPillarIds());
        } else if (dto.getPillarId() != null && dto.getPillarId() > 0) {
            selectedPillarIds.add(dto.getPillarId());
        }

        List<Pillar> pillarsToAssign = new ArrayList<>();
        double totalRequiredArea = 0.0;
        BigDecimal calculatedPillarsPrice = BigDecimal.ZERO;
        if (!selectedPillarIds.isEmpty()) {
            pillarsToAssign = pillarRepository.findAllById(selectedPillarIds);
            for (Pillar p : pillarsToAssign) {
                if (location != null && p.getLocation() != null && !p.getLocation().getId().equals(location.getId())) {
                    throw new IllegalArgumentException("Trụ " + p.getPillarCode() + " không thuộc cùng cơ sở với ô vườn.");
                }
                totalRequiredArea += p.getEffectiveArea();
                calculatedPillarsPrice = calculatedPillarsPrice.add(p.getEffectivePrice());
            }
        }

        if (totalRequiredArea > area) {
            throw new IllegalArgumentException(String.format(
                "Ô vườn diện tích %.1f m² không đủ chỗ cho các trụ đã chọn (cần tối thiểu %.1f m² theo quy chuẩn không gian từng loại trụ: Nhỏ 1.0 m², Vừa 1.5 m², Lớn 2.0 m²).",
                area, totalRequiredArea));
        }

        GardenSlot slot = new GardenSlot();
        slot.setSlotNumber(dto.getSlotNumber().trim());
        
        ESlotStatus status;
        try {
            status = dto.getStatus() != null ? ESlotStatus.valueOf(dto.getStatus().toUpperCase()) : ESlotStatus.AVAILABLE;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid slot status. Must be AVAILABLE, RENTED, or MAINTENANCE");
        }
        slot.setStatus(status);
        
        // Auto-assign calculated price if price is not provided or zero
        BigDecimal finalPrice = (dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) > 0)
                ? dto.getPrice()
                : (calculatedPillarsPrice.compareTo(BigDecimal.ZERO) > 0 ? calculatedPillarsPrice : BigDecimal.ZERO);
        slot.setPrice(finalPrice);
        slot.setArea(area);
        slot.setMaxPillars(maxPillars);
        slot.setImageUrl(dto.getImageUrl());
        slot.setLocation(location);

        GardenSlot saved = gardenSlotRepository.save(slot);

        // Assign selected pillars to this slot
        if (!pillarsToAssign.isEmpty()) {
            for (Pillar p : pillarsToAssign) {
                p.setGardenSlot(saved);
                if (location != null && p.getLocation() == null) {
                    p.setLocation(location);
                }
                pillarRepository.save(p);
            }
            saved.setPillars(pillarsToAssign);
        }

        return mapToSlotDTO(saved);
    }

    @Override
    @Transactional
    public GardenSlotDTO updateSlot(Long id, GardenSlotDTO dto) {
        GardenSlot slot = gardenSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Garden slot not found with ID " + id));

        Long targetLocId = dto.getLocationId() != null 
                ? dto.getLocationId() 
                : (slot.getLocation() != null ? slot.getLocation().getId() : locationContextService.resolveTargetLocationId(null));

        Location location = null;
        if (targetLocId != null) {
            location = locationRepository.findById(targetLocId)
                    .orElseThrow(() -> new IllegalArgumentException("Location not found with ID " + targetLocId));
        }

        // Validate area & calculate capacity
        Double area = dto.getArea() != null && dto.getArea() > 0 ? dto.getArea() : (slot.getArea() != null ? slot.getArea() : 3.0);
        int maxPillars = Math.max(1, (int) Math.floor(area / 1.5));

        // Determine list of selected pillar IDs
        List<Long> selectedPillarIds = new ArrayList<>();
        if (dto.getPillarIds() != null) {
            selectedPillarIds.addAll(dto.getPillarIds());
        } else if (dto.getPillarId() != null && dto.getPillarId() > 0) {
            selectedPillarIds.add(dto.getPillarId());
        }

        List<Pillar> newPillars = new ArrayList<>();
        double totalRequiredArea = 0.0;
        BigDecimal calculatedPillarsPrice = BigDecimal.ZERO;
        if (!selectedPillarIds.isEmpty()) {
            newPillars = pillarRepository.findAllById(selectedPillarIds);
            for (Pillar p : newPillars) {
                totalRequiredArea += p.getEffectiveArea();
                calculatedPillarsPrice = calculatedPillarsPrice.add(p.getEffectivePrice());
            }
        }

        if (totalRequiredArea > area) {
            throw new IllegalArgumentException(String.format(
                "Ô vườn diện tích %.1f m² không đủ chỗ cho các trụ đã chọn (cần tối thiểu %.1f m² theo quy chuẩn không gian từng loại trụ: Nhỏ 1.0 m², Vừa 1.5 m², Lớn 2.0 m²).",
                area, totalRequiredArea));
        }

        slot.setSlotNumber(dto.getSlotNumber().trim());
        if (dto.getStatus() != null) {
            try {
                slot.setStatus(ESlotStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid slot status. Must be AVAILABLE, RENTED, or MAINTENANCE");
            }
        }
        
        BigDecimal finalPrice = (dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) > 0)
                ? dto.getPrice()
                : (calculatedPillarsPrice.compareTo(BigDecimal.ZERO) > 0 ? calculatedPillarsPrice : slot.getPrice());
        slot.setPrice(finalPrice);
        slot.setArea(area);
        slot.setMaxPillars(maxPillars);
        slot.setImageUrl(dto.getImageUrl());
        if (location != null) {
            slot.setLocation(location);
        }

        // Unassign old pillars not in selectedPillarIds
        List<Pillar> existingPillars = pillarRepository.findByGardenSlotId(slot.getId());
        for (Pillar p : existingPillars) {
            if (!selectedPillarIds.contains(p.getId())) {
                p.setGardenSlot(null);
                pillarRepository.save(p);
            }
        }

        // Assign newly selected pillars
        if (!newPillars.isEmpty()) {
            for (Pillar p : newPillars) {
                p.setGardenSlot(slot);
                if (slot.getLocation() != null) {
                    p.setLocation(slot.getLocation());
                }
                pillarRepository.save(p);
            }
            slot.setPillars(newPillars);
        } else {
            slot.setPillars(new ArrayList<>());
        }

        GardenSlot saved = gardenSlotRepository.save(slot);
        return mapToSlotDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GardenSlotDTO> getAllSlots() {
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        return gardenSlotRepository.findAll().stream()
                .filter(s -> targetLocationId == null 
                        || (s.getLocation() != null && targetLocationId.equals(s.getLocation().getId()))
                        || (s.getPillars() != null && s.getPillars().stream().anyMatch(p -> p.getLocation() != null && targetLocationId.equals(p.getLocation().getId()))))
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
        List<Pillar> slotPillars = (s.getPillars() != null && !s.getPillars().isEmpty()) 
                ? s.getPillars() 
                : pillarRepository.findByGardenSlotId(s.getId());
        List<Long> pIds = slotPillars.stream().map(Pillar::getId).collect(Collectors.toList());
        List<String> pCodes = slotPillars.stream().map(Pillar::getPillarCode).collect(Collectors.toList());
        Long singlePillarId = pIds.isEmpty() ? null : pIds.get(0);
        
        Long locId = s.getLocation() != null ? s.getLocation().getId() : (slotPillars.stream().filter(p -> p.getLocation() != null).map(p -> p.getLocation().getId()).findFirst().orElse(null));
        String locName = s.getLocation() != null ? s.getLocation().getName() : (slotPillars.stream().filter(p -> p.getLocation() != null).map(p -> p.getLocation().getName()).findFirst().orElse(null));

        Double area = s.getArea() != null ? s.getArea() : 3.0;
        Integer maxPillars = s.getMaxPillars() != null ? s.getMaxPillars() : Math.max(1, (int) Math.floor(area / 1.5));

        int totalHoles = slotPillars.stream().mapToInt(Pillar::getEffectiveHoles).sum();
        BigDecimal calculatedPillarsPrice = slotPillars.stream().map(Pillar::getEffectivePrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        double requiredArea = slotPillars.stream().mapToDouble(Pillar::getEffectiveArea).sum();

        GardenSlotDTO dto = new GardenSlotDTO();
        dto.setId(s.getId());
        dto.setSlotNumber(s.getSlotNumber());
        dto.setStatus(s.getStatus() != null ? s.getStatus().name() : "AVAILABLE");
        dto.setPrice(s.getPrice());
        dto.setArea(area);
        dto.setMaxPillars(maxPillars);
        dto.setLocationId(locId);
        dto.setLocationName(locName);
        dto.setPillarId(singlePillarId);
        dto.setPillarIds(pIds);
        dto.setPillarCodes(pCodes);
        dto.setTotalHoles(totalHoles);
        dto.setCalculatedPillarsPrice(calculatedPillarsPrice);
        dto.setRequiredArea(requiredArea);
        dto.setImageUrl(s.getImageUrl());
        return dto;
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
        serviceType.setDescription(dto.getDescription());
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
        serviceType.setDescription(dto.getDescription());
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
        return new ServiceTypeDTO(s.getId(), s.getServiceName(), s.getDescription(), s.getPrice(), s.getCategory().getId());
    }

    // ==========================================
    // Operational Dashboard
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<ActiveRentalDTO> getActiveRentals() {
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        List<SlotRental> rentals = slotRentalRepository.findAllActiveRentals();
        return rentals.stream()
                .filter(r -> {
                    if (targetLocationId == null) return true;
                    if (r.getGardenSlot() == null) return false;
                    GardenSlot slot = r.getGardenSlot();
                    Location loc = slot.getLocation() != null ? slot.getLocation() : (slot.getPillar() != null ? slot.getPillar().getLocation() : null);
                    if (loc == null && slot.getPillars() != null && !slot.getPillars().isEmpty()) {
                        Pillar firstP = slot.getPillars().iterator().next();
                        if (firstP != null) loc = firstP.getLocation();
                    }
                    return loc != null && targetLocationId.equals(loc.getId());
                })
                .map(r -> {
                    String username = (r.getUser() != null) ? r.getUser().getUsername() : "N/A";
                    String fullName = (r.getUser() != null) ? r.getUser().getFullName() : "N/A";
                    String slotNumber = (r.getGardenSlot() != null && r.getGardenSlot().getSlotNumber() != null) ? String.valueOf(r.getGardenSlot().getSlotNumber()) : "N/A";
                    
                    String pillarCode = "N/A";
                    String locationName = "N/A";
                    if (r.getGardenSlot() != null) {
                        GardenSlot slot = r.getGardenSlot();
                        if (slot.getPillars() != null && !slot.getPillars().isEmpty()) {
                            pillarCode = slot.getPillars().stream()
                                    .filter(java.util.Objects::nonNull)
                                    .map(Pillar::getPillarCode)
                                    .filter(java.util.Objects::nonNull)
                                    .collect(Collectors.joining(", "));
                        } else if (slot.getPillar() != null && slot.getPillar().getPillarCode() != null) {
                            pillarCode = slot.getPillar().getPillarCode();
                        }
                        
                        Location loc = slot.getLocation() != null ? slot.getLocation() : (slot.getPillar() != null ? slot.getPillar().getLocation() : null);
                        if (loc == null && slot.getPillars() != null && !slot.getPillars().isEmpty()) {
                            Pillar firstP = slot.getPillars().iterator().next();
                            if (firstP != null) loc = firstP.getLocation();
                        }
                        if (loc != null && loc.getName() != null) {
                            locationName = loc.getName();
                        }
                    }
                    
                    return new ActiveRentalDTO(
                            r.getId(),
                            username,
                            fullName,
                            slotNumber,
                            pillarCode,
                            locationName,
                            r.getStartTime(),
                            r.getEndTime(),
                            r.getStatus() != null ? r.getStatus().name() : "N/A"
                    );
                }).collect(Collectors.toList());
    }

    // ==========================================
    // Financial Analytics
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public RevenueAnalyticsResponseDTO getRevenueAnalytics(Long locationId, LocalDateTime start, LocalDateTime end) {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findSuccessfulTransactionsByLocationBetween(locationId, start, end);

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
                t.getRental() != null ? t.getRental().getId() : null,
                (t.getRental() != null && t.getRental().getGardenSlot() != null && t.getRental().getGardenSlot().getSlotNumber() != null) ? String.valueOf(t.getRental().getGardenSlot().getSlotNumber()) : "N/A",
                (t.getRental() != null && t.getRental().getUser() != null) ? t.getRental().getUser().getUsername() : "N/A",
                t.getAmount(),
                t.getVnpTxnRef(),
                t.getPaymentDate(),
                t.getStatus() != null ? t.getStatus().name() : "N/A"
        )).collect(Collectors.toList());

        return new RevenueAnalyticsResponseDTO(totalRevenue, dailyBreakdown, txDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueByLocationDTO> getRevenueByLocation(LocalDateTime start, LocalDateTime end) {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findSuccessfulTransactionsBetween(start, end);
        
        return transactions.stream()
                .filter(t -> t.getRental() != null 
                        && t.getRental().getGardenSlot() != null 
                        && (t.getRental().getGardenSlot().getLocation() != null || (t.getRental().getGardenSlot().getPillar() != null && t.getRental().getGardenSlot().getPillar().getLocation() != null)))
                .collect(Collectors.groupingBy(
                        t -> t.getRental().getGardenSlot().getLocation() != null ? t.getRental().getGardenSlot().getLocation() : t.getRental().getGardenSlot().getPillar().getLocation(),
                        Collectors.reducing(BigDecimal.ZERO, PaymentTransaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(entry -> new RevenueByLocationDTO(
                        entry.getKey().getId(),
                        entry.getKey().getName(),
                        entry.getValue(),
                        transactions.stream()
                                .filter(t -> t.getRental() != null 
                                        && t.getRental().getGardenSlot() != null 
                                        && (entry.getKey().equals(t.getRental().getGardenSlot().getLocation()) || 
                                            (t.getRental().getGardenSlot().getPillar() != null && entry.getKey().equals(t.getRental().getGardenSlot().getPillar().getLocation()))))
                                .count()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDeclarationDTO> getTransactionDeclarations(LocalDateTime start, LocalDateTime end) {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findSuccessfulTransactionsBetween(start, end);
        
        return transactions.stream()
                .filter(t -> t.getRental() != null 
                        && t.getRental().getGardenSlot() != null 
                        && (t.getRental().getGardenSlot().getLocation() != null || (t.getRental().getGardenSlot().getPillar() != null && t.getRental().getGardenSlot().getPillar().getLocation() != null))
                        && t.getRental().getUser() != null)
                .map(t -> {
                    GardenSlot slot = t.getRental().getGardenSlot();
                    Location loc = slot.getLocation() != null ? slot.getLocation() : (slot.getPillar() != null ? slot.getPillar().getLocation() : null);
                    String locName = loc != null ? loc.getName() : "N/A";
                    String pillarCode = (slot.getPillars() != null && !slot.getPillars().isEmpty())
                            ? slot.getPillars().stream().map(Pillar::getPillarCode).collect(Collectors.joining(", "))
                            : (slot.getPillar() != null ? slot.getPillar().getPillarCode() : "N/A");

                    return new TransactionDeclarationDTO(
                            t.getId(),
                            t.getRental().getId(),
                            slot.getSlotNumber(),
                            t.getRental().getUser().getUsername(),
                            t.getRental().getUser().getFullName(),
                            t.getAmount(),
                            t.getTransactionCode(),
                            t.getPaymentMethod() != null ? t.getPaymentMethod().name() : null,
                            t.getPaymentDate(),
                            t.getStatus() != null ? t.getStatus().name() : null,
                            locName,
                            pillarCode,
                            "Khach hang thue slot " + slot.getSlotNumber() + " tai " + locName
                    );
                })
                .collect(Collectors.toList());
    }

    // ==========================================
    // Delete Infrastructure
    // ==========================================

    @Override
    @Transactional
    public void deleteLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID " + id));

        boolean hasPillars = pillarRepository.existsByLocationId(id);
        if (hasPillars) {
            throw new IllegalArgumentException("Cannot delete Location with ID " + id + " because it contains associated Pillar records.");
        }

        locationRepository.delete(location);
    }

    @Override
    @Transactional
    public void deletePillar(Long id) {
        Pillar pillar = pillarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pillar not found with ID " + id));

        if (pillar.getGardenSlot() != null) {
            boolean hasActiveRental = slotRentalRepository.existsByGardenSlotIdAndStatus(pillar.getGardenSlot().getId(), ERentalStatus.ACTIVE);
            if (hasActiveRental) {
                throw new IllegalArgumentException("Cannot delete Pillar with ID " + id + " because its associated Garden Slot is actively rented.");
            }
        }

        pillarRepository.delete(pillar);
    }

    @Override
    @Transactional
    public void deleteSlot(Long id) {
        GardenSlot slot = gardenSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ô vườn với ID: " + id));

        boolean hasActiveRental = slotRentalRepository.existsByGardenSlotIdAndStatus(id, ERentalStatus.ACTIVE);
        if (hasActiveRental) {
            throw new IllegalArgumentException("Không thể xóa ô vườn " + slot.getSlotNumber() + " vì đang có hợp đồng thuê hoạt động.");
        }

        // Unlink any pillars attached to this slot to prevent Foreign Key constraint errors
        List<Pillar> assignedPillars = pillarRepository.findByGardenSlotId(id);
        for (Pillar p : assignedPillars) {
            p.setGardenSlot(null);
            pillarRepository.save(p);
        }

        if (slot.getPillars() != null) {
            slot.getPillars().clear();
        }

        gardenSlotRepository.delete(slot);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        ServiceCategory category = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service category not found with ID: " + id));

        boolean hasServiceTypes = serviceTypeRepository.existsByCategoryId(id);
        if (hasServiceTypes) {
            throw new IllegalArgumentException("Cannot delete Service Category with ID " + id + " because it contains associated Service Type records.");
        }

        serviceCategoryRepository.delete(category);
    }

    @Override
    @Transactional
    public void deleteServiceType(Long id) {
        ServiceType type = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service type not found with ID: " + id));

        boolean hasTasks = gardeningTaskRepository.existsByTaskName(type.getServiceName());
        if (hasTasks) {
            throw new IllegalArgumentException("Cannot delete Service Type with ID " + id + " because it contains associated Gardening Task records.");
        }

        serviceTypeRepository.delete(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAdminDTO> getGardenStaffsByLocation(Long locationId) {
        return userRepository.findByRoleNameAndLocation(ERole.ROLE_GARDEN_STAFF, locationId).stream()
                .map(user -> new UserAdminDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getPhone(),
                        user.getAddress(),
                        user.getEnabled() != null ? user.getEnabled() : true,
                        user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()),
                        user.getLocation() != null ? user.getLocation().getId() : null,
                        user.getLocation() != null ? user.getLocation().getName() : null
                ))
                .collect(Collectors.toList());
    }
}
