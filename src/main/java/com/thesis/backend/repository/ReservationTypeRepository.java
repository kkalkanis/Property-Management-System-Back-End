package com.thesis.backend.repository;

import com.thesis.backend.models.Reservation;
import com.thesis.backend.models.ReservationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ReservationTypeRepository extends JpaRepository<ReservationType,Long> {

    List<ReservationType> findAllByReservation(Reservation reservation);

    ReservationType findByReservation(Reservation reservation);
}
