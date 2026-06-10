package swp490.greeenslot.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.ESensorType;
import swp490.greeenslot.entity.SensorReading;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findByDeviceIdOrderByRecordedAtDesc(String deviceId, Pageable pageable);

    List<SensorReading> findByDeviceIdAndSensorTypeOrderByRecordedAtDesc(
            String deviceId, ESensorType sensorType, Pageable pageable);

    Optional<SensorReading> findFirstByDeviceIdAndSensorTypeOrderByRecordedAtDesc(
            String deviceId, ESensorType sensorType);
}
