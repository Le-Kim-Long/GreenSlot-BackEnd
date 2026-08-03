package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp490.greeenslot.dto.CustomerLifetimeValueDTO;
import swp490.greeenslot.entity.PaymentTransaction;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.PaymentTransactionRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.CustomerAnalyticsService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerAnalyticsServiceImpl implements CustomerAnalyticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Override
    public CustomerLifetimeValueDTO calculateCustomerLifetimeValue(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        List<SlotRental> rentals = slotRentalRepository.findAll().stream()
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(userId))
                .toList();

        List<PaymentTransaction> payments = paymentTransactionRepository.findAll().stream()
                .filter(p -> p.getRental() != null && p.getRental().getUser() != null 
                        && p.getRental().getUser().getId().equals(userId)
                        && p.getStatus() == swp490.greeenslot.entity.EPaymentStatus.SUCCESS)
                .toList();

        return calculateCLV(user, rentals, payments);
    }

    @Override
    public List<CustomerLifetimeValueDTO> getAllCustomerLifetimeValues() {
        List<User> customers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getName() == swp490.greeenslot.entity.ERole.ROLE_CUSTOMER))
                .toList();

        return customers.stream().map(customer -> {
            List<SlotRental> rentals = slotRentalRepository.findAll().stream()
                    .filter(r -> r.getUser() != null && r.getUser().getId().equals(customer.getId()))
                    .toList();

            List<PaymentTransaction> payments = paymentTransactionRepository.findAll().stream()
                    .filter(p -> p.getRental() != null && p.getRental().getUser() != null 
                            && p.getRental().getUser().getId().equals(customer.getId())
                            && p.getStatus() == swp490.greeenslot.entity.EPaymentStatus.SUCCESS)
                    .toList();

            return calculateCLV(customer, rentals, payments);
        }).collect(Collectors.toList());
    }

    private CustomerLifetimeValueDTO calculateCLV(User user, List<SlotRental> rentals, List<PaymentTransaction> payments) {
        CustomerLifetimeValueDTO clv = new CustomerLifetimeValueDTO();
        
        clv.setUserId(user.getId());
        clv.setUserName(user.getFullName());
        clv.setUserEmail(user.getEmail());
        
        // Total spent from successful payments
        double totalSpent = payments.stream()
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount().doubleValue() : 0.0)
                .sum();
        clv.setTotalSpent(totalSpent);
        
        // Total rentals
        clv.setTotalRentals((long) rentals.size());
        
        // Average rental value
        double avgRentalValue = rentals.isEmpty() ? 0.0 : totalSpent / rentals.size();
        clv.setAverageRentalValue(avgRentalValue);
        
        // First and last rental dates
        LocalDateTime firstRental = rentals.stream()
                .map(SlotRental::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime lastRental = rentals.stream()
                .map(SlotRental::getStartTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        clv.setFirstRentalDate(firstRental);
        clv.setLastRentalDate(lastRental);
        
        // Days as customer
        long daysAsCustomer = 0;
        if (firstRental != null) {
            daysAsCustomer = ChronoUnit.DAYS.between(firstRental, LocalDateTime.now());
        }
        clv.setDaysAsCustomer(daysAsCustomer);
        
        // Monthly average spend
        double monthlyAvgSpend = 0.0;
        if (daysAsCustomer > 0) {
            monthlyAvgSpend = (totalSpent / daysAsCustomer) * 30;
        }
        clv.setMonthlyAverageSpend(monthlyAvgSpend);
        
        // Customer Lifetime Value (simplified formula)
        // CLV = Average Purchase Value × Purchase Frequency × Customer Lifespan
        // Using monthly average as a proxy
        double customerLifetimeValue = monthlyAvgSpend * 12; // Annual value
        if (daysAsCustomer > 365) {
            // If customer has been with us for more than a year, use actual data
            customerLifetimeValue = monthlyAvgSpend * 12 * (daysAsCustomer / 365.0);
        }
        clv.setCustomerLifetimeValue(customerLifetimeValue);
        
        return clv;
    }
}
