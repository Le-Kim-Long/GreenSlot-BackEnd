package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevenueByLocationDTO {
    private Long locationId;
    private String locationName;
    private BigDecimal totalRevenue;
    private Long transactionCount;
}
