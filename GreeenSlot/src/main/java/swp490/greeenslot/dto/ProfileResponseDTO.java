package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import swp490.greeenslot.entity.User;

@Schema(description = "DTO representing the user profile information returned from GET /api/users/profile")
public class ProfileResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String imageUrl;
    private Long locationId;
    private String locationName;

    public ProfileResponseDTO() {}

    public ProfileResponseDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.address = user.getAddress();
        this.imageUrl = user.getImageUrl();
        if (user.getLocation() != null) {
            this.locationId = user.getLocation().getId();
            this.locationName = user.getLocation().getName();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
