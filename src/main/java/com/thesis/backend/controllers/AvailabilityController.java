package com.thesis.backend.controllers;

import com.thesis.backend.models.Availability;
import com.thesis.backend.models.Room;
import com.thesis.backend.models.RoomType;
import com.thesis.backend.payload.request.AvailabilityRequest;
import com.thesis.backend.repository.AvailabilityRepository;
import com.thesis.backend.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class AvailabilityController {
    private AvailabilityRequest availRequest;

    @Autowired
    private AvailabilityRepository availRepository;

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping("/getAvailability")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public int getAvailability(@RequestParam int numberOfRooms){
        List<Availability> availability = new ArrayList<>();

        availability=availRepository.findAllByOrderByDateAsc();
        // create an LocalDate object
        LocalDate lt= LocalDate.now();
        lt=lt.plusMonths(2);
        int i;
        int pageNumber=0;
        for (i = 0; i < availability.size(); i++) {
            if(availability.get(i).getDate().equals(lt)){
                 break;
            }
        }
        pageNumber=i/(numberOfRooms*30);
        System.out.println(pageNumber+"Position");
        return pageNumber;
    }

    @GetMapping("/availability")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Page<AvailabilityRequest> getPage(Pageable pageable)
    {
        Page<Availability> page = availRepository.findAllByOrderByDateAsc(pageable);
        List<AvailabilityRequest> newAvailability  = new ArrayList<>();

        for (int i = 0; i < page.getContent().size(); i++) {
            availRequest = new AvailabilityRequest();
            availRequest.setId(page.getContent().get(i).getId());
            availRequest.setAvailable(page.getContent().get(i).getAvailable());
            availRequest.setDate(page.getContent().get(i).getDate());
            availRequest.setType(page.getContent().get(i).getType().getType());
            availRequest.setRoomNumber(page.getContent().get(i).getRoom().getRoomNumber());
            newAvailability.add(i,availRequest);
        }
        Page<AvailabilityRequest> availabilities = new PageImpl<>(newAvailability, pageable,page.getTotalElements());

        return availabilities;
    }

    @GetMapping("/getAvailabilities/{checkInDate}/{checkOutDate}/{availableType}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Room> getAvailabilities(@PathVariable String checkInDate , @PathVariable String checkOutDate, @PathVariable String availableType)
    {
        List<Availability> availabilities = availRepository.findAllByOrderByDateAsc();
        List<AvailabilityRequest> newAvailability  = new ArrayList<>();

        for (int i = 0; i < availabilities.size(); i++) {
            availRequest = new AvailabilityRequest();
            availRequest.setId(availabilities.get(i).getId());
            availRequest.setAvailable(availabilities.get(i).getAvailable());
            availRequest.setDate(availabilities.get(i).getDate());
            availRequest.setType(availabilities.get(i).getType().getType());
            availRequest.setRoomNumber(availabilities.get(i).getRoom().getRoomNumber());
            newAvailability.add(i,availRequest);
        }
        List<Room> tempAvailableRooms = new ArrayList<>();
        Room room;

        LocalDate checkIn = LocalDate.parse(checkInDate);
        LocalDate checkOut = LocalDate.parse(checkOutDate);
        List<Room> unwantedRooms = new ArrayList<>();
        for (int i = 0; i < newAvailability.size(); i++) {
            if(newAvailability.get(i).getDate().isEqual(checkOut)) {
                break;
            }
            if (newAvailability.get(i).getDate().isEqual(checkIn) || newAvailability.get(i).getDate().isAfter(checkIn) ) {
                if (newAvailability.get(i).getType().equals(availableType) && newAvailability.get(i).getAvailable() == 1) {
                        room = new Room();
                        room = roomRepository.findByRoomNumber(newAvailability.get(i).getRoomNumber());
                    tempAvailableRooms.add(room);
                }
                else if(newAvailability.get(i).getType().equals(availableType) && newAvailability.get(i).getAvailable() == 0){
                    room = new Room();
                    room = roomRepository.findByRoomNumber(newAvailability.get(i).getRoomNumber());
                    unwantedRooms.add(room);
                }
            }
        }
        //remove unwanted rooms
        for (int i = 0; i < unwantedRooms.size(); i++) {
            for (int j = 0; j < tempAvailableRooms.size() ; j++) {
                if(unwantedRooms.get(i).getRoomNumber()==(tempAvailableRooms.get(j).getRoomNumber())){
                    tempAvailableRooms.remove(tempAvailableRooms.get(j));}
            }
        }

        //populate available rooms
        List<Room> rooms = new ArrayList<>();
        rooms=roomRepository.findAll();
        List<Room> availableRooms = new ArrayList<>();
        for (int i = 0; i < tempAvailableRooms.size(); i++) {
            for (int j = 0; j < rooms.size(); j++) {
                if(rooms.get(j).getRoomNumber()==(tempAvailableRooms.get(i).getRoomNumber())){
                    availableRooms.add(rooms.get(j));
                }
            }
        }

        Set<Room> tempAvailableRooms2 = new HashSet<Room>(availableRooms); //remove duplicates converting list to Set
        List<Room> newAvailableRooms = new ArrayList<Room>(tempAvailableRooms2); // Convert it back to List
        return newAvailableRooms;

    }
}
