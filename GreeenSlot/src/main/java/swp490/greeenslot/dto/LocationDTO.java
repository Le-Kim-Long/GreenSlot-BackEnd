package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private Long id;

    @NotBlank(message = "Tên cơ sở không được để trống")
    private String name;

    @NotBlank(message = "Địa chỉ cơ sở không được để trống")
    private String address;

    private String contactPhone;

    private String status;

    @NotNull(message = "Diện tích cơ sở không được để trống")
    @Positive(message = "Diện tích cơ sở phải lớn hơn 0")
    private Double area;

    private String imageUrl;
}
