package swp490.greeenslot.service;

import swp490.greeenslot.dto.StaffScheduleDTO;

import java.time.LocalDate;
import java.util.List;

public interface StaffScheduleService {
    
    List<StaffScheduleDTO> getAllSchedules();
    
    StaffScheduleDTO getScheduleById(Long id);
    
    StaffScheduleDTO createSchedule(StaffScheduleDTO dto);
    
    StaffScheduleDTO updateSchedule(Long id, StaffScheduleDTO dto);
    
    void deleteSchedule(Long id);
    
    List<StaffScheduleDTO> getSchedulesByStaff(Long staffId);
    
    List<StaffScheduleDTO> getSchedulesByLocation(Long locationId);
    
    List<StaffScheduleDTO> getSchedulesByDate(LocalDate date);
    
    List<StaffScheduleDTO> getSchedulesByLocationAndDate(Long locationId, LocalDate date);
}
