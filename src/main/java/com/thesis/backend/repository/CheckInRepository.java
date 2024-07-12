package com.thesis.backend.repository;

import com.thesis.backend.models.CheckIn;
import com.thesis.backend.models.ReservationType;
import com.thesis.backend.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckInRepository extends JpaRepository<CheckIn,Long> {

    List<CheckIn> findAllByReservationType(ReservationType reservationType);

    CheckIn findByReservationType(ReservationType reservationType);

    List<CheckIn> findAllByRoomAndReservationType(Room room, ReservationType reservationType);

    List<CheckIn> findAllByRoom(Room room);
}
