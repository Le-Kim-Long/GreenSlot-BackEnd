package swp490.greeenslot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp490.greeenslot.dto.RevenueAnalyticsResponseDTO;
import swp490.greeenslot.entity.EPaymentStatus;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.PaymentTransaction;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.PaymentTransactionRepository;
import swp490.greeenslot.service.impl.BusinessManagementServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueAnalyticsServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private BusinessManagementServiceImpl businessManagementService;

    private PaymentTransaction tx1;
    private PaymentTransaction tx2;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2026, 9, 1, 0, 0, 0);
        end = LocalDateTime.of(2026, 9, 30, 23, 59, 59);

        Location location = new Location();
        location.setId(1L);
        location.setName("Cơ sở Quận 1");

        GardenSlot slot = new GardenSlot();
        slot.setId(10L);
        slot.setSlotNumber("S-01");
        slot.setLocation(location);

        User customer = new User();
        customer.setId(5L);
        customer.setUsername("customer1");

        SlotRental rental = new SlotRental();
        rental.setId(100L);
        rental.setGardenSlot(slot);
        rental.setUser(customer);

        tx1 = new PaymentTransaction();
        tx1.setId(1L);
        tx1.setRental(rental);
        tx1.setAmount(new BigDecimal("1500000"));
        tx1.setPaymentDate(LocalDateTime.of(2026, 9, 10, 10, 0, 0));
        tx1.setStatus(EPaymentStatus.SUCCESS);
        tx1.setVnpTxnRef("TXN_001");

        tx2 = new PaymentTransaction();
        tx2.setId(2L);
        tx2.setRental(rental);
        tx2.setAmount(new BigDecimal("2500000"));
        tx2.setPaymentDate(LocalDateTime.of(2026, 9, 11, 14, 0, 0));
        tx2.setStatus(EPaymentStatus.SUCCESS);
        tx2.setVnpTxnRef("TXN_002");
    }

    @Test
    @DisplayName("When locationId is null (Global Manager), should call findSuccessfulTransactionsBetween and return global breakdown")
    void testGetRevenueAnalytics_GlobalManager_NullLocationId() {
        when(paymentTransactionRepository.findSuccessfulTransactionsBetween(start, end))
                .thenReturn(List.of(tx1, tx2));

        RevenueAnalyticsResponseDTO result = businessManagementService.getRevenueAnalytics(null, start, end);

        assertNotNull(result);
        assertEquals(new BigDecimal("4000000"), result.getTotalRevenue());
        assertEquals(2, result.getDailyBreakdown().size());
        assertEquals("2026-09-10", result.getDailyBreakdown().get(0).getDate());
        assertEquals(new BigDecimal("1500000"), result.getDailyBreakdown().get(0).getRevenue());
        assertEquals("2026-09-11", result.getDailyBreakdown().get(1).getDate());
        assertEquals(new BigDecimal("2500000"), result.getDailyBreakdown().get(1).getRevenue());
        verify(paymentTransactionRepository).findSuccessfulTransactionsBetween(start, end);
    }

    @Test
    @DisplayName("When locationId is provided, should call findSuccessfulTransactionsByLocationBetween")
    void testGetRevenueAnalytics_SpecificLocationId() {
        when(paymentTransactionRepository.findSuccessfulTransactionsByLocationBetween(1L, start, end))
                .thenReturn(List.of(tx1));

        RevenueAnalyticsResponseDTO result = businessManagementService.getRevenueAnalytics(1L, start, end);

        assertNotNull(result);
        assertEquals(new BigDecimal("1500000"), result.getTotalRevenue());
        assertEquals(1, result.getDailyBreakdown().size());
        verify(paymentTransactionRepository).findSuccessfulTransactionsByLocationBetween(1L, start, end);
    }
}
