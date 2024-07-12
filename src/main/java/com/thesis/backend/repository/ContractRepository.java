package com.thesis.backend.repository;

import com.thesis.backend.models.Contract;
import com.thesis.backend.models.TourOperator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract,Long> {
    List<Contract> findAllByTourOperator(TourOperator tourOperator);

    Contract findByTourOperator(TourOperator tourOperator);
}
