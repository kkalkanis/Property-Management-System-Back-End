package com.thesis.backend.repository;

import com.thesis.backend.models.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin("http://localhost:4200")
public interface RoomRepository extends JpaRepository<Room,Long> {
    public void deleteByRoomNumber(int roomNumber);
    public Room findByRoomNumber(int roomNumber);

    public List<Room> findAll();

    public Page<Room> findAll(Pageable pageable);

}
