package com.thesis.backend.repository;

import com.thesis.backend.models.TourOperator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourOperatorRepository extends JpaRepository<TourOperator,Long> {
    TourOperator findByName(String tourOperator);
}
