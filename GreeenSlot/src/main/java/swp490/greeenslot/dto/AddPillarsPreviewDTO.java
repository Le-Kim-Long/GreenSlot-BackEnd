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
public class AddPillarsPreviewDTO {
    private Long rentalId;
    private String slotNumber;
    private long daysRemaining;
    private double slotTotalArea;
    private double currentUsedArea;
    private double availableArea;
    private double requestedArea;
    private double remainingAreaAfter;
    private int smallCount;
    private int mediumCount;
    private int largeCount;
    private int totalPillars;
    private BigDecimal monthlyPillarsPrice;
    private BigDecimal totalAmount;
    private boolean canAdd;
    private String message;
}
