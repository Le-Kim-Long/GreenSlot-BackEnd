package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PillarEquipmentBindingDTO {
    private String pillarCode;
    private Long equipmentId;
    private String newEquipmentName;
    private String newSerialNumber;
}
