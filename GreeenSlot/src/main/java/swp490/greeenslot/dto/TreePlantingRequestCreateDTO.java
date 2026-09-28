package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreePlantingRequestCreateDTO {
    @NotNull(message = "Hợp đồng thuê ô đất không được để trống")
    private Long rentalId;
    
    @NotNull(message = "Giống cây trồng không được để trống")
    private Long newTreeId;
    
    @Nationalized
    private String reason;
    
    @Nationalized
    private String notes;

    private Boolean isMobile;
    
    private Long targetPillarId;

    private String redirectUrl;

    private String mobileRedirectUrl;

    public String getEffectiveRedirectUrl() {
        return redirectUrl != null ? redirectUrl : mobileRedirectUrl;
    }
}

