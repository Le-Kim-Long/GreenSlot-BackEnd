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
import swp490.greeenslot.dto.StaffScheduleDTO;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.StaffSchedule;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.LocationRepository;
import swp490.greeenslot.repository.StaffScheduleRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.impl.StaffScheduleServiceImpl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
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
class StaffScheduleValidationTest {

    private static Validator validator;

    @Mock
    private StaffScheduleRepository staffScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private StaffScheduleServiceImpl staffScheduleService;

    private User testStaff;
    private Location testLocation;
    private StaffScheduleDTO validDTO;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        testStaff = new User();
        testStaff.setId(5L);
        testStaff.setFullName("Gardener Staff");

        testLocation = new Location();
        testLocation.setId(10L);
        testLocation.setName("HCM Farm");

        validDTO = new StaffScheduleDTO();
        validDTO.setStaffId(5L);
        validDTO.setLocationId(10L);
        validDTO.setScheduleDate(LocalDate.now().plusDays(2));
        validDTO.setStartTime(LocalTime.of(8, 0));
        validDTO.setEndTime(LocalTime.of(17, 0));
        validDTO.setNotes("Regular morning shift");
        validDTO.setIsActive(true);
    }

    // ================= DTO Validation Tests =================

    @Test
    @DisplayName("DTO Validation: Past scheduleDate should trigger @FutureOrPresent violation")
    void testDTO_PastScheduleDate_FailsValidation() {
        validDTO.setScheduleDate(LocalDate.now().minusDays(1));

        Set<ConstraintViolation<StaffScheduleDTO>> violations = validator.validate(validDTO);

        assertFalse(violations.isEmpty());
        boolean hasDateViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("scheduleDate")
                        && v.getMessage().contains("cannot be in the past"));
        assertTrue(hasDateViolation);
    }

    @Test
    @DisplayName("DTO Validation: Missing required fields should trigger @NotNull violations")
    void testDTO_NullFields_FailsValidation() {
        StaffScheduleDTO emptyDTO = new StaffScheduleDTO();

        Set<ConstraintViolation<StaffScheduleDTO>> violations = validator.validate(emptyDTO);

        assertEquals(5, violations.size()); // staffId, locationId, scheduleDate, startTime, endTime
    }

    @Test
    @DisplayName("DTO Validation: Future date and valid fields should pass validation")
    void testDTO_ValidSchedule_PassesValidation() {
        Set<ConstraintViolation<StaffScheduleDTO>> violations = validator.validate(validDTO);
        assertTrue(violations.isEmpty());
    }

    // ================= Service Validation Tests =================

    @Test
    @DisplayName("Service Validation: createSchedule with past date throws IllegalArgumentException")
    void testService_CreateSchedule_PastDate_ThrowsException() {
        validDTO.setScheduleDate(LocalDate.now().minusDays(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                staffScheduleService.createSchedule(validDTO)
        );

        assertEquals("Schedule date cannot be in the past", ex.getMessage());
        verify(staffScheduleRepository, never()).save(any(StaffSchedule.class));
    }

    @Test
    @DisplayName("Service Validation: createSchedule with startTime >= endTime throws IllegalArgumentException")
    void testService_CreateSchedule_InvalidTimeRange_ThrowsException() {
        validDTO.setStartTime(LocalTime.of(17, 0));
        validDTO.setEndTime(LocalTime.of(8, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                staffScheduleService.createSchedule(validDTO)
        );

        assertEquals("Start time must be before end time", ex.getMessage());
        verify(staffScheduleRepository, never()).save(any(StaffSchedule.class));
    }

    @Test
    @DisplayName("Service Validation: createSchedule with startTime equal to endTime throws IllegalArgumentException")
    void testService_CreateSchedule_EqualStartTimeEndTime_ThrowsException() {
        validDTO.setStartTime(LocalTime.of(10, 0));
        validDTO.setEndTime(LocalTime.of(10, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                staffScheduleService.createSchedule(validDTO)
        );

        assertEquals("Start time must be before end time", ex.getMessage());
        verify(staffScheduleRepository, never()).save(any(StaffSchedule.class));
    }

    @Test
    @DisplayName("Service Validation: createSchedule with valid date and time succeeds")
    void testService_CreateSchedule_Valid_Success() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(testStaff));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(testLocation));

        StaffSchedule savedSchedule = new StaffSchedule();
        savedSchedule.setId(1L);
        savedSchedule.setStaff(testStaff);
        savedSchedule.setLocation(testLocation);
        savedSchedule.setScheduleDate(validDTO.getScheduleDate());
        savedSchedule.setStartTime(validDTO.getStartTime());
        savedSchedule.setEndTime(validDTO.getEndTime());
        savedSchedule.setIsActive(true);

        when(staffScheduleRepository.save(any(StaffSchedule.class))).thenReturn(savedSchedule);

        StaffScheduleDTO result = staffScheduleService.createSchedule(validDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(5L, result.getStaffId());
        verify(staffScheduleRepository).save(any(StaffSchedule.class));
    }

    @Test
    @DisplayName("Service Validation: updateSchedule with past date throws IllegalArgumentException")
    void testService_UpdateSchedule_PastDate_ThrowsException() {
        validDTO.setScheduleDate(LocalDate.now().minusDays(2));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                staffScheduleService.updateSchedule(1L, validDTO)
        );

        assertEquals("Schedule date cannot be in the past", ex.getMessage());
        verify(staffScheduleRepository, never()).save(any(StaffSchedule.class));
    }
}
