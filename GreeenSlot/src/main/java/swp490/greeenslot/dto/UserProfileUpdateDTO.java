package swp490.greeenslot.dto;

import jakarta.validation.constraints.Size;

public class UserProfileUpdateDTO {

    private String fullName;

    @Size(min = 10, max = 15, message = "Số điện thoại phải từ 10 đến 15 ký tự")
    private String phone;

    private String address;
    private String imageUrl;

    public UserProfileUpdateDTO() {
    }

    public UserProfileUpdateDTO(String fullName, String phone, String address, String imageUrl) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.imageUrl = imageUrl;
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
}
