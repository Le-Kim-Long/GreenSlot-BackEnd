package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swp490.greeenslot.service.BookingService;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Endpoints for handling online payment callbacks")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private BookingService bookingService;

    @Value("${greeenslot.vnpay.frontendReturnUrl:${FRONTEND_RETURN_URL:http://localhost:5173/payment-result}}")
    private String defaultReturnUrl;

    @Value("${greeenslot.vnpay.mobileReturnUrl:${MOBILE_RETURN_URL:greenslot://payment-result}}")
    private String mobileReturnUrl;

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

    @GetMapping("/vnpay-return")
    @Operation(summary = "VNPay Return callback redirector", description = "Processes VNPay return parameters and redirects browser securely to Frontend SPA or Mobile App.")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            if (values.length > 0) {
                fields.put(name, values[0]);
            }
        }
        String txnRef = fields.getOrDefault("vnp_TxnRef", "");
        Map<String, String> ipnResult;
        try {
            ipnResult = bookingService.processIpn(fields);
        } catch (Exception e) {
            logger.error("Error processing VNPay return callback for txnRef={}", txnRef, e);
            ipnResult = new HashMap<>();
        }

        // Trạng thái hiển thị cho người dùng phải dựa trên kết quả xử lý/lưu DB thực tế
        // (TxnStatus), không được suy ra trực tiếp từ vnp_ResponseCode do client gửi lên —
        // vì processIpn có thể thất bại (sai chữ ký, không tìm thấy giao dịch, sai số tiền...)
        // trong khi vnp_ResponseCode vẫn báo "00".
        String txnStatus = ipnResult.getOrDefault("TxnStatus", "");
        String responseCode = fields.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = fields.getOrDefault("vnp_TransactionStatus", "");
        String amount = fields.getOrDefault("vnp_Amount", "");
        String orderInfo = fields.getOrDefault("vnp_OrderInfo", "");
        String payDate = fields.getOrDefault("vnp_PayDate", "");
        String status = "SUCCESS".equals(txnStatus) ? "success" : "failed";

        // Determine whether target is Mobile App or Web Frontend
        String targetUrl = defaultReturnUrl;
        String clientParam = fields.getOrDefault("client", fields.getOrDefault("source", ""));
        String isMobileParam = fields.get("isMobile");
        String customRedirectUrl = fields.get("redirectUrl");
        String userAgent = request.getHeader("User-Agent");

        if (customRedirectUrl != null && !customRedirectUrl.isEmpty()) {
            targetUrl = customRedirectUrl;
        } else if ("mobile".equalsIgnoreCase(clientParam)
                || "true".equalsIgnoreCase(isMobileParam)
                || (userAgent != null && (userAgent.contains("GreenSlotMobile") || userAgent.contains("Expo") || userAgent.contains("okhttp")))) {
            targetUrl = mobileReturnUrl;
        }

        String delimiter = targetUrl.contains("?") ? "&" : "?";
        String redirectUrl = targetUrl + delimiter
                + "status=" + urlEncode(status)
                + "&vnp_ResponseCode=" + urlEncode(responseCode)
                + "&vnp_TxnRef=" + urlEncode(txnRef)
                + "&vnp_TransactionStatus=" + urlEncode(transactionStatus)
                + "&vnp_Amount=" + urlEncode(amount)
                + "&vnp_OrderInfo=" + urlEncode(orderInfo)
                + "&vnp_PayDate=" + urlEncode(payDate);
        response.sendRedirect(redirectUrl);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
