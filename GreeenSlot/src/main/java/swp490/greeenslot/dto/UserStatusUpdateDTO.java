package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateDTO {
    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}
