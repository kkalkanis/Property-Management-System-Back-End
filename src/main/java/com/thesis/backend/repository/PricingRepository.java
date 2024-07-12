package com.thesis.backend.repository;

import com.thesis.backend.models.Pricing;
import com.thesis.backend.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PricingRepository extends JpaRepository<Pricing,Long> {
    List<Pricing> findAllByDate(LocalDate currentLocalDate);

    Pricing findByDateAndRoom(LocalDate currentLocalDate, Room room);
}
