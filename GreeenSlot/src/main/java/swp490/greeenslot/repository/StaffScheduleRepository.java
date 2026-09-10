package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.StaffSchedule;
import swp490.greeenslot.entity.User;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, Long> {
    
    List<StaffSchedule> findByStaff(User staff);
    
    List<StaffSchedule> findByLocation(Location location);
    
    @Query("SELECT s FROM StaffSchedule s WHERE s.scheduleDate <= :date AND (s.endDate IS NULL OR s.endDate >= :date)")
    List<StaffSchedule> findByScheduleDate(@Param("date") LocalDate date);
    
    @Query("SELECT s FROM StaffSchedule s WHERE s.staff.id = :staffId AND s.scheduleDate <= :endDate AND (s.endDate IS NULL OR s.endDate >= :startDate)")
    List<StaffSchedule> findByStaffAndDateRange(@Param("staffId") Long staffId, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
    
    @Query("SELECT s FROM StaffSchedule s WHERE s.location.id = :locationId AND s.scheduleDate <= :date AND (s.endDate IS NULL OR s.endDate >= :date)")
    List<StaffSchedule> findByLocationAndDate(@Param("locationId") Long locationId, 
                                               @Param("date") LocalDate date);
}
