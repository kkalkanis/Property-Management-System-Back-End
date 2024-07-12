package com.thesis.backend.controllers;

import com.thesis.backend.models.*;
import com.thesis.backend.payload.request.RoomRequest;
import com.thesis.backend.payload.response.MessageResponse;
import com.thesis.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class RoomController {
    private List<HotelSpecifics> hotelSpecifics = new ArrayList<>();
    private List<TypeAvailability> typeAvailability = new ArrayList<>();

    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private TypeAvailabilityRepository typeAvailRepo;

    @Autowired
    private HotelSpecificsRepository specrepository;

    @Autowired
    private TourOperatorRepository tourOperatorRepository;


    // Save room API
    @PostMapping("/setRoom")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> setRoom(@Valid @RequestBody RoomRequest room){
        if(roomRepository.findByRoomNumber(room.getRoomNumber())!=null){

            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Room with same room number already exists!"));
        }
        else{
            RoomType roomType = roomTypeRepository.findByType(room.getType()); //Get the parent Object
            Room newRoom  = new Room(); //Create a new Many object
            newRoom.setRoomType(roomType);
            newRoom.setRoomNumber(room.getRoomNumber());
            newRoom.setDescription(room.getDescription());
            newRoom.setStatus(room.getStatus());

            roomRepository.save(newRoom);

            System.out.println("Room added successfully!");

            //Create SELF Operator (Hotel's Customer if not exists)
            TourOperator tourOperator = new TourOperator();
            tourOperator = tourOperatorRepository.findByName("SELF");
            try{
                if(tourOperator.getId()!= null){ // if exists
                    System.out.println("tour operator exists");
                    ;
                }
            }catch (Exception e){
                tourOperator = new TourOperator();
                tourOperator.setName("SELF");
                tourOperatorRepository.save(tourOperator);
            }

            typeAvailability=typeAvailRepo.findAllByType(roomType);

            for (int i = 0; i < typeAvailability.size(); i++) {
                typeAvailability.get(i).setAvailable(typeAvailability.get(i).getAvailable()+1);
                typeAvailability.get(i).setTourOperator(tourOperator);
                typeAvailability.get(i).setVirtualAvailable(typeAvailability.get(i).getVirtualAvailable()+1);
            }

            //Retrieve date from hotel specifics
            hotelSpecifics=specrepository.findAll();
            Availability availability;
            LocalDate end = hotelSpecifics.get(0).getStartDate().plusYears(1);
            for (LocalDate date = hotelSpecifics.get(0).getStartDate(); date.isBefore(end); date = date.plusDays(1)) {
                availability = new Availability();

                availability.setDate(date);


                availability.setRoom(newRoom);


                availability.setType(newRoom.getRoomType());

                availability.setAvailable(1);

                availability.setVirtualAvailable(1);

                availabilityRepository.save(availability);
            }
            return ResponseEntity.ok(new MessageResponse("Room added successfully!"));
        }

    }

    // Get All rooms API
    @GetMapping("/rooms")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ArrayList <RoomRequest> getRooms(){
         ArrayList<Room> rooms = (ArrayList<Room>) roomRepository.findAll();
        ArrayList <RoomRequest> newRooms  = new ArrayList<>();


        for (int i = 0; i < rooms.size(); i++) {
            RoomRequest tempRoom = new RoomRequest();
            tempRoom.setId(rooms.get(i).getId());
            tempRoom.setStatus(rooms.get(i).getStatus());
            tempRoom.setRoomNumber(rooms.get(i).getRoomNumber());
            tempRoom.setDescription(rooms.get(i).getDescription());
            tempRoom.setType(rooms.get(i).getRoomType().getType());

            newRooms.add(tempRoom);
        }

        return newRooms;

    }
    // Delete Room API
    @DeleteMapping("rooms/{roomNumber}")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void deleteRoom(@PathVariable int roomNumber){
        Room room = new Room();
        room=roomRepository.findByRoomNumber(roomNumber);
        typeAvailability=typeAvailRepo.findAllByType(room.getRoomType());

        for (int i = 0; i < typeAvailability.size(); i++) {
            typeAvailability.get(i).setAvailable(typeAvailability.get(i).getAvailable()-1);
            typeAvailability.get(i).setVirtualAvailable(typeAvailability.get(i).getVirtualAvailable()-1);
        }
        typeAvailRepo.saveAll(typeAvailability);
        roomRepository.deleteByRoomNumber(roomNumber);
    }
    // Update Room API
    @PutMapping("rooms")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void updateRoom(@Valid @RequestBody RoomRequest room){
        Room newRoom = new Room();
        RoomType roomType = roomTypeRepository.findByType(room.getType());
        newRoom.setRoomType(roomType);
        newRoom.setId(room.getId());
        newRoom.setRoomNumber(room.getRoomNumber());
        newRoom.setDescription(room.getDescription());
        newRoom.setStatus(room.getStatus());
        roomRepository.save(newRoom);

    }
    @GetMapping("/rooms/pagination")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Page<RoomRequest> getPageOne(Pageable pageable)
    {

        // First page with 5 items
        //Pageable paging = PageRequest.of(
           //     1, 5, Sort.by("roomNumber").ascending());
        Page<Room> page = roomRepository.findAll(pageable);
        List<RoomRequest> newRooms  = new ArrayList<>();

        for (int i = 0; i < page.getContent().size(); i++) {
            RoomRequest tempRoom = new RoomRequest();
            tempRoom.setId(page.getContent().get(i).getId());
            tempRoom.setRoomNumber(page.getContent().get(i).getRoomNumber());
            tempRoom.setDescription(page.getContent().get(i).getDescription());
            tempRoom.setStatus(page.getContent().get(i).getStatus());
            tempRoom.setType(page.getContent().get(i).getRoomType().getType());

            newRooms.add(tempRoom);
        }
        Page<RoomRequest> rooms = new PageImpl<>(newRooms, pageable,page.getTotalElements());

        return rooms;
    }
    // Get All rooms by type API
    @GetMapping("/roomsByType/{type}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Room> getRoomByType(@PathVariable String type){
        Room room ;
        ArrayList<Room> rooms = (ArrayList<Room>) roomRepository.findAll();
        List<Room> newRooms = new ArrayList<>();
        for (int i = 0; i <rooms.size() ; i++) {
            if (rooms.get(i).getRoomType().getType().equals(type)) {
                room = new Room();
                room.setRoomType(rooms.get(i).getRoomType());
                room.setRoomNumber(rooms.get(i).getRoomNumber());
                room.setId(rooms.get(i).getId());
                room.setStatus(rooms.get(i).getStatus());
                room.setDescription(rooms.get(i).getDescription());
                newRooms.add(room);
            }
        }
            return newRooms;
        }
}