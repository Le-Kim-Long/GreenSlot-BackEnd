package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLifetimeValueDTO {
    
    private Long userId;
    
    private String userName;
    
    private String userEmail;
    
    private Double totalSpent;
    
    private Long totalRentals;
    
    private Double averageRentalValue;
    
    private LocalDateTime firstRentalDate;
    
    private LocalDateTime lastRentalDate;
    
    private Long daysAsCustomer;
    
    private Double monthlyAverageSpend;
    
    private Double customerLifetimeValue;
}
