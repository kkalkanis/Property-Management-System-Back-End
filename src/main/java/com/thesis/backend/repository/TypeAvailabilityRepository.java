package com.thesis.backend.repository;

import com.thesis.backend.models.RoomType;
import com.thesis.backend.models.TourOperator;
import com.thesis.backend.models.TypeAvailability;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin("http://localhost:4200")
public interface TypeAvailabilityRepository extends JpaRepository<TypeAvailability,Long> {

    List<TypeAvailability> findAllByType(RoomType roomType);

    Page<TypeAvailability> findAllByOrderByDateAsc(Pageable pageable);

    List<TypeAvailability> findAllByOrderByDateAsc();

    List<TypeAvailability> findAllByTourOperatorId(Long id);


    Page<TypeAvailability> findByTourOperatorIdOrderByDateAsc(Long id, Pageable pageable);

    List<TypeAvailability> findAllByTourOperator(TourOperator tourOperator);

    List<TypeAvailability> findAllByTourOperatorOrderByDateAsc(TourOperator tourOperator);

    List<TypeAvailability> findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperatorAndType(LocalDate minusDays, LocalDate checkInDate, TourOperator tourOperator, RoomType roomTypeId);

    List<TypeAvailability> findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperator(LocalDate checkOutDate, LocalDate checkInDate, TourOperator tourOperator);
}
