package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swp490.greeenslot.service.BookingService;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Endpoints for handling online payment callbacks")
public class PaymentController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/vnpay-ipn")
    @Operation(summary = "VNPay IPN callback listener", description = "Performs secure checksum validation, amount checking, and state updates, returning JSON response to VNPay.")
    public ResponseEntity<Map<String, String>> vnpayIpn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            if (values.length > 0) {
                fields.put(name, values[0]);
            }
        }

        Map<String, String> result = bookingService.processIpn(fields);
        return ResponseEntity.ok(result);
    }
}
