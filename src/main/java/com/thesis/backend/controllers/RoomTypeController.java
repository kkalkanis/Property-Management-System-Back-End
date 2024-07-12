package com.thesis.backend.controllers;

import com.thesis.backend.models.*;
import com.thesis.backend.payload.request.RoomTypeRequest;
import com.thesis.backend.payload.response.MessageResponse;
import com.thesis.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class RoomTypeController {
    private List<HotelSpecifics> hotelSpecifics = new ArrayList<>();
    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private TypeAvailabilityRepository typeAvailRepo;

    @Autowired
    public ReservationTypeRepository reservationTypeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private HotelSpecificsRepository specrepository;

    @Autowired
    private TourOperatorRepository tourOperatorRepository;

    @PostMapping("/roomTypes")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    //If type is already saved in db we just want to return an "error" message
    public ResponseEntity<?> setRoomTypes(@Valid @RequestBody RoomTypeRequest roomTypeRequest) {
        if (roomTypeRepository.findByType(roomTypeRequest.getType()) != null) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Type already exists"));
            //If type don't exist in db, we save it and
        } else {
            RoomType roomType = new RoomType();
            roomType.setNumberOfPersons(roomTypeRequest.getNumberOfPersons());
            roomType.setDescription(roomTypeRequest.getDescription());
            roomType.setType((roomTypeRequest.getType()));
            roomTypeRepository.save(roomType);
            //////////////////////////////////////////////////////////////////
            //and we fill typeAvailability & virtualTypeAvailability tables with availability
            // from starting date = season start date (hotelSpecifics.getStartDate())
            hotelSpecifics=specrepository.findAll();
            TypeAvailability typeAvailability;

            LocalDate end = hotelSpecifics.get(0).getStartDate().plusYears(1);
            for (LocalDate date = hotelSpecifics.get(0).getStartDate(); date.isBefore(end); date = date.plusDays(1)) {
                typeAvailability = new TypeAvailability();
                typeAvailability.setDate(date);
                typeAvailability.setType(roomType);
                typeAvailability.setAvailable(0);
                typeAvailability.setVirtualAvailable(0);
                typeAvailRepo.save(typeAvailability);
            }
            return ResponseEntity.ok(new MessageResponse("Type registered successfully!"));
        }
    }
    //Get Mapping here just returns all available room types
    @GetMapping("/roomTypes")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ArrayList<RoomType> getRoomTypes() {
        return (ArrayList<RoomType>) roomTypeRepository.findAll();
    }

    //DeleteMapping deletes a type from database and also deletes data from
    // typeAvailability & virtualTypeAvailability tables
    @DeleteMapping("roomTypes/{type}")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void deleteRoomType(@PathVariable String type) {
        roomTypeRepository.deleteByType(type);
    }

    @PutMapping("roomTypes/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateRoomTypes(@Valid @RequestBody RoomTypeRequest roomTypeRequest) {
            RoomType roomType = new RoomType();
            roomType.setId(roomTypeRequest.getId());
            roomType.setNumberOfPersons(roomTypeRequest.getNumberOfPersons());
            roomType.setDescription(roomTypeRequest.getDescription());
            roomType.setType((roomTypeRequest.getType()));
            roomTypeRepository.save(roomType);
            return ResponseEntity.ok(new MessageResponse("Type updated successfully!"));
        }

    @GetMapping("/roomTypes/getReservationRoomTypes/{reservationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<RoomType> getReservationRoomTypes(@PathVariable Long reservationId)
    {
        List<RoomType> types = new ArrayList<>();
        //Get old Reservation Object
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        TourOperator tourOperator = new TourOperator();
        tourOperator = tourOperatorRepository.findByName(reservation.getTourOperator().getName());
        List<TypeAvailability> typeAvailabilities = new ArrayList<>();
        typeAvailabilities = typeAvailRepo.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperator(reservation.getCheckOutDate(),reservation.getCheckInDate(),tourOperator);

        try {
            for (int i = 0; i < typeAvailabilities.size(); i++) {
                    if (!types.contains(typeAvailabilities.get(i).getType().getType())) {
                        types.add(typeAvailabilities.get(i).getType());
                    }
            }
            Set<RoomType> singletonTypes = new HashSet<RoomType>(types);
            types.clear();
            types.addAll(singletonTypes);
            return types;
        }catch (Exception e){
            return types;
        }
    }

    @GetMapping("/roomTypes/pagination")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Page<RoomType> getPage(Pageable pageable)
    {
        Page<RoomType> page = roomTypeRepository.findAll(pageable);
        return page;
    }

    @GetMapping("/roomTypes/getSpecificType/{typeName}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public RoomType getRoomType (@PathVariable String typeName){
        RoomType roomType = roomTypeRepository.findByType(typeName);
        return roomType;
    }
}