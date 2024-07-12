package com.thesis.backend.repository;

import com.thesis.backend.models.Availability;
import com.thesis.backend.models.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability,Long> {
    Page<Availability> findAllByOrderByDateAsc(Pageable pageable);
    List<Availability> findAllByOrderByDateAsc();
    List<Availability> findAllByDateLessThanEqualAndDateGreaterThanEqualAndRoom(LocalDate checkIn, LocalDate checkOut, Room room);

}
