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

    @Autowired
    private swp490.greeenslot.repository.GardenSlotRepository gardenSlotRepository;

    @Autowired
    private swp490.greeenslot.service.LocationContextService locationContextService;

    @Override
    public List<StaffScheduleDTO> getAllSchedules() {
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        return staffScheduleRepository.findAll().stream()
                .filter(s -> targetLocationId == null || (s.getLocation() != null && targetLocationId.equals(s.getLocation().getId())))
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
        validateScheduleDates(dto, null);
        StaffSchedule schedule = mapToEntity(dto);
        StaffSchedule savedSchedule = staffScheduleRepository.save(schedule);
        return mapToDTO(savedSchedule);
    }

    @Override
    @Transactional
    public StaffScheduleDTO updateSchedule(Long id, StaffScheduleDTO dto) {
        validateScheduleDates(dto, id);
        StaffSchedule existingSchedule = staffScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        
        updateEntityFromDTO(existingSchedule, dto);
        StaffSchedule updatedSchedule = staffScheduleRepository.save(existingSchedule);
        return mapToDTO(updatedSchedule);
    }

    private void validateScheduleDates(StaffScheduleDTO dto, Long currentScheduleId) {
        if (dto.getScheduleDate() != null && dto.getScheduleDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Schedule date cannot be in the past");
        }
        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            if (!dto.getStartTime().isBefore(dto.getEndTime())) {
                throw new IllegalArgumentException("Start time must be before end time");
            }
            long minutes = java.time.Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
            if (minutes <= 0) {
                throw new IllegalArgumentException("Thời gian làm việc phải lớn hơn 0 phút");
            }
            if (minutes > 8 * 60) {
                throw new IllegalArgumentException("Thời gian làm việc một ca không được vượt quá 8 tiếng (tối đa 8 giờ/ngày)");
            }

            if (dto.getStaffId() != null && dto.getScheduleDate() != null) {
                User staff = userRepository.findById(dto.getStaffId()).orElse(null);
                if (staff != null) {
                    List<StaffSchedule> existingSchedules = staffScheduleRepository.findByStaff(staff).stream()
                            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                            .filter(s -> dto.getScheduleDate().equals(s.getScheduleDate()))
                            .filter(s -> currentScheduleId == null || !s.getId().equals(currentScheduleId))
                            .toList();

                    long otherMinutes = existingSchedules.stream()
                            .mapToLong(s -> java.time.Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                            .sum();

                    if (otherMinutes + minutes > 8 * 60) {
                        double totalHours = (otherMinutes + minutes) / 60.0;
                        throw new IllegalArgumentException(String.format(
                                "Tổng thời gian làm việc trong ngày %s của nhân viên không được vượt quá 8 tiếng (Tổng phân công: %.1f tiếng)",
                                dto.getScheduleDate(), totalHours));
                    }
                }
            }
        }
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
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        return staffScheduleRepository.findByScheduleDate(date).stream()
                .filter(s -> targetLocationId == null || (s.getLocation() != null && targetLocationId.equals(s.getLocation().getId())))
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
                schedule.getGardenSlot() != null ? schedule.getGardenSlot().getId() : null,
                schedule.getGardenSlot() != null ? schedule.getGardenSlot().getSlotNumber() : null,
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
        if (dto.getSlotId() != null) {
            gardenSlotRepository.findById(dto.getSlotId()).ifPresent(schedule::setGardenSlot);
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
        if (dto.getSlotId() != null) {
            gardenSlotRepository.findById(dto.getSlotId()).ifPresent(schedule::setGardenSlot);
        } else if (dto.getSlotNumber() == null) {
            schedule.setGardenSlot(null);
        }
        if (dto.getScheduleDate() != null) schedule.setScheduleDate(dto.getScheduleDate());
        if (dto.getStartTime() != null) schedule.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) schedule.setEndTime(dto.getEndTime());
        if (dto.getNotes() != null) schedule.setNotes(dto.getNotes());
        if (dto.getIsActive() != null) schedule.setIsActive(dto.getIsActive());
    }
}
