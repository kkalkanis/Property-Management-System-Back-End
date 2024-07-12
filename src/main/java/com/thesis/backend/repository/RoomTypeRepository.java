package com.thesis.backend.repository;

import com.thesis.backend.models.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;

@CrossOrigin("http://localhost:4200")
public interface RoomTypeRepository extends JpaRepository<RoomType,Long> {
    public ArrayList<RoomType> findAll();
    public void deleteByType(String type);
    public RoomType findByType(String type);
}
