package com.thesis.backend.repository;

import com.thesis.backend.models.HotelSpecifics;
import org.springframework.data.jpa.repository.JpaRepository;

//@CrossOrigin(origins = "http://localhost:4200")
//@RepositoryRestResource(path="hotelSpecifics")
public interface HotelSpecificsRepository extends JpaRepository<HotelSpecifics,Long> {

}
