package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.SensorThreshold;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorThresholdRepository extends JpaRepository<SensorThreshold, Long> {
    Optional<SensorThreshold> findByDeviceIdAndSensorType(String deviceId, String sensorType);
    List<SensorThreshold> findByDeviceId(String deviceId);
}
