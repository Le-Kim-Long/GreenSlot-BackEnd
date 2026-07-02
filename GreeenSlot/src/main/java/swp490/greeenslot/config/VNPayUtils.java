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

    @Value("${greeenslot.vnpay.url}")
    private String url;

    @Value("${greeenslot.vnpay.returnUrl}")
    private String returnUrl;

    public String buildPaymentUrl(String txnRef, BigDecimal amount, String ipAddress, String orderInfo) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = txnRef;
        String vnp_OrderInfo = orderInfo;
        String vnp_OrderType = "other";
        // VNPay expects amount in cents/VND without decimals (e.g. multiplied by 100)
        String vnp_Amount = amount.multiply(new BigDecimal(100)).setScale(0).toString();
        String vnp_Locale = "vn";
        String vnp_IpAddr = ipAddress;

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnp_CreateDate = now.format(formatter);

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
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

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

    public boolean verifySignature(Map<String, String> fields) {
        String vnp_SecureHash = fields.get("vnp_SecureHash");
        if (vnp_SecureHash == null) {
            logger.warn("VNPay verifySignature failed: vnp_SecureHash is missing");
            return false;
        }

        // Sort keys and exclude vnp_SecureHash & vnp_SecureHashType
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        fieldNames.remove("vnp_SecureHash");
        fieldNames.remove("vnp_SecureHashType");
        Collections.sort(fieldNames);

        // Build hash data using StringJoiner to avoid trailing '&' when values are empty
        StringJoiner hashData = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.add(fieldName + "=" + encode(fieldValue));
            }
        }

        String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
        boolean isValid = calculatedHash.equalsIgnoreCase(vnp_SecureHash);
        if (!isValid) {
            logger.error("VNPay signature verification failed. Calculated: {}, Received: {}", calculatedHash, vnp_SecureHash);
            logger.debug("Raw hash data string used: {}", hashData.toString());
        }
        return isValid;
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
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
