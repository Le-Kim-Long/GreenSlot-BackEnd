package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import swp490.greeenslot.entity.Pillar;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PillarDetailDTO {
    private Long id;
    private String pillarCode;
    private String status;
    private String pillarType;
    private String pillarTypeName;
    private Integer capacityHoles;
    private BigDecimal price;
    private Double requiredArea;
    private Long defaultTreeId;
    private String defaultTreeName;
    private BigDecimal defaultTreePrice;
    private String defaultTreeImageUrl;
    private String cameraStreamUrl;
    private String cameraStatus;
    private Long locationId;
    private String locationName;
    private Long slotId;
    private String slotNumber;

    public static PillarDetailDTO fromEntity(Pillar p) {
        if (p == null) return null;
        return PillarDetailDTO.builder()
                .id(p.getId())
                .pillarCode(p.getPillarCode())
                .status(p.getStatus() != null ? p.getStatus().name() : "ACTIVE")
                .pillarType(p.getEffectivePillarType().name())
                .pillarTypeName(p.getEffectivePillarType().getDisplayName())
                .capacityHoles(p.getEffectiveHoles())
                .price(p.getEffectivePrice())
                .requiredArea(p.getEffectiveArea())
                .defaultTreeId(p.getDefaultTree() != null ? p.getDefaultTree().getId() : null)
                .defaultTreeName(p.getDefaultTree() != null ? p.getDefaultTree().getTreeName() : null)
                .defaultTreePrice(p.getDefaultTree() != null ? p.getDefaultTree().getPrice() : null)
                .defaultTreeImageUrl(p.getDefaultTree() != null ? p.getDefaultTree().getImageUrl() : null)
                .cameraStreamUrl(p.getCameraStreamUrl())
                .cameraStatus(p.getCameraStatus())
                .locationId(p.getLocation() != null ? p.getLocation().getId() : null)
                .locationName(p.getLocation() != null ? p.getLocation().getName() : null)
                .slotId(p.getGardenSlot() != null ? p.getGardenSlot().getId() : null)
                .slotNumber(p.getGardenSlot() != null ? p.getGardenSlot().getSlotNumber() : null)
                .build();
    }
}