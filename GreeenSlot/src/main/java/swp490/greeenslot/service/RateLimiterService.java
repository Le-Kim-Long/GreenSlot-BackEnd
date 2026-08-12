package swp490.greeenslot.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, RequestData> cache = new ConcurrentHashMap<>();
    
    // Limits: 5 requests per minute
    private static final int MAX_REQUESTS = 5;
    private static final long TIME_WINDOW_MS = TimeUnit.MINUTES.toMillis(1);

    public boolean isAllowed(String ipAddress) {
        long now = System.currentTimeMillis();
        cache.compute(ipAddress, (key, value) -> {
            if (value == null || (now - value.timestamp) > TIME_WINDOW_MS) {
                return new RequestData(1, now);
            } else {
                value.count++;
                return value;
            }
        });
        
        RequestData data = cache.get(ipAddress);
        return data != null && data.count <= MAX_REQUESTS;
    }

    private static class RequestData {
        int count;
        long timestamp;

        RequestData(int count, long timestamp) {
            this.count = count;
            this.timestamp = timestamp;
        }
    }
}
