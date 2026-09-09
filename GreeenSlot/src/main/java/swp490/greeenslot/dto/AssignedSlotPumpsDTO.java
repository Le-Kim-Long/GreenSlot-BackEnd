package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedSlotPumpsDTO {
    private Long slotId;
    private String slotNumber;
    private Double area;
    private Long locationId;
    private String locationName;
    @Builder.Default
    private List<PillarPumpInfoDTO> pillars = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PillarPumpInfoDTO {
        private Long pillarId;
        private String pillarCode;
        private Integer capacityHoles;
        private String pillarType;
        private String pillarTypeName;
        private String treeName;
        private String pumpStatus;
        private Boolean autoMode;
        private String lastTriggerReason;
        private LocalDateTime lastTriggerTime;
    }
}
