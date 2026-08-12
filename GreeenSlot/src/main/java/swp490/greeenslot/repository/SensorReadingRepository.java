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

    // Pillar-based aggregation queries.
    // nativeQuery=true vi CONVERT/DATEADD/DATEDIFF la ham SQL Server, khong phai ham JPQL chuan.
    // Bucket tra ve la 1 moc DATETIME2 dai dien (dau gio/ngay/tuan) thay vi String/DATE/so tuan
    // de tranh loi ep kieu va co the sap xep/parse nhat quan o tang service.
    @Query(value = "SELECT AVG(sr.value) as avgValue, MIN(sr.value) as minValue, MAX(sr.value) as maxValue, " +
           "COUNT(*) as readingCount, DATEADD(HOUR, DATEDIFF(HOUR, 0, sr.recorded_at), 0) as bucketTime " +
           "FROM sensor_readings sr JOIN equipment e ON sr.device_id = e.serial_number " +
           "WHERE e.pillar_id = :pillarId AND sr.sensor_type = :sensorType " +
           "AND sr.recorded_at >= :startTime AND sr.recorded_at <= :endTime " +
           "GROUP BY DATEADD(HOUR, DATEDIFF(HOUR, 0, sr.recorded_at), 0) " +
           "ORDER BY bucketTime ASC", nativeQuery = true)
    List<Object[]> findHourlyAggregatesByPillar(
            @Param("pillarId") Long pillarId,
            @Param("sensorType") String sensorType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query(value = "SELECT AVG(sr.value) as avgValue, MIN(sr.value) as minValue, MAX(sr.value) as maxValue, " +
           "COUNT(*) as readingCount, DATEADD(DAY, DATEDIFF(DAY, 0, sr.recorded_at), 0) as bucketTime " +
           "FROM sensor_readings sr JOIN equipment e ON sr.device_id = e.serial_number " +
           "WHERE e.pillar_id = :pillarId AND sr.sensor_type = :sensorType " +
           "AND sr.recorded_at >= :startTime AND sr.recorded_at <= :endTime " +
           "GROUP BY DATEADD(DAY, DATEDIFF(DAY, 0, sr.recorded_at), 0) " +
           "ORDER BY bucketTime ASC", nativeQuery = true)
    List<Object[]> findDailyAggregatesByPillar(
            @Param("pillarId") Long pillarId,
            @Param("sensorType") String sensorType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query(value = "SELECT AVG(sr.value) as avgValue, MIN(sr.value) as minValue, MAX(sr.value) as maxValue, " +
           "COUNT(*) as readingCount, DATEADD(WEEK, DATEDIFF(WEEK, 0, sr.recorded_at), 0) as bucketTime " +
           "FROM sensor_readings sr JOIN equipment e ON sr.device_id = e.serial_number " +
           "WHERE e.pillar_id = :pillarId AND sr.sensor_type = :sensorType " +
           "AND sr.recorded_at >= :startTime AND sr.recorded_at <= :endTime " +
           "GROUP BY DATEADD(WEEK, DATEDIFF(WEEK, 0, sr.recorded_at), 0) " +
           "ORDER BY bucketTime ASC", nativeQuery = true)
    List<Object[]> findWeeklyAggregatesByPillar(
            @Param("pillarId") Long pillarId,
            @Param("sensorType") String sensorType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
