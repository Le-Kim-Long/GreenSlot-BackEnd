package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PillarIoTStatusDTO {
    private Long pillarId;
    private String pillarCode;
    private String pillarType;
    private Long slotId;
    private String slotNumber;
    private Long locationId;
    private String locationName;
    private List<EquipmentDTO> equipments;
    private List<SensorReadingResponseDTO> latestReadings;
    private boolean hasSignal;
    private Instant lastSignalAt;
    private String deviceStatus;
    private String cameraStatus;
    private String cameraStreamUrl;
}
