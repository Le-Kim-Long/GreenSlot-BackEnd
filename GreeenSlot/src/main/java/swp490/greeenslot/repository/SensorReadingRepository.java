package swp490.greeenslot.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.ESensorType;
import swp490.greeenslot.entity.SensorReading;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findByDeviceIdOrderByRecordedAtDesc(String deviceId, Pageable pageable);

    List<SensorReading> findByDeviceIdAndSensorTypeOrderByRecordedAtDesc(
            String deviceId, ESensorType sensorType, Pageable pageable);

    Optional<SensorReading> findFirstByDeviceIdAndSensorTypeOrderByRecordedAtDesc(
            String deviceId, ESensorType sensorType);

    @Query("SELECT sr FROM SensorReading sr WHERE sr.deviceId = :deviceId " +
           "AND sr.recordedAt >= :startTime AND sr.recordedAt <= :endTime ORDER BY sr.recordedAt ASC")
    List<SensorReading> findByDeviceIdAndTimeRange(
            @Param("deviceId") String deviceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT sr FROM SensorReading sr WHERE sr.deviceId = :deviceId AND sr.sensorType = :sensorType " +
           "AND sr.recordedAt >= :startTime AND sr.recordedAt <= :endTime ORDER BY sr.recordedAt ASC")
    List<SensorReading> findByDeviceIdAndSensorTypeAndTimeRange(
            @Param("deviceId") String deviceId,
            @Param("sensorType") ESensorType sensorType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT DATE(sr.recordedAt) as date, AVG(sr.value) as avgValue, MIN(sr.value) as minValue, " +
           "MAX(sr.value) as maxValue, COUNT(sr) as count FROM SensorReading sr " +
           "WHERE sr.deviceId = :deviceId AND sr.sensorType = :sensorType " +
           "AND sr.recordedAt >= :startTime AND sr.recordedAt <= :endTime " +
           "GROUP BY DATE(sr.recordedAt) ORDER BY DATE(sr.recordedAt) ASC")
    List<Object[]> findDailyAggregatedData(
            @Param("deviceId") String deviceId,
            @Param("sensorType") ESensorType sensorType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT HOUR(sr.recordedAt) as hour, AVG(sr.value) as avgValue, MIN(sr.value) as minValue, " +
           "MAX(sr.value) as maxValue, COUNT(sr) as count FROM SensorReading sr " +
           "WHERE sr.deviceId = :deviceId AND sr.sensorType = :sensorType " +
           "AND sr.recordedAt >= :startTime AND sr.recordedAt <= :endTime " +
           "GROUP BY HOUR(sr.recordedAt) ORDER BY HOUR(sr.recordedAt) ASC")
    List<Object[]> findHourlyAggregatedData(
            @Param("deviceId") String deviceId,
            @Param("sensorType") ESensorType sensorType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    // Pillar-based aggregation queries
    @Query("SELECT AVG(sr.value) as avgValue, MIN(sr.value) as minValue, MAX(sr.value) as maxValue, " +
           "COUNT(sr) as count, CONVERT(VARCHAR(13), sr.recordedAt, 120) as hourBucket " +
           "FROM SensorReading sr JOIN Equipment e ON sr.deviceId = e.serialNumber " +
           "WHERE e.pillar.id = :pillarId AND sr.sensorType = :sensorType " +
           "AND sr.recordedAt >= :startTime AND sr.recordedAt <= :endTime " +
           "GROUP BY CONVERT(VARCHAR(13), sr.recordedAt, 120) " +
           "ORDER BY CONVERT(VARCHAR(13), sr.recordedAt, 120) ASC")
    List<Object[]> findHourlyAggregatesByPillar(
            @Param("pillarId") Long pillarId,
            @Param("sensorType") ESensorType sensorType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT AVG(sr.value) as avgValue, MIN(sr.value) as minValue, MAX(sr.value) as maxValue, " +
           "COUNT(sr) as count, CAST(CONVERT(VARCHAR(10), sr.recordedAt, 120) AS DATE) as dateBucket " +
           "FROM SensorReading sr JOIN Equipment e ON sr.deviceId = e.serialNumber " +
           "WHERE e.pillar.id = :pillarId AND sr.sensorType = :sensorType " +
           "AND sr.recordedAt >= :startTime AND sr.recordedAt <= :endTime " +
           "GROUP BY CAST(CONVERT(VARCHAR(10), sr.recordedAt, 120) AS DATE) " +
           "ORDER BY CAST(CONVERT(VARCHAR(10), sr.recordedAt, 120) AS DATE) ASC")
    List<Object[]> findDailyAggregatesByPillar(
            @Param("pillarId") Long pillarId,
            @Param("sensorType") ESensorType sensorType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT AVG(sr.value) as avgValue, MIN(sr.value) as minValue, MAX(sr.value) as maxValue, " +
           "COUNT(sr) as count, DATEPART(WEEK, sr.recordedAt) as weekBucket " +
           "FROM SensorReading sr JOIN Equipment e ON sr.deviceId = e.serialNumber " +
           "WHERE e.pillar.id = :pillarId AND sr.sensorType = :sensorType " +
           "AND sr.recordedAt >= :startTime AND sr.recordedAt <= :endTime " +
           "GROUP BY DATEPART(WEEK, sr.recordedAt) " +
           "ORDER BY DATEPART(WEEK, sr.recordedAt) ASC")
    List<Object[]> findWeeklyAggregatesByPillar(
            @Param("pillarId") Long pillarId,
            @Param("sensorType") ESensorType sensorType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
