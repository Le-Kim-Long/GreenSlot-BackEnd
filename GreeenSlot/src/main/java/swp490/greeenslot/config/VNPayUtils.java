package swp490.greeenslot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class VNPayUtils {

    private static final Logger logger = LoggerFactory.getLogger(VNPayUtils.class);

    @Value("${greeenslot.vnpay.tmnCode}")
    private String tmnCode;

    @Value("${greeenslot.vnpay.hashSecret}")
    private String hashSecret;

    @Value("${greeenslot.vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String url;

    @Value("${greeenslot.vnpay.returnUrl:https://greenslot-backend.onrender.com/api/payments/vnpay-return}")
    private String returnUrl;

    @Value("${greeenslot.vnpay.mobileReturnUrl:greenslot://payment-result}")
    private String mobileReturnUrl;

    @Value("${greeenslot.vnpay.ipnUrl:}")
    private String ipnUrl;

    public String buildPaymentUrl(String txnRef, BigDecimal amount, String ipAddress, String orderInfo) {
        return buildPaymentUrl(txnRef, amount, ipAddress, orderInfo, false);
    }

    public String buildPaymentUrl(String txnRef, BigDecimal amount, String ipAddress, String orderInfo, boolean isMobile) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = txnRef;
        String vnp_OrderInfo = sanitizeOrderInfo(orderInfo);
        String vnp_OrderType = "other";
        
        // VNPay expects amount in cents/VND without decimals (e.g. multiplied by 100)
        long vnpAmountLong = amount.multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        String vnp_Amount = String.valueOf(vnpAmountLong);
        String vnp_Locale = "vn";

        // Sanitize IP Address to strictly valid IPv4 (reject IPv6 like 0:0:0:0:0:0:0:1)
        String vnp_IpAddr = ipAddress;
        if (vnp_IpAddr == null || vnp_IpAddr.isBlank() || vnp_IpAddr.contains(":") || "localhost".equalsIgnoreCase(vnp_IpAddr)) {
            vnp_IpAddr = "127.0.0.1";
        } else if (vnp_IpAddr.contains(",")) {
            vnp_IpAddr = vnp_IpAddr.split(",")[0].trim();
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnp_CreateDate = now.format(formatter);
        String vnp_ExpireDate = now.plusMinutes(15).format(formatter);

        String effectiveReturnUrl = returnUrl;
        if (isMobile) {
            effectiveReturnUrl += (effectiveReturnUrl.contains("?") ? "&" : "?") + "client=mobile";
        }

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", vnp_Amount);
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", vnp_Locale);
        vnp_Params.put("vnp_ReturnUrl", effectiveReturnUrl);
        if (ipnUrl != null && !ipnUrl.isEmpty()) {
            vnp_Params.put("vnp_IpnUrl", ipnUrl);
        }
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Sort keys alphabetically
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        // Use StringJoiner to avoid trailing '&' bug
        StringJoiner hashData = new StringJoiner("&");
        StringJoiner query = new StringJoiner("&");

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                // Build hash data (VNPay requires UTF-8 encoding)
                hashData.add(fieldName + "=" + encode(fieldValue));

                // Build query
                query.add(encode(fieldName) + "=" + encode(fieldValue));
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(hashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return url + "?" + queryUrl;
    }

    private String sanitizeOrderInfo(String input) {
        if (input == null || input.isBlank()) {
            return "GreenSlot Payment";
        }
        String temp = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccents = pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
        return noAccents.replaceAll("[^a-zA-Z0-9 \\-_#.]", " ").replaceAll("\\s+", " ").trim();
    }

    public boolean verifySignature(Map<String, String> fields) {
        String vnp_SecureHash = fields.get("vnp_SecureHash");
        if (vnp_SecureHash == null) {
            logger.warn("VNPay verifySignature failed: vnp_SecureHash is missing");
            return false;
        }

        // Filter only vnp_ fields and exclude vnp_SecureHash & vnp_SecureHashType
        List<String> fieldNames = fields.keySet().stream()
                .filter(k -> k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType"))
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        // Build hash data using StringJoiner to avoid trailing '&' when values are empty
        StringJoiner hashData = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.add(fieldName + "=" + encode(fieldValue));
            }
        }

        String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
        if (calculatedHash.equalsIgnoreCase(vnp_SecureHash)) {
            return true;
        }

        // Also check unencoded raw values in case Spring or proxy decoded them differently
        StringJoiner hashDataRaw = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashDataRaw.add(fieldName + "=" + fieldValue);
            }
        }
        String calculatedHashRaw = hmacSHA512(hashSecret, hashDataRaw.toString());
        if (calculatedHashRaw.equalsIgnoreCase(vnp_SecureHash)) {
            return true;
        }

        // Also check percent-encoded spaces (%20) instead of '+'
        String calculatedHashPercent = hmacSHA512(hashSecret, hashData.toString().replace("+", "%20"));
        if (calculatedHashPercent.equalsIgnoreCase(vnp_SecureHash)) {
            return true;
        }

        logger.error("VNPay signature verification failed. Calculated: {}, Received: {}", calculatedHash, vnp_SecureHash);
        logger.debug("Raw hash data string used: {}", hashData.toString());

        return false;
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return "";
        }
    }

    public String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Key or data cannot be null");
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate HMAC-SHA512 signature", ex);
        }
    }
}
