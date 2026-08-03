package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp490.greeenslot.dto.SystemHealthDTO;
import swp490.greeenslot.service.SystemHealthService;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Instant;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    @Autowired(required = false)
    private DataSource dataSource;

    @Override
    public SystemHealthDTO getSystemHealth() {
        SystemHealthDTO health = new SystemHealthDTO();
        health.setTimestamp(Instant.now().toEpochMilli());
        
        // CPU Info
        SystemHealthDTO.CpuInfo cpuInfo = getCpuInfo();
        health.setCpu(cpuInfo);
        
        // Memory Info
        SystemHealthDTO.MemoryInfo memoryInfo = getMemoryInfo();
        health.setMemory(memoryInfo);
        
        // Database Info
        SystemHealthDTO.DatabaseInfo databaseInfo = getDatabaseInfo();
        health.setDatabase(databaseInfo);
        
        // Disk Info
        SystemHealthDTO.DiskInfo diskInfo = getDiskInfo();
        health.setDisk(diskInfo);
        
        // Overall status
        if (cpuInfo.getUsagePercent() > 90 || memoryInfo.getUsagePercent() > 90) {
            health.setStatus("DEGRADED");
        } else if (databaseInfo.getStatus().equals("DOWN")) {
            health.setStatus("DOWN");
        } else {
            health.setStatus("UP");
        }
        
        return health;
    }

    private SystemHealthDTO.CpuInfo getCpuInfo() {
        SystemHealthDTO.CpuInfo cpuInfo = new SystemHealthDTO.CpuInfo();
        
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        cpuInfo.setAvailableProcessors(availableProcessors);
        
        // Get system load average (Unix-like systems only)
        double loadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        cpuInfo.setSystemLoadAverage(loadAverage >= 0 ? String.valueOf(loadAverage) : "N/A");
        
        // CPU usage estimation (simplified)
        // In production, you'd use a more accurate method like OS-specific commands or libraries
        cpuInfo.setUsagePercent(loadAverage / availableProcessors * 100);
        
        return cpuInfo;
    }

    private SystemHealthDTO.MemoryInfo getMemoryInfo() {
        SystemHealthDTO.MemoryInfo memoryInfo = new SystemHealthDTO.MemoryInfo();
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        memoryInfo.setTotalMemory(totalMemory);
        memoryInfo.setFreeMemory(freeMemory);
        memoryInfo.setUsedMemory(usedMemory);
        memoryInfo.setMaxMemory(maxMemory);
        memoryInfo.setUsagePercent((double) usedMemory / maxMemory * 100);
        
        return memoryInfo;
    }

    private SystemHealthDTO.DatabaseInfo getDatabaseInfo() {
        SystemHealthDTO.DatabaseInfo databaseInfo = new SystemHealthDTO.DatabaseInfo();
        
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                databaseInfo.setStatus("UP");
                databaseInfo.setActiveConnections((long) connection.getMetaData().getMaxConnections());
                databaseInfo.setQueryCount(0L); // Would need query tracking implementation
                databaseInfo.setAvgResponseTime(0.0); // Would need query timing implementation
            } catch (Exception e) {
                databaseInfo.setStatus("DOWN");
                databaseInfo.setActiveConnections(0L);
                databaseInfo.setQueryCount(0L);
                databaseInfo.setAvgResponseTime(0.0);
            }
        } else {
            databaseInfo.setStatus("UNKNOWN");
            databaseInfo.setActiveConnections(0L);
            databaseInfo.setQueryCount(0L);
            databaseInfo.setAvgResponseTime(0.0);
        }
        
        return databaseInfo;
    }

    private SystemHealthDTO.DiskInfo getDiskInfo() {
        SystemHealthDTO.DiskInfo diskInfo = new SystemHealthDTO.DiskInfo();
        
        File root = new File("/");
        if (root.exists()) {
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            diskInfo.setTotalSpace(totalSpace);
            diskInfo.setFreeSpace(freeSpace);
            diskInfo.setUsedSpace(usedSpace);
            diskInfo.setUsagePercent((double) usedSpace / totalSpace * 100);
        } else {
            // Try current directory if root doesn't exist (Windows)
            File currentDir = new File(".");
            long totalSpace = currentDir.getTotalSpace();
            long freeSpace = currentDir.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            diskInfo.setTotalSpace(totalSpace);
            diskInfo.setFreeSpace(freeSpace);
            diskInfo.setUsedSpace(usedSpace);
            diskInfo.setUsagePercent((double) usedSpace / totalSpace * 100);
        }
        
        return diskInfo;
    }
}
