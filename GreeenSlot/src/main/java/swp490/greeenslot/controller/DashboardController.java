package swp490.greeenslot.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/public")
    public String publicAccess() {
        return "Trang cong khai - tat ca deu truy cap duoc.";
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public String customerAccess() {
        return "Trang danh rieng cho Customer.";
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_FARMER')")
    public String staffAccess() {
        return "Trang danh rieng cho nhan vien (Admin, Manager, Farmer).";
    }
}
