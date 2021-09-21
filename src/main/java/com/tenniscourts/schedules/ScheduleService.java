package com.tenniscourts.schedules;

import com.tenniscourts.tenniscourts.TennisCourtDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    private final ScheduleMapper scheduleMapper;

    public ScheduleDTO addSchedule(Long tennisCourtId, CreateScheduleRequestDTO createScheduleRequestDTO) {
        //TODO: implement addSchedule
        return null;
    }

    public List<ScheduleDTO> findSchedulesByDates(LocalDateTime startDate, LocalDateTime endDate) {
        //TODO: implement
        return null;
    }

    @Transactional
    public ScheduleDTO findSchedule(Long scheduleId) {
        //TODO: implement
        TennisCourtDTO tennisCourtDTO = new TennisCourtDTO();
        Optional<Schedule> sc =  scheduleRepository.findById(scheduleId);

        tennisCourtDTO.setId(sc.get().getTennisCourt().getId());
        tennisCourtDTO.setName(sc.get().getTennisCourt().getName());


        ScheduleDTO scheduleDTO= new ScheduleDTO();

        scheduleDTO.setEndDateTime(sc.get().getEndDateTime());
        scheduleDTO.setStartDateTime(sc.get().getStartDateTime());
        scheduleDTO.setId(sc.get().getId());
        scheduleDTO.setTennisCourtId(sc.get().getTennisCourt().getId());
        scheduleDTO.setTennisCourt(tennisCourtDTO);

        return scheduleDTO;
    }

    public List<ScheduleDTO> findSchedulesByTennisCourtId(Long tennisCourtId) {
        return scheduleMapper.map(scheduleRepository.findByTennisCourt_IdOrderByStartDateTime(tennisCourtId));
    }
}
