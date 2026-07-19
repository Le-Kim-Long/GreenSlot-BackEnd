package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.TreePlantingRequestCreateDTO;
import swp490.greeenslot.dto.TreePlantingRequestDTO;
import swp490.greeenslot.entity.EPlantingRequestStatus;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.Tree;
import swp490.greeenslot.entity.TreePlantingRequest;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.TreePlantingRequestRepository;
import swp490.greeenslot.repository.TreeRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.TreePlantingService;

import java.time.LocalDateTime;
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

    @Override
    public List<TreePlantingRequestDTO> getAllRequests() {
        return treePlantingRequestRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TreePlantingRequestDTO getRequestById(Long id) {
        return treePlantingRequestRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO createRequest(TreePlantingRequestCreateDTO dto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        SlotRental rental = slotRentalRepository.findById(dto.getRentalId())
                .orElseThrow(() -> new RuntimeException("Rental not found with id: " + dto.getRentalId()));
        
        Tree newTree = treeRepository.findById(dto.getNewTreeId())
                .orElseThrow(() -> new RuntimeException("Tree not found with id: " + dto.getNewTreeId()));
        
        TreePlantingRequest request = new TreePlantingRequest();
        request.setRental(rental);
        request.setNewTree(newTree);
        request.setRequestedBy(user);
        request.setStatus(EPlantingRequestStatus.PENDING);
        request.setReason(dto.getReason());
        request.setNotes(dto.getNotes());
        
        TreePlantingRequest savedRequest = treePlantingRequestRepository.save(request);
        return mapToDTO(savedRequest);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO approveRequest(Long id, String username) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        request.setStatus(EPlantingRequestStatus.APPROVED);
        request.setProcessedBy(user);
        request.setProcessedAt(LocalDateTime.now());
        
        TreePlantingRequest updatedRequest = treePlantingRequestRepository.save(request);
        return mapToDTO(updatedRequest);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO rejectRequest(Long id, String reason, String username) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        request.setStatus(EPlantingRequestStatus.REJECTED);
        request.setProcessedBy(user);
        request.setProcessedAt(LocalDateTime.now());
        if (reason != null) {
            request.setNotes(reason);
        }
        
        TreePlantingRequest updatedRequest = treePlantingRequestRepository.save(request);
        return mapToDTO(updatedRequest);
    }

    @Override
    @Transactional
    public TreePlantingRequestDTO completeRequest(Long id, String username) {
        TreePlantingRequest request = treePlantingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree planting request not found with id: " + id));
        
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
        return treePlantingRequestRepository.findByStatus(EPlantingRequestStatus.PENDING).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TreePlantingRequestDTO mapToDTO(TreePlantingRequest request) {
        return new TreePlantingRequestDTO(
                request.getId(),
                request.getRental() != null ? request.getRental().getId() : null,
                request.getRental() != null && request.getRental().getGardenSlot() != null ? 
                    request.getRental().getGardenSlot().getSlotNumber() : null,
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
