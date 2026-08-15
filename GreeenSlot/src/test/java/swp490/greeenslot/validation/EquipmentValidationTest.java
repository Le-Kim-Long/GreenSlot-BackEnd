package swp490.greeenslot.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp490.greeenslot.dto.EquipmentDTO;
import swp490.greeenslot.entity.Equipment;
import swp490.greeenslot.repository.EquipmentRepository;
import swp490.greeenslot.repository.PillarRepository;
import swp490.greeenslot.service.impl.EquipmentServiceImpl;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentValidationTest {

    private static Validator validator;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private PillarRepository pillarRepository;

    @InjectMocks
    private EquipmentServiceImpl equipmentService;

    private EquipmentDTO validDTO;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        validDTO = new EquipmentDTO();
        validDTO.setEquipmentName("Water Pump Auto");
        validDTO.setSerialNumber("WP-100234");
        validDTO.setStatus("AVAILABLE");
        validDTO.setPurchaseDate(LocalDateTime.now().minusMonths(6));
        validDTO.setLastMaintenanceDate(LocalDateTime.now().minusDays(10));
    }

    // ================= DTO Validation Tests =================

    @Test
    @DisplayName("DTO Validation: Future purchaseDate should trigger @PastOrPresent violation")
    void testDTO_FuturePurchaseDate_FailsValidation() {
        validDTO.setPurchaseDate(LocalDateTime.now().plusDays(5));

        Set<ConstraintViolation<EquipmentDTO>> violations = validator.validate(validDTO);

        assertFalse(violations.isEmpty());
        boolean hasViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("purchaseDate")
                        && v.getMessage().contains("cannot be in the future"));
        assertTrue(hasViolation);
    }

    @Test
    @DisplayName("DTO Validation: Future lastMaintenanceDate should trigger @PastOrPresent violation")
    void testDTO_FutureMaintenanceDate_FailsValidation() {
        validDTO.setLastMaintenanceDate(LocalDateTime.now().plusDays(2));

        Set<ConstraintViolation<EquipmentDTO>> violations = validator.validate(validDTO);

        assertFalse(violations.isEmpty());
        boolean hasViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("lastMaintenanceDate")
                        && v.getMessage().contains("cannot be in the future"));
        assertTrue(hasViolation);
    }

    @Test
    @DisplayName("DTO Validation: Past dates should pass validation")
    void testDTO_PastDates_PassesValidation() {
        Set<ConstraintViolation<EquipmentDTO>> violations = validator.validate(validDTO);
        assertTrue(violations.isEmpty());
    }

    // ================= Service Validation Tests =================

    @Test
    @DisplayName("Service Validation: createEquipment with future purchaseDate throws IllegalArgumentException")
    void testService_CreateEquipment_FuturePurchaseDate_ThrowsException() {
        validDTO.setPurchaseDate(LocalDateTime.now().plusDays(10));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                equipmentService.createEquipment(validDTO)
        );

        assertEquals("Purchase date cannot be in the future", ex.getMessage());
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    @Test
    @DisplayName("Service Validation: createEquipment with future lastMaintenanceDate throws IllegalArgumentException")
    void testService_CreateEquipment_FutureMaintenanceDate_ThrowsException() {
        validDTO.setLastMaintenanceDate(LocalDateTime.now().plusDays(10));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                equipmentService.createEquipment(validDTO)
        );

        assertEquals("Last maintenance date cannot be in the future", ex.getMessage());
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    @Test
    @DisplayName("Service Validation: createEquipment with valid historical dates succeeds")
    void testService_CreateEquipment_ValidDates_Success() {
        Equipment saved = new Equipment();
        saved.setId(1L);
        saved.setEquipmentName(validDTO.getEquipmentName());
        saved.setSerialNumber(validDTO.getSerialNumber());
        saved.setPurchaseDate(validDTO.getPurchaseDate());
        saved.setLastMaintenanceDate(validDTO.getLastMaintenanceDate());

        when(equipmentRepository.save(any(Equipment.class))).thenReturn(saved);

        EquipmentDTO result = equipmentService.createEquipment(validDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Water Pump Auto", result.getEquipmentName());
        verify(equipmentRepository).save(any(Equipment.class));
    }
}
