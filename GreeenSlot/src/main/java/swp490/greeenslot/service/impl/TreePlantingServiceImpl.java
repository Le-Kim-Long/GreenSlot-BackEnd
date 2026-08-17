package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.TreePlantingRequestCreateDTO;
import swp490.greeenslot.dto.TreePlantingRequestDTO;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.TreePlantingRequestRepository;
import swp490.greeenslot.repository.TreeRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.NotificationService;
import swp490.greeenslot.service.TreePlantingService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TreePlantingServiceImpl implements TreePlantingService {

    @Autowired
    private TreePlantingRequestRepository treePlantingRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private TreeRepository treeRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private swp490.greeenslot.service.LocationContextService locationContextService;

    private Long getRequestLocationId(TreePlantingRequest request) {
        if (request != null && request.getRental() != null && request.getRental().getGardenSlot() != null
                && request.getRental().getGardenSlot().getPillar() != null
                && request.getRental().getGardenSlot().getPillar().getLocation() != null) {
            return request.getRental().getGardenSlot().getPillar().getLocation().getId();
        }
        return null;
    }

    private String getRequestLocationName(TreePlantingRequest request) {
        if (request != null && request.getRental() != null && request.getRental().getGardenSlot() != null
                && request.getRental().getGardenSlot().getPillar() != null
                && request.getRental().getGardenSlot().getPillar().getLocation() != null) {
            return request.getRental().getGardenSlot().getPillar().getLocation().getName();
        }
        return null;
    }

    private boolean isRequestAccessible(TreePlantingRequest request, Long locationId) {
        if (locationId == null) return true;
        Long reqLocId = getRequestLocationId(request);
        return reqLocId != null && reqLocId.equals(locationId);
    }

    @Override
    public List<TreePlantingRequestDTO> getAllRequests() {
        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        return treePlantingRequestRepository.findAll().stream()
                .filter(r -> isRequestAccessible(r, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TreePlantingRequestDTO getRequestById(Long id) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        if (locationContextService != null && locationContextService.isLocationManager()) {
            Long locId = getRequestLocationId(request);
            locationContextService.validateLocationAccess(locId);
        }
        return mapToDTO(request);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO createRequest(TreePlantingRequestCreateDTO dto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        
        SlotRental rental = slotRentalRepository.findById(dto.getRentalId())
                .orElseThrow(() -> new IllegalArgumentException("Rental not found with id: " + dto.getRentalId()));
        
        // 1. Check rental ownership
        if (rental.getUser() == null || !rental.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized: You do not own this rental contract.");
        }
        
        // 2. Check rental status is ACTIVE
        if (rental.getStatus() != ERentalStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot request planting: Slot rental is not ACTIVE (current status: " + rental.getStatus() + ").");
        }
        
        // 3. Check rental has not expired
        LocalDateTime now = LocalDateTime.now();
        if (rental.getEndTime() == null || rental.getEndTime().isBefore(now)) {
            throw new IllegalArgumentException("Cannot request planting: Slot rental has already expired.");
        }

        // 3b. Chỉ cho trồng khi ô đất đang trống — không cho gửi yêu cầu thay thế cây đang trồng
        if (rental.getTree() != null) {
            throw new IllegalArgumentException("Cannot request planting: This slot already has a tree planted. " +
                    "You cannot replace it with another tree until the current one is harvested.");
        }

        // 4. Check tree exists and is active
        Tree newTree = treeRepository.findById(dto.getNewTreeId())
                .orElseThrow(() -> new IllegalArgumentException("Tree not found with id: " + dto.getNewTreeId()));
        
        if (Boolean.FALSE.equals(newTree.getIsActive())) {
            throw new IllegalArgumentException("Cannot request planting: Selected tree type is inactive.");
        }
        
        // 5. Check tree growth duration vs remaining rental duration
        long remainingDays = ChronoUnit.DAYS.between(now, rental.getEndTime());
        Integer harvestDays = newTree.getHarvestDays();
        Integer minRentalDays = newTree.getMinRentalDays();
        
        int requiredDays = 0;
        if (harvestDays != null && harvestDays > 0) {
            requiredDays = Math.max(requiredDays, harvestDays);
        }
        if (minRentalDays != null && minRentalDays > 0) {
            requiredDays = Math.max(requiredDays, minRentalDays);
        }
        
        if (requiredDays > 0 && remainingDays < requiredDays) {
            throw new IllegalArgumentException(String.format(
                "Cannot request planting: Tree '%s' requires at least %d days to grow/harvest, but your rental only has %d day(s) remaining (until %s). Please extend your rental duration.",
                newTree.getTreeName(),
                requiredDays,
                remainingDays,
                rental.getEndTime()
            ));
        }
        
        TreePlantingRequest request = new TreePlantingRequest();
        request.setRental(rental);
        request.setNewTree(newTree);
        request.setRequestedBy(user);
        request.setStatus(EPlantingRequestStatus.PENDING);
        request.setReason(dto.getReason());
        request.setNotes(dto.getNotes());
        
        TreePlantingRequest savedRequest = treePlantingRequestRepository.save(request);

        // Notify location managers about planting request
        if (notificationService != null) {
            String slotNumber = rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A";
            Long locId = getRequestLocationId(savedRequest);
            List<User> managers = locId != null
                    ? userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, locId)
                    : List.of();
            if (managers.isEmpty()) {
                managers = userRepository.findByRoleName(ERole.ROLE_MANAGER);
            }
            String title = "Yêu cầu trồng cây mới";
            String message = String.format("Khách hàng %s yêu cầu trồng cây %s tại ô đất %s.",
                    user.getFullName() != null ? user.getFullName() : username,
                    newTree.getTreeName(),
                    slotNumber);

            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        title,
                        message,
                        "PLANTING_REQUEST_CREATED",
                        savedRequest.getId(),
                        "/dashboard/manager/tree-requests"
                );
            }
        }

        return mapToDTO(savedRequest);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO approveRequest(Long id, String username) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        if (locationContextService != null && locationContextService.isLocationManager()) {
            Long locId = getRequestLocationId(request);
            locationContextService.validateLocationAccess(locId);
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        LocalDateTime now = LocalDateTime.now();
        request.setStatus(EPlantingRequestStatus.APPROVED);
        request.setProcessedBy(user);
        request.setProcessedAt(now);

        // Duyệt xong thì cây yêu cầu mới thực sự được trồng vào ô đất của rental này
        SlotRental rental = request.getRental();
        rental.setTree(request.getNewTree());
        rental.setTreeStatus(swp490.greeenslot.entity.ETreeStatus.HEALTHY);
        rental.setPlantedAt(now);
        rental.setHarvestReminderSent(false);
        slotRentalRepository.save(rental);

        TreePlantingRequest updatedRequest = treePlantingRequestRepository.save(request);

        // Notify customer about approved planting request
        if (notificationService != null && updatedRequest.getRequestedBy() != null) {
            String slotNumber = (rental.getGardenSlot() != null) ? rental.getGardenSlot().getSlotNumber() : "N/A";
            String treeName = updatedRequest.getNewTree() != null ? updatedRequest.getNewTree().getTreeName() : "cây trồng";
            String title = "Yêu cầu trồng cây đã được duyệt";
            String message = String.format("Yêu cầu trồng cây %s tại ô đất %s của bạn đã được duyệt.", treeName, slotNumber);

            notificationService.createNotification(
                    updatedRequest.getRequestedBy().getId(),
                    title,
                    message,
                    "PLANTING_REQUEST_APPROVED",
                    updatedRequest.getId(),
                    "/dashboard/customer/rentals"
            );
        }

        return mapToDTO(updatedRequest);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO rejectRequest(Long id, String reason, String username) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        if (locationContextService != null && locationContextService.isLocationManager()) {
            Long locId = getRequestLocationId(request);
            locationContextService.validateLocationAccess(locId);
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        request.setStatus(EPlantingRequestStatus.REJECTED);
        request.setProcessedBy(user);
        request.setProcessedAt(LocalDateTime.now());
        if (reason != null) {
            request.setNotes(reason);
        }
        
        TreePlantingRequest updatedRequest = treePlantingRequestRepository.save(request);

        // Notify customer about rejected planting request
        if (notificationService != null && updatedRequest.getRequestedBy() != null) {
            String slotNumber = (updatedRequest.getRental() != null && updatedRequest.getRental().getGardenSlot() != null)
                    ? updatedRequest.getRental().getGardenSlot().getSlotNumber() : "N/A";
            String treeName = updatedRequest.getNewTree() != null ? updatedRequest.getNewTree().getTreeName() : "cây trồng";
            String title = "Yêu cầu trồng cây bị từ chối";
            String message = String.format("Yêu cầu trồng cây %s tại ô đất %s bị từ chối. Lý do: %s",
                    treeName, slotNumber, reason != null ? reason : "Không có lý do cụ thể");

            notificationService.createNotification(
                    updatedRequest.getRequestedBy().getId(),
                    title,
                    message,
                    "PLANTING_REQUEST_REJECTED",
                    updatedRequest.getId(),
                    "/dashboard/customer/rentals"
            );
        }

        return mapToDTO(updatedRequest);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO completeRequest(Long id, String username) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        if (locationContextService != null && locationContextService.isLocationManager()) {
            Long locId = getRequestLocationId(request);
            locationContextService.validateLocationAccess(locId);
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        request.setStatus(EPlantingRequestStatus.COMPLETED);
        request.setProcessedBy(user);
        request.setProcessedAt(LocalDateTime.now());
        
        SlotRental rental = request.getRental();
        rental.setTree(request.getNewTree());
        rental.setTreeStatus(swp490.greeenslot.entity.ETreeStatus.HEALTHY);
        slotRentalRepository.save(rental);
        
        TreePlantingRequest updatedRequest = treePlantingRequestRepository.save(request);

        // Notify customer about completed planting request
        if (notificationService != null && updatedRequest.getRequestedBy() != null) {
            String slotNumber = (rental.getGardenSlot() != null) ? rental.getGardenSlot().getSlotNumber() : "N/A";
            String treeName = updatedRequest.getNewTree() != null ? updatedRequest.getNewTree().getTreeName() : "cây trồng";
            String title = "Đã hoàn thành trồng cây";
            String message = String.format("Cây %s đã được trồng thành công vào ô đất %s.", treeName, slotNumber);

            notificationService.createNotification(
                    updatedRequest.getRequestedBy().getId(),
                    title,
                    message,
                    "PLANTING_REQUEST_COMPLETED",
                    updatedRequest.getId(),
                    "/dashboard/customer/rentals"
            );
        }

        return mapToDTO(updatedRequest);
    }

    @Override
    public List<TreePlantingRequestDTO> getRequestsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return treePlantingRequestRepository.findByRequestedBy(user).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TreePlantingRequestDTO> getPendingRequests() {
        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        return treePlantingRequestRepository.findByStatus(EPlantingRequestStatus.PENDING).stream()
                .filter(r -> isRequestAccessible(r, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TreePlantingRequestDTO mapToDTO(TreePlantingRequest request) {
        return new TreePlantingRequestDTO(
                request.getId(),
                request.getRental() != null ? request.getRental().getId() : null,
                request.getRental() != null && request.getRental().getGardenSlot() != null ?
                    request.getRental().getGardenSlot().getSlotNumber() : null,
                getRequestLocationId(request),
                getRequestLocationName(request),
                request.getNewTree() != null ? request.getNewTree().getId() : null,
                request.getNewTree() != null ? request.getNewTree().getTreeName() : null,
                request.getRequestedBy() != null ? request.getRequestedBy().getId() : null,
                request.getRequestedBy() != null ? request.getRequestedBy().getFullName() : null,
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getReason(),
                request.getNotes(),
                request.getRequestedAt(),
                request.getProcessedAt(),
                request.getProcessedBy() != null ? request.getProcessedBy().getId() : null,
                request.getProcessedBy() != null ? request.getProcessedBy().getFullName() : null
        );
    }
}
