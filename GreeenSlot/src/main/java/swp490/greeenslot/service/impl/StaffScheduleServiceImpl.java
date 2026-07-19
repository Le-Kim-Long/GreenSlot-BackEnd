package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.StaffScheduleDTO;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.StaffSchedule;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.LocationRepository;
import swp490.greeenslot.repository.StaffScheduleRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.StaffScheduleService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffScheduleServiceImpl implements StaffScheduleService {

    @Autowired
    private StaffScheduleRepository staffScheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Override
    public List<StaffScheduleDTO> getAllSchedules() {
        return staffScheduleRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public StaffScheduleDTO getScheduleById(Long id) {
        return staffScheduleRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
    }

    @Override
    @Transactional
    public StaffScheduleDTO createSchedule(StaffScheduleDTO dto) {
        StaffSchedule schedule = mapToEntity(dto);
        StaffSchedule savedSchedule = staffScheduleRepository.save(schedule);
        return mapToDTO(savedSchedule);
    }

    @Override
    @Transactional
    public StaffScheduleDTO updateSchedule(Long id, StaffScheduleDTO dto) {
        StaffSchedule existingSchedule = staffScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        
        updateEntityFromDTO(existingSchedule, dto);
        StaffSchedule updatedSchedule = staffScheduleRepository.save(existingSchedule);
        return mapToDTO(updatedSchedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        StaffSchedule schedule = staffScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        schedule.setIsActive(false);
        staffScheduleRepository.save(schedule);
    }

    @Override
    public List<StaffScheduleDTO> getSchedulesByStaff(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found with id: " + staffId));
        return staffScheduleRepository.findByStaff(staff).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffScheduleDTO> getSchedulesByLocation(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + locationId));
        return staffScheduleRepository.findByLocation(location).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffScheduleDTO> getSchedulesByDate(LocalDate date) {
        return staffScheduleRepository.findByScheduleDate(date).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffScheduleDTO> getSchedulesByLocationAndDate(Long locationId, LocalDate date) {
        return staffScheduleRepository.findByLocationAndDate(locationId, date).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private StaffScheduleDTO mapToDTO(StaffSchedule schedule) {
        return new StaffScheduleDTO(
                schedule.getId(),
                schedule.getStaff() != null ? schedule.getStaff().getId() : null,
                schedule.getStaff() != null ? schedule.getStaff().getFullName() : null,
                schedule.getLocation() != null ? schedule.getLocation().getId() : null,
                schedule.getLocation() != null ? schedule.getLocation().getName() : null,
                schedule.getScheduleDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getNotes(),
                schedule.getIsActive()
        );
    }

    private StaffSchedule mapToEntity(StaffScheduleDTO dto) {
        StaffSchedule schedule = new StaffSchedule();
        if (dto.getStaffId() != null) {
            User staff = userRepository.findById(dto.getStaffId())
                    .orElseThrow(() -> new RuntimeException("Staff not found with id: " + dto.getStaffId()));
            schedule.setStaff(staff);
        }
        if (dto.getLocationId() != null) {
            Location location = locationRepository.findById(dto.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Location not found with id: " + dto.getLocationId()));
            schedule.setLocation(location);
        }
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setNotes(dto.getNotes());
        schedule.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return schedule;
    }

    private void updateEntityFromDTO(StaffSchedule schedule, StaffScheduleDTO dto) {
        if (dto.getStaffId() != null) {
            User staff = userRepository.findById(dto.getStaffId())
                    .orElseThrow(() -> new RuntimeException("Staff not found with id: " + dto.getStaffId()));
            schedule.setStaff(staff);
        }
        if (dto.getLocationId() != null) {
            Location location = locationRepository.findById(dto.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Location not found with id: " + dto.getLocationId()));
            schedule.setLocation(location);
        }
        if (dto.getScheduleDate() != null) schedule.setScheduleDate(dto.getScheduleDate());
        if (dto.getStartTime() != null) schedule.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) schedule.setEndTime(dto.getEndTime());
        if (dto.getNotes() != null) schedule.setNotes(dto.getNotes());
        if (dto.getIsActive() != null) schedule.setIsActive(dto.getIsActive());
    }
}
