package swp490.greeenslot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp490.greeenslot.dto.TreePlantingRequestCreateDTO;
import swp490.greeenslot.dto.TreePlantingRequestDTO;
import swp490.greeenslot.entity.EPlantingRequestStatus;
import swp490.greeenslot.entity.ERentalStatus;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.Tree;
import swp490.greeenslot.entity.TreePlantingRequest;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.TreePlantingRequestRepository;
import swp490.greeenslot.repository.TreeRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.impl.TreePlantingServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreePlantingServiceImplTest {

    @Mock
    private TreePlantingRequestRepository treePlantingRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SlotRentalRepository slotRentalRepository;

    @Mock
    private TreeRepository treeRepository;

    @InjectMocks
    private TreePlantingServiceImpl treePlantingService;

    private User user;
    private User otherUser;
    private SlotRental activeRental;
    private Tree validTree;
    private TreePlantingRequestCreateDTO validCreateDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("customer1");
        user.setFullName("Customer One");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("customer2");

        GardenSlot slot = new GardenSlot();
        slot.setId(10L);
        slot.setSlotNumber("A-101");

        activeRental = new SlotRental();
        activeRental.setId(100L);
        activeRental.setUser(user);
        activeRental.setGardenSlot(slot);
        activeRental.setStatus(ERentalStatus.ACTIVE);
        activeRental.setStartTime(LocalDateTime.now().minusDays(10));
        activeRental.setEndTime(LocalDateTime.now().plusDays(90));

        validTree = new Tree();
        validTree.setId(200L);
        validTree.setTreeName("Cherry Tomato");
        validTree.setHarvestDays(60);
        validTree.setMinRentalDays(30);
        validTree.setIsActive(true);

        validCreateDTO = new TreePlantingRequestCreateDTO();
        validCreateDTO.setRentalId(100L);
        validCreateDTO.setNewTreeId(200L);
        validCreateDTO.setReason("Seasonal planting");
        validCreateDTO.setNotes("Please plant in center");
    }

    @Test
    @DisplayName("Should successfully create planting request when rental duration is sufficient")
    void testCreateRequest_Success_WhenRentalDurationIsSufficient() {
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(slotRentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));
        when(treeRepository.findById(200L)).thenReturn(Optional.of(validTree));

        TreePlantingRequest savedRequest = new TreePlantingRequest();
        savedRequest.setId(1L);
        savedRequest.setRental(activeRental);
        savedRequest.setNewTree(validTree);
        savedRequest.setRequestedBy(user);
        savedRequest.setStatus(EPlantingRequestStatus.PENDING);
        savedRequest.setReason("Seasonal planting");
        savedRequest.setNotes("Please plant in center");

        when(treePlantingRequestRepository.save(any(TreePlantingRequest.class))).thenReturn(savedRequest);

        TreePlantingRequestDTO result = treePlantingService.createRequest(validCreateDTO, "customer1");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(200L, result.getNewTreeId());
        assertEquals("Cherry Tomato", result.getNewTreeName());
        verify(treePlantingRequestRepository).save(any(TreePlantingRequest.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when harvest days exceeds remaining rental duration")
    void testCreateRequest_ThrowsException_WhenHarvestDaysExceedsRemainingDuration() {
        // Rental only has 20 days remaining, but tree requires 60 days to harvest
        activeRental.setEndTime(LocalDateTime.now().plusDays(20));

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(slotRentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));
        when(treeRepository.findById(200L)).thenReturn(Optional.of(validTree));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                treePlantingService.createRequest(validCreateDTO, "customer1")
        );

        assertTrue(ex.getMessage().contains("requires at least 60 days"));
        assertTrue(ex.getMessage().contains("Please extend your rental duration"));
        verify(treePlantingRequestRepository, never()).save(any(TreePlantingRequest.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when min rental days exceeds remaining rental duration")
    void testCreateRequest_ThrowsException_WhenMinRentalDaysExceedsRemainingDuration() {
        // Tree has short harvest time (20 days) but high minRentalDays (60 days), rental has 40 days
        validTree.setHarvestDays(20);
        validTree.setMinRentalDays(60);
        activeRental.setEndTime(LocalDateTime.now().plusDays(40));

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(slotRentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));
        when(treeRepository.findById(200L)).thenReturn(Optional.of(validTree));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                treePlantingService.createRequest(validCreateDTO, "customer1")
        );

        assertTrue(ex.getMessage().contains("requires at least 60 days"));
        verify(treePlantingRequestRepository, never()).save(any(TreePlantingRequest.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user does not own the rental")
    void testCreateRequest_ThrowsException_WhenUserDoesNotOwnRental() {
        // Rental belongs to otherUser (id=2), but request made by user (id=1)
        activeRental.setUser(otherUser);

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(slotRentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                treePlantingService.createRequest(validCreateDTO, "customer1")
        );

        assertTrue(ex.getMessage().contains("Unauthorized: You do not own this rental contract"));
        verify(treePlantingRequestRepository, never()).save(any(TreePlantingRequest.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rental is not in ACTIVE status")
    void testCreateRequest_ThrowsException_WhenRentalIsNotActive() {
        activeRental.setStatus(ERentalStatus.PENDING);

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(slotRentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                treePlantingService.createRequest(validCreateDTO, "customer1")
        );

        assertTrue(ex.getMessage().contains("Slot rental is not ACTIVE"));
        verify(treePlantingRequestRepository, never()).save(any(TreePlantingRequest.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rental has already expired")
    void testCreateRequest_ThrowsException_WhenRentalExpired() {
        activeRental.setEndTime(LocalDateTime.now().minusDays(1));

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(slotRentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                treePlantingService.createRequest(validCreateDTO, "customer1")
        );

        assertTrue(ex.getMessage().contains("Slot rental has already expired"));
        verify(treePlantingRequestRepository, never()).save(any(TreePlantingRequest.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when selected tree is inactive")
    void testCreateRequest_ThrowsException_WhenTreeIsInactive() {
        validTree.setIsActive(false);

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(slotRentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));
        when(treeRepository.findById(200L)).thenReturn(Optional.of(validTree));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                treePlantingService.createRequest(validCreateDTO, "customer1")
        );

        assertTrue(ex.getMessage().contains("Selected tree type is inactive"));
        verify(treePlantingRequestRepository, never()).save(any(TreePlantingRequest.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not found")
    void testCreateRequest_ThrowsException_WhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                treePlantingService.createRequest(validCreateDTO, "unknown")
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rental is not found")
    void testCreateRequest_ThrowsException_WhenRentalNotFound() {
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(slotRentalRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                treePlantingService.createRequest(validCreateDTO, "customer1")
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when tree is not found")
    void testCreateRequest_ThrowsException_WhenTreeNotFound() {
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(slotRentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));
        when(treeRepository.findById(200L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                treePlantingService.createRequest(validCreateDTO, "customer1")
        );
    }
}
