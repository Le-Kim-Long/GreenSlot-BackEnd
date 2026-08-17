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
import swp490.greeenslot.dto.BookingRequestDTO;
import swp490.greeenslot.dto.BookingResponseDTO;
import swp490.greeenslot.entity.ESlotStatus;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.entity.PaymentTransaction;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.GardenSlotRepository;
import swp490.greeenslot.repository.PaymentTransactionRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.impl.BookingServiceImpl;
import swp490.greeenslot.config.VNPayUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingDateValidationTest {

    private static Validator validator;

    @Mock
    private GardenSlotRepository gardenSlotRepository;

    @Mock
    private SlotRentalRepository slotRentalRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VNPayUtils vnPayUtils;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private GardenSlot testSlot;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testSlot = new GardenSlot();
        testSlot.setId(10L);
        testSlot.setSlotNumber("S-01");
        testSlot.setStatus(ESlotStatus.AVAILABLE);
        testSlot.setPrice(new BigDecimal("500000"));
    }

    // ================= DTO Bean Validation Tests =================

    @Test
    @DisplayName("DTO Validation: Should validate slotId and durationInMonths constraints")
    void testDTO_Validation_Constraints() {
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setSlotId(null);
        dto.setDurationInMonths(0);

        Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        boolean hasSlotIdViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("slotId"));
        boolean hasDurationViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("durationInMonths"));
        assertTrue(hasSlotIdViolation);
        assertTrue(hasDurationViolation);
    }

    @Test
    @DisplayName("DTO Validation: Should pass when valid slotId and duration are provided")
    void testDTO_WithValidData_ShouldHaveNoViolations() {
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setSlotId(1L);
        dto.setDurationInMonths(3);
        dto.setStartTime(LocalDateTime.now());

        Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("DTO Validation: Should pass when startTime is null (defaults to now)")
    void testDTO_WithNullStartTime_ShouldHaveNoViolations() {
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setSlotId(1L);
        dto.setDurationInMonths(3);
        dto.setStartTime(null);

        Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    // ================= Service Level Validation Tests =================

    @Test
    @DisplayName("Service Validation: Should throw IllegalArgumentException when startTime is a past calendar day")
    void testService_CreateBooking_WithPastStartTime_ThrowsException() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setSlotId(10L);
        request.setDurationInMonths(3);
        request.setStartTime(LocalDateTime.now().minusDays(2));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(gardenSlotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testSlot));
        when(slotRentalRepository.findActiveRentals(eq(10L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(paymentTransactionRepository.findRecentPendingTransactions(eq(10L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                bookingService.createBooking(request, "testuser", "127.0.0.1")
        );

        assertEquals("Start time cannot be in the past", ex.getMessage());
    }

    @Test
    @DisplayName("Service Validation: Should allow booking when startTime is null (defaults to now)")
    void testService_CreateBooking_WithNullStartTime_Success() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setSlotId(10L);
        request.setDurationInMonths(3);
        request.setStartTime(null);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(gardenSlotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testSlot));
        when(slotRentalRepository.findActiveRentals(eq(10L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(paymentTransactionRepository.findRecentPendingTransactions(eq(10L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        SlotRental savedRental = new SlotRental();
        savedRental.setId(100L);
        when(slotRentalRepository.save(any(SlotRental.class))).thenReturn(savedRental);
        when(vnPayUtils.buildPaymentUrl(anyString(), any(BigDecimal.class), anyString(), anyString(), anyBoolean()))
                .thenReturn("http://vnpay.mock/url");

        BookingResponseDTO response = bookingService.createBooking(request, "testuser", "127.0.0.1");

        assertNotNull(response);
        assertEquals(100L, response.getRentalId());
        assertEquals("http://vnpay.mock/url", response.getPaymentUrl());
        verify(slotRentalRepository).save(any(SlotRental.class));
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Service Validation: Should allow booking with future startTime")
    void testService_CreateBooking_WithFutureStartTime_Success() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setSlotId(10L);
        request.setDurationInMonths(3);
        request.setStartTime(LocalDateTime.now().plusDays(5));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(gardenSlotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testSlot));
        when(slotRentalRepository.findActiveRentals(eq(10L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(paymentTransactionRepository.findRecentPendingTransactions(eq(10L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        SlotRental savedRental = new SlotRental();
        savedRental.setId(101L);
        when(slotRentalRepository.save(any(SlotRental.class))).thenReturn(savedRental);
        when(vnPayUtils.buildPaymentUrl(anyString(), any(BigDecimal.class), anyString(), anyString(), anyBoolean()))
                .thenReturn("http://vnpay.mock/url");

        BookingResponseDTO response = bookingService.createBooking(request, "testuser", "127.0.0.1");

        assertNotNull(response);
        assertEquals(101L, response.getRentalId());
        verify(slotRentalRepository).save(any(SlotRental.class));
    }
}
