package swp490.greeenslot.service;

import swp490.greeenslot.dto.CustomerLifetimeValueDTO;

import java.util.List;

public interface CustomerAnalyticsService {
    
    CustomerLifetimeValueDTO calculateCustomerLifetimeValue(Long userId);
    
    List<CustomerLifetimeValueDTO> getAllCustomerLifetimeValues();
}
