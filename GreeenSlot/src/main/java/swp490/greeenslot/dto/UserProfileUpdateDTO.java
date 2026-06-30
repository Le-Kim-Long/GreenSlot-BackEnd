package swp490.greeenslot.dto;

import jakarta.validation.constraints.Size;

public class UserProfileUpdateDTO {

    private String fullName;

    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters")
    private String phone;

    private String address;

    public UserProfileUpdateDTO() {
    }

    public UserProfileUpdateDTO(String fullName, String phone, String address) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
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
}
