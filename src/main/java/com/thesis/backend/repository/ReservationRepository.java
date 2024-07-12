package com.thesis.backend.repository;

import com.thesis.backend.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;


public interface ReservationRepository extends JpaRepository<Reservation,Long> {

    List<Reservation> findByCheckInDate(LocalDate date);

    List<Reservation> findByCheckOutDate(LocalDate parse);
}
