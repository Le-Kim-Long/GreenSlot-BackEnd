package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthorityUpdateDTO {
    @NotEmpty(message = "Danh sách vai trò không được để trống")
    private List<String> roles;
}
