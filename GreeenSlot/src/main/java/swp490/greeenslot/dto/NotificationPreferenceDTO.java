package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDTO {
    
    private Long id;
    
    private Long userId;
    
    private Boolean emailEnabled;
    
    private Boolean pushEnabled;
    
    private Boolean smsEnabled;
    
    private Boolean iotAlertsEnabled;
    
    private Boolean taskAssignmentEnabled;
    
    private Boolean paymentAlertsEnabled;
    
    private Boolean rentalExpirationEnabled;
    
    private Boolean marketingEnabled;
}
