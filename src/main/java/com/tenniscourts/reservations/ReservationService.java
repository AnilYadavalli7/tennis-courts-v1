package com.tenniscourts.reservations;

import com.tenniscourts.exceptions.EntityNotFoundException;
import com.tenniscourts.guests.Guest;
import com.tenniscourts.schedules.Schedule;
import com.tenniscourts.schedules.ScheduleDTO;
import com.tenniscourts.schedules.ScheduleService;
import com.tenniscourts.tenniscourts.TennisCourt;
import com.tenniscourts.tenniscourts.TennisCourtDTO;
import com.tenniscourts.tenniscourts.TennisCourtService;
import jdk.jfr.TransitionTo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.print.attribute.standard.JobOriginatingUserName;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.tenniscourts.reservations.ReservationStatus.READY_TO_PLAY;

@Service
@AllArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ScheduleService scheduleService;
    private  final TennisCourtService tennisCourtService;

    private final ReservationMapper reservationMapper;

//    @Transactional
//    public Role createRole(String roleStr) {
//        validateRoleName(roleStr);
//
//        // check roleStr not in use
//        if (roleRepository.findByRole(roleStr).isPresent()) {
//            String errMsg = String.format("The role %s already exists", roleStr);
//            log.error(errMsg);
//            throw new RoleInUseException(errMsg);
//        }
//
//        Role role = new Role();
//        role.setRole(roleStr);
//
//        role = roleRepository.save(role);
//        log.info(String.format("Role %s %s has been created.", role.getId(), role.getRole()));
//
//        return role;
//    }

    @Transactional
    public ReservationDTO bookReservation(CreateReservationRequestDTO createReservationRequestDTO) {
           ScheduleDTO scheduleDTO = scheduleService.findSchedule(createReservationRequestDTO.getScheduleId());
           TennisCourtDTO tennisCourtDTO = scheduleDTO.getTennisCourt();

           TennisCourt tennisCourt = new TennisCourt();
           tennisCourt.setName(tennisCourtDTO.getName());
           tennisCourt.setId(tennisCourtDTO.getId());

           Guest guest = new Guest();
           guest.setId(createReservationRequestDTO.getGuestId());
           List<Reservation> rs = reservationRepository.findBySchedule_Id(scheduleDTO.getId());

           Schedule schedule = Schedule.builder()
                   .endDateTime(scheduleDTO.getEndDateTime())
                   .startDateTime(scheduleDTO.getStartDateTime())
                   .tennisCourt(tennisCourt)
                   .reservations(rs)
                   .build();



           Reservation reservation = Reservation.builder()
                   .guest(guest)
                   .reservationStatus(READY_TO_PLAY)
                   .schedule(schedule)
                   .refundValue(new BigDecimal(10))
                   .build();

          reservationRepository.save(reservation);

           ReservationDTO reservationDTO = ReservationDTO.builder()
                   .reservationStatus(reservation.getReservationStatus().toString())
                   .guestId(reservation.getGuest().getId())
                   .schedule(scheduleDTO)
                   .scheduledId(scheduleDTO.getId())
                   .build();

             return  reservationDTO;
       //  throw new UnsupportedOperationException();
    }

    @Transactional
    public ReservationDTO findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId).map(reservationMapper::map).orElseThrow(() -> {
            throw new EntityNotFoundException("Reservation not found.");
        });
    }

    public ReservationDTO cancelReservation(Long reservationId) {
        return reservationMapper.map(this.cancel(reservationId));
    }

    private Reservation cancel(Long reservationId) {
        return reservationRepository.findById(reservationId).map(reservation -> {

            this.validateCancellation(reservation);

            BigDecimal refundValue = getRefundValue(reservation);
            return this.updateReservation(reservation, refundValue, ReservationStatus.CANCELLED);

        }).orElseThrow(() -> {
            throw new EntityNotFoundException("Reservation not found.");
        });
    }

    private Reservation updateReservation(Reservation reservation, BigDecimal refundValue, ReservationStatus status) {
        reservation.setReservationStatus(status);
        reservation.setValue(reservation.getValue().subtract(refundValue));
        reservation.setRefundValue(refundValue);

        return reservationRepository.save(reservation);
    }

    private void validateCancellation(Reservation reservation) {
        if (!READY_TO_PLAY.equals(reservation.getReservationStatus())) {
            throw new IllegalArgumentException("Cannot cancel/reschedule because it's not in ready to play status.");
        }

        if (reservation.getSchedule().getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Can cancel/reschedule only future dates.");
        }
    }

    public BigDecimal getRefundValue(Reservation reservation) {
        long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), reservation.getSchedule().getStartDateTime());

        if (hours >= 24) {
            return reservation.getValue();
        }

        return BigDecimal.ZERO;
    }

    /*TODO: This method actually not fully working, find a way to fix the issue when it's throwing the error:
            "Cannot reschedule to the same slot.*/
    public ReservationDTO rescheduleReservation(Long previousReservationId, Long scheduleId) {
        Reservation previousReservation = cancel(previousReservationId);

        if (scheduleId.equals(previousReservation.getSchedule().getId())) {
            throw new IllegalArgumentException("Cannot reschedule to the same slot.");
        }

        previousReservation.setReservationStatus(ReservationStatus.RESCHEDULED);
        reservationRepository.save(previousReservation);

        ReservationDTO newReservation = bookReservation(CreateReservationRequestDTO.builder()
                .guestId(previousReservation.getGuest().getId())
                .scheduleId(scheduleId)
                .build());
        newReservation.setPreviousReservation(reservationMapper.map(previousReservation));
        return newReservation;
    }
}
