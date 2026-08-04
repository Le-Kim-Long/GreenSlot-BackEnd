package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthDTO {
    
    private String status; // UP, DOWN, DEGRADED
    
    private Long timestamp;
    
    private CpuInfo cpu;
    
    private MemoryInfo memory;
    
    private DatabaseInfo database;
    
    private DiskInfo disk;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CpuInfo {
        private Double usagePercent;
        private Integer availableProcessors;
        private String systemLoadAverage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryInfo {
        private Long totalMemory;
        private Long freeMemory;
        private Long usedMemory;
        private Double usagePercent;
        private Long maxMemory;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseInfo {
        private String status;
        private Long activeConnections;
        private Long queryCount;
        private Double avgResponseTime;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskInfo {
        private Long totalSpace;
        private Long freeSpace;
        private Long usedSpace;
        private Double usagePercent;
    }
}
