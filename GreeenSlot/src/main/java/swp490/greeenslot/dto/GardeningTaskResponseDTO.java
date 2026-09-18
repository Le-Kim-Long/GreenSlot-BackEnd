package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GardeningTaskResponseDTO {
    private Long id;
    private String taskName;
    private String description;
    private String status;
    private String evidenceImageUrl;
    private String taskType;
    private Long assignedStaffId;
    private String assignedStaffName;
    private Long targetSlotId;
    private String targetSlotNumber;
    private LocalDateTime createdAt;
    private String rejectionReason;
    private String locationName;
    private String pillarCodes;
    private String treeName;
    private Boolean isEarlyHarvest;
    private String customerName;

    // IoT & Hardware Details for Pillar/Slot
    private List<EquipmentDTO> equipments;
    private String cameraStatus;
    private String cameraStreamUrl;
    private String deviceStatus;
    private String iotStatus;
    private String iotRecommendation;
    private String staffNotes;
    private List<PillarEquipmentBindingDTO> equipmentBindings;

    public GardeningTaskResponseDTO(Long id, String taskName, String description, String status,
                                  String evidenceImageUrl, String taskType, Long assignedStaffId,
                                  String assignedStaffName, Long targetSlotId, String targetSlotNumber,
                                  LocalDateTime createdAt, String rejectionReason, String locationName,
                                  String pillarCodes, String treeName, Boolean isEarlyHarvest,
                                  String customerName) {
        this.id = id;
        this.taskName = taskName;
        this.description = description;
        this.status = status;
        this.evidenceImageUrl = evidenceImageUrl;
        this.taskType = taskType;
        this.assignedStaffId = assignedStaffId;
        this.assignedStaffName = assignedStaffName;
        this.targetSlotId = targetSlotId;
        this.targetSlotNumber = targetSlotNumber;
        this.createdAt = createdAt;
        this.rejectionReason = rejectionReason;
        this.locationName = locationName;
        this.pillarCodes = pillarCodes;
        this.treeName = treeName;
        this.isEarlyHarvest = isEarlyHarvest;
        this.customerName = customerName;
    }
}
