package com.thesis.backend.controllers;

import com.thesis.backend.models.*;
import com.thesis.backend.payload.response.MessageResponse;
import com.thesis.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class HotelSpecificsController {
    @Autowired
    public HotelSpecificsRepository specRepository;

    @Autowired
    public RoomRepository roomRepository;

    @Autowired
    public RoomTypeRepository roomTypeRepository;

    @Autowired
    public AvailabilityRepository availabilityRepository;

    @Autowired
    public TypeAvailabilityRepository typeAvailabilityRepository;

    @PostMapping("/setHotelSpecifics")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> setHotelSpecifics(@Valid @RequestBody HotelSpecifics hotelSpecifics) {

        try{
            List<HotelSpecifics> hotelSpecs = new ArrayList<>();
            hotelSpecs=specRepository.findAll();
            if(hotelSpecs.get(0).getId()!=null){ //If true , means that HotelSpecifics row already exist
                                                //and that we want to update HotelSpecifics
                //If getStartDates are different then we want to update also other tables such as
                //1) Availability
                //2)TypeAvailability
                if(hotelSpecs.get(0).getStartDate()!=hotelSpecifics.getStartDate()){
                    //populate the tables we need to update
                    List<Availability> availabilities = new ArrayList<>();
                    availabilities=availabilityRepository.findAll();

                    List<TypeAvailability> typeAvailabilities = new ArrayList<>();
                    typeAvailabilities = typeAvailabilityRepository.findAll();


                    int j = 0;
                    LocalDate end = hotelSpecifics.getStartDate().plusYears(1); //find end year

                    List<Room> rooms = new ArrayList<>();
                    rooms = roomRepository.findAll();
                    try{
                        if(availabilities.get(0)!=null) { //if availabilities & virtualAvailabilities both exist
                            int roomsCount = rooms.size(); //we retrieve roomNumber to populate again tables with new startDate
                            for (int i = 0; i < roomsCount; i++) {
                                for (LocalDate date = hotelSpecifics.getStartDate(); date.isBefore(end); date = date.plusDays(1)) {
                                    availabilities.get(j).setDate(date);
                                    availabilities.get(j).setAvailable(1);
                                    availabilities.get(j).setVirtualAvailable(1);
                                    availabilityRepository.save(availabilities.get(j));
                                    j++;
                                }
                            }
                        }
                    }catch(Exception e){
                        System.out.println("Cant update availability tables cause rooms are not exist");
                    }
                    try {
                        //if typeAvailabilities exists
                        j = 0;
                        if (typeAvailabilities.get(0) != null) {
                            List<RoomType> types = new ArrayList<>();
                            types = roomTypeRepository.findAll();
                            int typeCount = types.size();
                            for (int i = 0; i < typeCount; i++) {
                                for (LocalDate date = hotelSpecifics.getStartDate(); date.isBefore(end); date = date.plusDays(1)) {
                                    typeAvailabilities.get(j).setAvailable(0);
                                    typeAvailabilities.get(j).setVirtualAvailable(0);
                                    for (int k = 0; k < rooms.size(); k++) {
                                        if(typeAvailabilities.get(j).getType()==rooms.get(k).getRoomType()){
                                            typeAvailabilities.get(j).setAvailable(typeAvailabilities.get(j).getAvailable()+1);
                                            typeAvailabilities.get(j).setVirtualAvailable(typeAvailabilities.get(j).getAvailable());
                                        }
                                    }
                                    typeAvailabilities.get(j).setDate(date);
                                    typeAvailabilityRepository.save(typeAvailabilities.get(j));
                                    j++;
                                }
                            }
                        }
                    }catch(Exception e){
                            System.out.println("Cant update typeAvailability tables cause types dont exist");
                        }
                    }
                }
        }catch(Exception e){
            System.out.println("General catch exception: HotelSpecifics record don't exist!");
        }

        specRepository.save(hotelSpecifics);
        return ResponseEntity.ok(new MessageResponse("Specifics added successfully!"));
    }

    // Get All specifics API
    @GetMapping("/getHotelSpecifics")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ArrayList<HotelSpecifics> getHotelSpecifics() {
        ArrayList<HotelSpecifics> hotelSpecifics = (ArrayList<HotelSpecifics>) specRepository.findAll();
        return hotelSpecifics;
    }
}

