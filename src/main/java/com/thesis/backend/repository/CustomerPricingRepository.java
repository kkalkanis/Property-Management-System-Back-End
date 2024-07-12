package com.thesis.backend.repository;

import com.thesis.backend.models.CustomerPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CustomerPricingRepository extends JpaRepository<CustomerPricing,Long> {
    List<CustomerPricing> findAllByRoomNumberAndDate(int roomNumber, LocalDate currentLocalDate);
}
