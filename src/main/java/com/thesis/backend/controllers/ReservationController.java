package com.thesis.backend.controllers;

import com.thesis.backend.models.*;
import com.thesis.backend.payload.request.ReservationRequest;
import com.thesis.backend.payload.request.ReservationTypeRequest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ReservationController {
    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TypeAvailabilityRepository typeAvailabilityRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private TourOperatorRepository tourOperatorRepository;

    @Autowired
    public ReservationTypeRepository reservationTypeRepository;

    @Autowired
    public ContractRepository contractRepository;

    @Autowired
    public ContractRoomTypeRepository contractRoomTypeRepository;

    @Autowired
    public CheckInRepository checkInRepository;

    @GetMapping("/getReservationByReservationId/{reservationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Reservation getSpecificReservation(@PathVariable Long reservationId) {
        Reservation reservation = new Reservation();
        reservation = reservationRepository.findById(reservationId).orElse(null);
        return reservation;
    }


    @GetMapping("/reservations")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Page<ReservationRequest> getPage(Pageable pageable) {
        Page<Reservation> page = reservationRepository.findAll(pageable);
        List<ReservationRequest> newReservations = new ArrayList<>();
        for (int i = 0; i < page.getContent().size(); i++) {
            ReservationRequest tempReservation = new ReservationRequest();
            tempReservation.setId(page.getContent().get(i).getId());
            tempReservation.setTourOperatorName(page.getContent().get(i).getTourOperator().getName());
            tempReservation.setCheckInDate(page.getContent().get(i).getCheckInDate());
            tempReservation.setCheckOutDate(page.getContent().get(i).getCheckOutDate());
            tempReservation.setReservationName(page.getContent().get(i).getReservationName());
            tempReservation.setContactPhone(page.getContent().get(i).getContactPhone());
            newReservations.add(tempReservation);
        }
        Page<ReservationRequest> reservations = new PageImpl<>(newReservations, pageable, page.getTotalElements());

        //////////////////////////////////////////////////////////////////////////////////////////
        //Delete empty Contracts(without types)
        List<Contract> contracts = new ArrayList<>();
        contracts = contractRepository.findAll();

        List<ContractRoomType> contractRoomTypes = new ArrayList<>();
        contractRoomTypes = contractRoomTypeRepository.findAll();
        int sumOfContractTypesPerContract = 0;
        for (int i = 0; i < contracts.size(); i++) {
            for (int j = 0; j < contractRoomTypes.size(); j++) {
                if (contracts.get(i).getId() == contractRoomTypes.get(j).getContractId().getId()) {
                    sumOfContractTypesPerContract += 1;
                }
            }
            if (sumOfContractTypesPerContract == 0) {
                System.out.println("Contract with id " + contracts.get(i).getId() + " deleted!");
                contractRepository.delete(contracts.get(i));
            }
            sumOfContractTypesPerContract = 0;
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////
        return reservations;
    }

    @PostMapping("/saveReservation")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> setReservation(@Valid @RequestBody ReservationRequest reservationRequest) {
        TourOperator tourOperator = new TourOperator();
        try{
            tourOperator = tourOperatorRepository.findByName(reservationRequest.getTourOperatorName());
            System.out.println(tourOperator.getName());

            List<Contract> contracts = new ArrayList<>();
            contracts = contractRepository.findAllByTourOperator(tourOperator);
            boolean typeAvailabilityExists = false;
            for (int i = 0; i < contracts.size(); i++) {
                if (contracts.get(i).getCheckInDate().isEqual(reservationRequest.getCheckInDate()) || reservationRequest.getCheckInDate().isAfter(contracts.get(i).getCheckInDate())) {
                    if (reservationRequest.getCheckOutDate().isEqual(contracts.get(i).getCheckOutDate()) || reservationRequest.getCheckOutDate().isBefore(contracts.get(i).getCheckOutDate())) {
                        typeAvailabilityExists = true;
                        break;
                    }
                }
            }
            System.out.println("TypeAvailabilityExists :" + typeAvailabilityExists);

            if (typeAvailabilityExists || reservationRequest.getTourOperatorName().equals("SELF")) {
                Reservation reservation = new Reservation();
                reservation.setReservationName(reservationRequest.getReservationName());
                reservation.setTourOperator(tourOperator);
                reservation.setCheckInDate(reservationRequest.getCheckInDate());
                reservation.setCheckOutDate(reservationRequest.getCheckOutDate());
                reservation.setContactPhone(reservationRequest.getContactPhone());
                reservation.setStatus("Pending");
                reservationRepository.save(reservation);}
            else{
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("There is no contract created for requested reservation," +
                                " so there is no Type Availability! Please update corresponding contract first, in (Tour Contracts -> New Tour Contract) section and you can update your reservation later!"));

            }

        }catch(Exception e){
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Tour Operator does not exist!"));
        }

        return ResponseEntity.ok(new MessageResponse("Reservation Created Successfully"));

    }

    @DeleteMapping("delReservation/{delResId}")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void deleteReservation(@PathVariable Long delResId) {
        List<Reservation> reservations = new ArrayList<>();
        reservations = reservationRepository.findAll();
        Reservation reservation = new Reservation();
        //Find associated reservation entity
        for (int i = 0; i < reservations.size(); i++) {
            if (reservations.get(i).getId() == delResId) {
                reservation = reservations.get(i);
                break;
            }
        }
        List<ReservationType> reservationTypes = new ArrayList<>();
        List<ReservationType> tempReservationTypes = new ArrayList<>();
        reservationTypes = reservationTypeRepository.findAll();

        //Delete reservation's reservation types
        for (int i = 0; i < reservationTypes.size(); i++) {
            if (reservationTypes.get(i).getReservation().getId() == reservation.getId()) {
                tempReservationTypes.add(reservationTypes.get(i));
                reservationTypeRepository.delete(reservationTypes.get(i));
            }
        }
        reservationRepository.delete(reservation); //Delete reservation after type deletion

        //After the deletion of whole reservation entity , we have to recalculate virtual availability again , in order not to lose it!
        //Update TypeAvailability table ,add deleted virtual availability again
        TourOperator tourOperator;
        tourOperator = tourOperatorRepository.findByName(reservation.getTourOperator().getName());
        List<TypeAvailability> typeAvailabilityList = new ArrayList<>();
        typeAvailabilityList = typeAvailabilityRepository.findAllByTourOperatorOrderByDateAsc(tourOperator);
        for (int i = 0; i < typeAvailabilityList.size(); i++) {
            if (typeAvailabilityList.get(i).getDate().isEqual(reservation.getCheckOutDate())) {
                break;
            }
            if (typeAvailabilityList.get(i).getDate().isEqual(reservation.getCheckInDate()) || typeAvailabilityList.get(i).getDate().isAfter(reservation.getCheckInDate())) {
                for (int j = 0; j < tempReservationTypes.size(); j++) {
                    if (tempReservationTypes.get(j).getRoomTypeId().getId() == typeAvailabilityList.get(i).getType().getId()) {
                        typeAvailabilityList.get(i).setVirtualAvailable(typeAvailabilityList.get(i).getVirtualAvailable() + tempReservationTypes.get(j).getNumberOfRooms());
                    }
                }
            }
        }

    }

    @PutMapping("/updateReservation")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateReservation(@Valid @RequestBody ReservationRequest reservationRequest) {
        //Get old Reservation Object
        Reservation oldReservation = reservationRepository.findById(reservationRequest.getId()).orElse(null);

        //Construct Requested Reservation Object
        TourOperator tourOperator = tourOperatorRepository.findByName(reservationRequest.getTourOperatorName());
        Reservation reservation = new Reservation();
        reservation.setId(reservationRequest.getId());
        reservation.setCheckInDate(reservationRequest.getCheckInDate());
        reservation.setCheckOutDate(reservationRequest.getCheckOutDate());
        reservation.setReservationName(reservationRequest.getReservationName());
        reservation.setTourOperator(tourOperator);
        reservation.setContactPhone(reservationRequest.getContactPhone());
        reservation.setStatus(oldReservation.getStatus());

        //Check If there is typeAvailability for requested reservation
        //If there is no contract created for tour operator then there is no TypeAvailability
        //Only SELF has availability without contract creation
        List<Contract> contracts = new ArrayList<>();
        contracts = contractRepository.findAllByTourOperator(reservation.getTourOperator());

        boolean typeAvailabilityExists = false;
        for (int i = 0; i < contracts.size(); i++) {
            if (contracts.get(i).getCheckInDate().isEqual(reservation.getCheckInDate()) || reservation.getCheckInDate().isAfter(contracts.get(i).getCheckInDate())) {
                if (reservation.getCheckOutDate().isEqual(contracts.get(i).getCheckOutDate()) || reservation.getCheckOutDate().isBefore(contracts.get(i).getCheckOutDate())) {
                    typeAvailabilityExists = true;
                    break;
                }
            }
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (typeAvailabilityExists || reservation.getTourOperator().getName().equals("SELF")) {
            //Retrieve Requested TypeAvailability
            List<TypeAvailability> typeAvailabilities = typeAvailabilityRepository.findAllByTourOperatorOrderByDateAsc(tourOperator);
            //Retrieve requested reservation's Reservation Types
            List<ReservationType> reservationTypes = reservationTypeRepository.findAllByReservation(reservation);

            //Eliminate double(find overall room number of each room type) and keep Room Types with their corresponding rooms
            List<RoomType> roomTypes = new ArrayList<>();
            RoomType roomType = new RoomType();
            for (int i = 0; i < reservationTypes.size(); i++) {
                roomType = roomTypeRepository.findByType(reservationTypes.get(i).getRoomTypeId().getType());
                roomTypes.add(roomType);
            }
            Set<RoomType> tempTypes = new HashSet<RoomType>(roomTypes); //remove duplicates converting list to Set
            List<RoomType> singletonRoomTypes = new ArrayList<RoomType>(tempTypes); // Convert it back to List
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            //Calculate sum of room number per room type
            List<Integer> sumOfRoomNumberPerType = new ArrayList<>();
            int sum=0;
            for (int i = 0; i < singletonRoomTypes.size(); i++) {
                for (int j = 0; j < reservationTypes.size(); j++) {
                    if(singletonRoomTypes.get(i).getType().equals(reservationTypes.get(j).getRoomTypeId().getType())){
                        sum+=reservationTypes.get(j).getNumberOfRooms();
                    }
                }
                sumOfRoomNumberPerType.add(sum);
                sum=0;
            }
            //Check for availability
            List<LocalDate> datesMustUnshackle= new ArrayList<>();
            List<LocalDate> dateCollisions = new ArrayList<>();
            List<Integer> virtualRoomCollisions = new ArrayList<>();
            List<String> roomTypeCollisions = new ArrayList<>();
            ////////////////////////////////////////////////////////////////
            //reservationDateExistsInOldReservation() method returns the dates that need to be checked
            //in order to see if there is availability
            List<LocalDate> datesToCheck = new ArrayList<>();
            datesToCheck = reservationDateExistsInOldReservation(reservation,oldReservation,datesMustUnshackle);


            //Check if there is availability for reservation
            for (int i = 0; i < typeAvailabilities.size(); i++) {
                if(typeAvailabilities.get(i).getDate().isEqual(reservation.getCheckOutDate())){
                    break;
                }
                if(datesToCheck.contains(typeAvailabilities.get(i).getDate())){
                    if(singletonRoomTypes.contains(typeAvailabilities.get(i).getType())){
                        if(typeAvailabilities.get(i).getVirtualAvailable()>0){
                            System.out.print("I have availability for date "+typeAvailabilities.get(i).getDate());
                            System.out.println(" and Type  "+typeAvailabilities.get(i).getType().getType());
                        }else{
                            dateCollisions.add(typeAvailabilities.get(i).getDate());
                            virtualRoomCollisions.add(typeAvailabilities.get(i).getVirtualAvailable());
                            roomTypeCollisions.add(typeAvailabilities.get(i).getType().getType());
                        }
                    }
                }
            }

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            try{ //If there are collisions
                System.out.print(dateCollisions.get(0)+" ");
                System.out.print(virtualRoomCollisions.get(0)+" ");
                System.out.println(roomTypeCollisions.get(0));
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("There is no availability for date "+dateCollisions.get(0)+" , Room Type:  "+roomTypeCollisions.get(0)
                        +" , Available Rooms:  "+virtualRoomCollisions.get(0)));
            }catch (Exception e) { //If no collisions found
                //Make the reservation
                if(datesToCheck.size()>0) {
                    for (int i = 0; i < typeAvailabilities.size(); i++) {
                        if (typeAvailabilities.get(i).getDate().isEqual(reservation.getCheckOutDate())) {
                            break;
                        }
                        for (int j = 0; j < singletonRoomTypes.size(); j++) {
                            if (datesToCheck.contains(typeAvailabilities.get(i).getDate())) {
                                if (singletonRoomTypes.get(j).getType().equals(typeAvailabilities.get(i).getType().getType())) {
                                    typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() - sumOfRoomNumberPerType.get(j));
                                    typeAvailabilityRepository.save(typeAvailabilities.get(i));
                                }
                            }
                        }
                    }
                }
                //Then Iterate through TypeAvailability and unshackle the reservation
                if(datesMustUnshackle.size()!=0){
                    for (int i = 0; i < typeAvailabilities.size(); i++) {
                        if (typeAvailabilities.get(i).getDate().isEqual(oldReservation.getCheckOutDate())) {
                            break;
                        }
                        for (int j = 0; j < singletonRoomTypes.size(); j++) {
                            if (datesMustUnshackle.contains(typeAvailabilities.get(i).getDate())) {
                                if (singletonRoomTypes.get(j).getType().equals(typeAvailabilities.get(i).getType().getType())) {
                                    typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() + sumOfRoomNumberPerType.get(j));
                                    typeAvailabilityRepository.save(typeAvailabilities.get(i));
                                }
                            }
                        }
                    }
                }
                reservationRepository.save(reservation);
            }
        }else {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("There is no contract created for requested reservation," +
                                " so there is no Type Availability! Please update corresponding contract first, in (Tour Contracts -> New Tour Contract) section and you can update your reservation later!"));
            }

        //and return OK message
        return ResponseEntity.ok(new MessageResponse("Reservation Type Created Successfully"));
    }

    private List<LocalDate> reservationDateExistsInOldReservation(Reservation reservation, Reservation oldReservation, List<LocalDate> datesMustUnshackle) {

        List<LocalDate> reservationDates = new ArrayList<>();
        List<LocalDate> oldReservationDates = new ArrayList<>();


        for (LocalDate date = reservation.getCheckInDate(); date.isBefore(reservation.getCheckOutDate()); date = date.plusDays(1)) {
            reservationDates.add(date);
            System.out.println("Reservation Date "+date);
        }
        for (LocalDate date = oldReservation.getCheckInDate(); date.isBefore(oldReservation.getCheckOutDate()); date = date.plusDays(1)) {
            oldReservationDates.add(date);
            System.out.println("Old Reservation Date "+date);
        }
        //Check for out of bounds date
        if((reservation.getCheckOutDate().isBefore(oldReservation.getCheckInDate()) || reservation.getCheckOutDate().isEqual(oldReservation.getCheckInDate()) || (reservation.getCheckInDate().isAfter(oldReservation.getCheckOutDate()) || reservation.getCheckInDate().isEqual(oldReservation.getCheckOutDate()))))
        {
            System.out.println("Out Of Bounds date");
            datesMustUnshackle.addAll(oldReservationDates);
            return reservationDates;
        }
        //Check for overlapping dates
        if((reservation.getCheckInDate().isEqual(oldReservation.getCheckInDate()) || reservation.getCheckInDate().isAfter(oldReservation.getCheckInDate())) && (reservation.getCheckOutDate().isBefore(oldReservation.getCheckOutDate()) || reservation.getCheckOutDate().isEqual(oldReservation.getCheckOutDate())))
        {   //Calculate dates must set back available
            Set<LocalDate> ad = new HashSet<LocalDate>(oldReservationDates);
            Set<LocalDate> tempNewRes = new HashSet<LocalDate>(reservationDates);
            datesMustUnshackle.addAll(ad);
            datesMustUnshackle.removeAll(tempNewRes);

            for (int i = 0; i < datesMustUnshackle.size(); i++) {
                System.out.println("Must unshackle date "+datesMustUnshackle.get(i));
            }
            System.out.println("Overlapping dates detected");
            return new ArrayList<>();
        }

        // Print the result
        Set<LocalDate> ad = new HashSet<LocalDate>(oldReservationDates);
        Set<LocalDate> tempOldRes = new HashSet<LocalDate>(oldReservationDates);
        Set<LocalDate> bd = new HashSet<LocalDate>(reservationDates);
        Set<LocalDate> tempNewRes = new HashSet<LocalDate>(reservationDates);
        List<LocalDate> datesToCheck = new ArrayList<>();

        bd.removeAll(tempOldRes);
        datesToCheck.addAll(bd);
        datesMustUnshackle.addAll(ad);
        datesMustUnshackle.removeAll(tempNewRes);
        for (int i = 0; i < datesMustUnshackle.size(); i++) {
            System.out.println("Must unshackle date "+datesMustUnshackle.get(i));
        }
        return datesToCheck;
    }

    ////////////////////////////////////Reservation Type APIS///////////////////////////////////////////////////////
    @PostMapping("/postReservationType")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> postRoomType(@Valid @RequestBody ReservationTypeRequest reservationTypeRequest) {
        List<TypeAvailability> typeAvailabilities = new ArrayList<>();
        Reservation reservation = new Reservation();
        List<Reservation> reservations = new ArrayList<>();
        reservations = reservationRepository.findAll();
        //Find Reservation object to assign to reservationType
        for (int i = 0; i < reservations.size(); i++) {
            if (reservations.get(i).getId() == reservationTypeRequest.getReservationId()) {
                reservation = reservations.get(i);
                break;
            }
        }
        //Check If there is typeAvailability for requested reservation
        //If there is no contract created for tour operator then there is no TypeAvailability
        //Only SELF has availability without contract creation
        List<Contract> contracts = new ArrayList<>();
        contracts = contractRepository.findAllByTourOperator(reservation.getTourOperator());

        boolean typeAvailabilityExists = false;
        for (int i = 0; i < contracts.size(); i++) {
            if (contracts.get(i).getCheckInDate().isEqual(reservation.getCheckInDate()) || reservation.getCheckInDate().isAfter(contracts.get(i).getCheckInDate())) {
                if (reservation.getCheckOutDate().isEqual(contracts.get(i).getCheckOutDate()) || reservation.getCheckOutDate().isBefore(contracts.get(i).getCheckOutDate())) {
                    typeAvailabilityExists = true;
                    break;
                }
            }
        }
        System.out.println("TypeAvailabilityExists :" + typeAvailabilityExists);
        if (typeAvailabilityExists || reservation.getTourOperator().getName().equals("SELF")) {

            //Retrieve typeAvailability for specific tour operator we want to check for availability
            //and make reservation if availability exists
            RoomType reqRoomType = new RoomType();
            reqRoomType = roomTypeRepository.findByType(reservationTypeRequest.getRoomType());
            typeAvailabilities = typeAvailabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperatorAndType(reservation.getCheckOutDate().minusDays(1),reservation.getCheckInDate(),reservation.getTourOperator(),reqRoomType);
            for (int i = 0; i < typeAvailabilities.size(); i++) {
                System.out.println("TypeAvailability date "+typeAvailabilities.get(i).getDate());
            }

            //Here we will keep info about availability
            List<RoomType> availableTypes = new ArrayList<>();
            List<String> tempAvailableTypes = new ArrayList<>();
            List<String> unwantedTypes = new ArrayList<>();
            List<Integer> roomsRemained = new ArrayList<>();

            List<String> collisions = new ArrayList<>();
            //Check for Availability
            for (int i = 0; i < typeAvailabilities.size(); i++) {
                if (typeAvailabilities.get(i).getVirtualAvailable() >= reservationTypeRequest.getNumberOfRooms()) {
                    tempAvailableTypes.add(typeAvailabilities.get(i).getType().getType());
                } else {
                    unwantedTypes.add(typeAvailabilities.get(i).getType().getType());
                    collisions.add(typeAvailabilities.get(i).getDate().toString());
                    roomsRemained.add(typeAvailabilities.get(i).getVirtualAvailable());
                }
            }
            System.out.println(unwantedTypes.size()+" Unwanted types");
            System.out.println(tempAvailableTypes.size()+" temp available types");


            //remove unwanted types
            for (int i = 0; i < unwantedTypes.size(); i++) {
                for (int j = 0; j < tempAvailableTypes.size(); j++) {
                    if (unwantedTypes.get(i) == (tempAvailableTypes.get(j))) {
                        tempAvailableTypes.remove(tempAvailableTypes.get(j));
                    }
                }
            }

            //populate available types
            List<RoomType> types = new ArrayList<>();
            types = roomTypeRepository.findAll();
            for (int i = 0; i < tempAvailableTypes.size(); i++) {
                for (int j = 0; j < types.size(); j++) {
                    if (types.get(j).getType().equals(tempAvailableTypes.get(i))) {
                        availableTypes.add(types.get(j));
                    }
                }
            }
            Set<RoomType> tempAvailableTypes2 = new HashSet<RoomType>(availableTypes); //remove duplicates converting list to Set
            List<RoomType> newAv = new ArrayList<RoomType>(tempAvailableTypes2); // Convert it back to List

            if (newAv.size() == 0) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("There is no availability, " + roomsRemained.get(0) + " rooms left! " + "Collision Found at " + collisions.get(0) + "!"));
            } else {
                //Retrieve room type object
                RoomType roomType = roomTypeRepository.findByType(reservationTypeRequest.getRoomType());

                //Set values to reservation Type object
                ReservationType reservationType = new ReservationType();
                reservationType.setReservation(reservation);
                reservationType.setRoomTypeId(roomType);
                reservationType.setNumberOfRooms(reservationTypeRequest.getNumberOfRooms());
                reservationType.setNumberOfAdults(reservationTypeRequest.getNumberOfAdults());
                reservationType.setNumberOfChildren(reservationTypeRequest.getNumberOfChildren());
                reservationType.setTerms(reservationTypeRequest.getTerms());
                reservationTypeRepository.save(reservationType);
                ////////////////////////////////////////////////////////////////////////////////////////
                //Now we have to decrease TypeAvailability
                // (virtual availability = virtual availability - reservationType.getNumberOfRooms )
                for (int i = 0; i < typeAvailabilities.size(); i++) {
                    if (typeAvailabilities.get(i).getVirtualAvailable() >= reservationTypeRequest.getNumberOfRooms()) {
                        typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() - reservationType.getNumberOfRooms());
                        typeAvailabilityRepository.save(typeAvailabilities.get(i));
                    }
                }
                //and return OK message
                return ResponseEntity.ok(new MessageResponse("Reservation Type Created Successfully"));
            }
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("There is no contract created for requested reservation," +
                            " so there is no Type Availability! Please create a contract first in (Tour Contracts -> New Tour Contract) section!"));
        }

    }
    @GetMapping("/getSpecificReservationType/{reservationTypeId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ReservationTypeRequest getSpecificReservationType(@PathVariable Long reservationTypeId) {
        ReservationType reservationType = reservationTypeRepository.findById(reservationTypeId).orElse(null);
        ReservationTypeRequest reservationTypeRequest = new ReservationTypeRequest();
        reservationTypeRequest.setId(reservationType.getId());
        reservationTypeRequest.setRoomType(reservationType.getRoomTypeId().getType());
        reservationTypeRequest.setTerms(reservationType.getTerms());
        reservationTypeRequest.setNumberOfChildren(reservationType.getNumberOfChildren());
        reservationTypeRequest.setNumberOfAdults(reservationType.getNumberOfAdults());
        reservationTypeRequest.setNumberOfRooms(reservationType.getNumberOfRooms());
        reservationTypeRequest.setReservationId(reservationType.getReservation().getId());
        return reservationTypeRequest;
    }


    @GetMapping("/getReservationTypes/{requestedReservationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<ReservationTypeRequest> getReservationTypes(@PathVariable Long requestedReservationId) {
        List<Reservation> reservations = reservationRepository.findAll();
        Reservation reservation = new Reservation();
        //Find the reservation you want to return its ReservationTypes
        for (int i = 0; i < reservations.size(); i++) {
            if (reservations.get(i).getId() == requestedReservationId) {
                reservation = reservations.get(i);
                break;
            }
        }

        //After we find it we have to return its types as well
        List<ReservationType> reservationTypes = new ArrayList<>();
        reservationTypes = reservationTypeRepository.findAll();

        List<ReservationTypeRequest> newReservationTypes = new ArrayList<>();

        for (int i = 0; i < reservationTypes.size(); i++) {
            if (reservationTypes.get(i).getReservation().getId() == reservation.getId()) {
                ReservationTypeRequest reservationTypeRequest = new ReservationTypeRequest();
                reservationTypeRequest.setId(reservationTypes.get(i).getId());
                reservationTypeRequest.setRoomType(reservationTypes.get(i).getRoomTypeId().getType());
                reservationTypeRequest.setNumberOfRooms(reservationTypes.get(i).getNumberOfRooms());
                reservationTypeRequest.setNumberOfAdults(reservationTypes.get(i).getNumberOfAdults());
                reservationTypeRequest.setTerms(reservationTypes.get(i).getTerms());
                newReservationTypes.add(reservationTypeRequest); // we hold types we want
            }
        }
        return newReservationTypes;

    }

    @DeleteMapping("/deleteReservationType/{reservationTypeId}")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void deleteReservationType(@PathVariable Long reservationTypeId) {
        ReservationType reservationType;
        reservationType = reservationTypeRepository.findById(reservationTypeId).orElse(null);

        // Get reservationType associated reservation object
        Reservation reservation = reservationRepository.findById(reservationType.getReservation().getId()).orElse(null);
        reservationTypeRepository.delete(reservationType);
        //Update TypeAvailability table ,add deleted virtual availability again
        TourOperator tourOperator;
        tourOperator = tourOperatorRepository.findByName(reservation.getTourOperator().getName());
        List<TypeAvailability> typeAvailabilityList = new ArrayList<>();
        typeAvailabilityList = typeAvailabilityRepository.findAllByTourOperatorOrderByDateAsc(tourOperator);
        for (int i = 0; i < typeAvailabilityList.size(); i++) {
            if (typeAvailabilityList.get(i).getDate().isEqual(reservation.getCheckOutDate())) {
                break;
            }
            if (typeAvailabilityList.get(i).getType() == reservationType.getRoomTypeId()) {
                if (typeAvailabilityList.get(i).getDate().isEqual(reservation.getCheckInDate()) || typeAvailabilityList.get(i).getDate().isAfter(reservation.getCheckInDate())) {
                    typeAvailabilityList.get(i).setVirtualAvailable(typeAvailabilityList.get(i).getVirtualAvailable() + reservationType.getNumberOfRooms());
                }
            }
        }
    }

    @PutMapping("/updateReservationType")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateReservationType(@Valid @RequestBody ReservationTypeRequest reservationTypeRequest) {
        Reservation reservation = reservationRepository.findById(reservationTypeRequest.getReservationId()).orElse(null);

        ReservationType oldReservationType = reservationTypeRepository.findById(reservationTypeRequest.getId()).orElse(null);

        List<TypeAvailability> typeAvailabilities = new ArrayList<>();
        typeAvailabilities = typeAvailabilityRepository.findAllByTourOperatorOrderByDateAsc(reservation.getTourOperator());

        //Iterate through TypeAvailability to check for availability
        //If user requested > availability than exists an appropriate message must be returned
        List<LocalDate> dateCollisions = new ArrayList<>();
        List<Integer> virtualRoomCollisions = new ArrayList<>();
        availabilityExists(reservation, typeAvailabilities, reservationTypeRequest, dateCollisions, virtualRoomCollisions,oldReservationType);
        try {
            if (dateCollisions.get(0) != null) { //means that collision exists!
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Collision detected at date " + dateCollisions.get(0) + " where there are another " +
                                virtualRoomCollisions.get(0) + " rooms available"));
            }
        } catch (Exception e) {
            List<TypeAvailability> oldReservationTypeAvailabilities = new ArrayList<>();
            boolean userChoseToUpdateRoomType = false;
            for (int i = 0; i < typeAvailabilities.size(); i++) {
                if (reservation.getCheckOutDate().isEqual(typeAvailabilities.get(i).getDate())) {
                    break;
                }
                if (typeAvailabilities.get(i).getDate().isEqual(reservation.getCheckInDate()) || typeAvailabilities.get(i).getDate().isAfter(reservation.getCheckInDate())) {
                    //If we found old room type we just keep it in case that user updated room type
                    if (typeAvailabilities.get(i).getType().getType().equals(oldReservationType.getRoomTypeId().getType())) {
                        oldReservationTypeAvailabilities.add(typeAvailabilities.get(i));
                    }
                    if (typeAvailabilities.get(i).getType().getType().equals(reservationTypeRequest.getRoomType())) {
                        System.out.println("Found same type" + typeAvailabilities.get(i).getType().getType());
                        //If user dont choose to update room type
                        if (oldReservationType.getRoomTypeId().getType().equals(reservationTypeRequest.getRoomType())) {
                            System.out.println("User keep same room type " + reservationTypeRequest.getRoomType());
                            //If user want to decrease number of rooms
                            if (reservationTypeRequest.getNumberOfRooms() < oldReservationType.getNumberOfRooms()) {
                                System.out.println("user want to decrease number of rooms");
                                typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() + (oldReservationType.getNumberOfRooms() - reservationTypeRequest.getNumberOfRooms()));
                            }
                            //If user want to reserve more rooms
                            else if (reservationTypeRequest.getNumberOfRooms() > oldReservationType.getNumberOfRooms()) {
                                System.out.println("user want to reserve more rooms");
                                System.out.println(typeAvailabilities.get(i).getVirtualAvailable() - (reservationTypeRequest.getNumberOfRooms() - oldReservationType.getNumberOfRooms()));
                                typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() - (reservationTypeRequest.getNumberOfRooms() - oldReservationType.getNumberOfRooms()));
                            }
                            //If chose reservation room number to remain the same
                            else if (reservationTypeRequest.getNumberOfRooms() == oldReservationType.getNumberOfRooms()) {
                                System.out.println("user want to keep same rooms");
                                break;
                            }
                        }
                        //If user want to update room type
                        else {
                            userChoseToUpdateRoomType = true;
                            System.out.println("User chose to update room type to " + reservationTypeRequest.getRoomType());
                            //If user want to decrease number of rooms
                            typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() - reservationTypeRequest.getNumberOfRooms());
                            for (int j = 0; j < oldReservationTypeAvailabilities.size(); j++) {
                                if (oldReservationTypeAvailabilities.get(j).getDate().isEqual(typeAvailabilities.get(i).getDate())) {
                                    typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() + oldReservationType.getNumberOfRooms());
                                }
                            }
                        }
                    }
                }
            }
        }
            //Set updated values to ReservationType Object
            //Retrieve RoomType Object
            RoomType roomType = new RoomType();
            roomType = roomTypeRepository.findByType(reservationTypeRequest.getRoomType());
            ReservationType reservationType = new ReservationType();
            reservationType.setReservation(reservation);
            reservationType.setId(reservationTypeRequest.getId());
            reservationType.setTerms(reservationTypeRequest.getTerms());
            reservationType.setNumberOfAdults(reservationTypeRequest.getNumberOfAdults());
            reservationType.setNumberOfRooms(reservationTypeRequest.getNumberOfRooms());
            reservationType.setNumberOfChildren(reservationTypeRequest.getNumberOfChildren());
            reservationType.setRoomTypeId(roomType);
            reservationTypeRepository.save(reservationType);

            //Retrieve all reservation types
            List<ReservationType> reservationTypes = new ArrayList<>();
            reservationTypes = reservationTypeRepository.findAllByReservation(reservation);

            List<CheckIn> allCheckIns = new ArrayList<>();
            allCheckIns = checkInRepository.findAll();

            List<CheckIn> reservationCheckIns = new ArrayList<>();
            if(allCheckIns.size()>0) {
                // find only reservations check ins
                for (int i = 0; i < allCheckIns.size(); i++) {
                    if (allCheckIns.get(i).getReservationType().getReservation() == reservation) {
                        reservationCheckIns.add(allCheckIns.get(i));
                    }
                }
            }
            //Check In table to see if there are check ins to make reservation checked in
            //Retrieve reservation's check ins
            if(reservationCheckIns.size()>0) {
                List<RoomType> allReservationRoomTypes = new ArrayList<>();
                //find all reservation room types even duplicates or even more
                for (int i = 0; i < reservationTypes.size(); i++) {
                    allReservationRoomTypes.add(reservationTypes.get(i).getRoomTypeId());
                }
                //and make them singleton
                List<RoomType> singletonRoomTypesOfReservation = new ArrayList<>(new HashSet<>(allReservationRoomTypes));
                List<Integer> sumPerType = new ArrayList<>();
                //init sum of rooms per type
                for (int i = 0; i < singletonRoomTypesOfReservation.size(); i++) {
                    sumPerType.add(0);
                }
                for (int i = 0; i < singletonRoomTypesOfReservation.size(); i++) {
                    for (int j = 0; j < reservationTypes.size(); j++) {
                        if (singletonRoomTypesOfReservation.get(i) == reservationTypes.get(j).getRoomTypeId()) {
                            sumPerType.set(i, sumPerType.get(i) + reservationTypes.get(j).getNumberOfRooms());
                        }
                    }
                }
                //Main code to check if reservation is fulfilled after reservation type modification
                int ok = 0;
                for (int i = 0; i < singletonRoomTypesOfReservation.size(); i++) {
                    int temp = 0;
                    for (int j = 0; j < reservationCheckIns.size(); j++) {
                        if (reservationCheckIns.get(j).getRoom().getRoomType().getType() == singletonRoomTypesOfReservation.get(i).getType()) {
                            temp = temp + 1;
                            if (temp == sumPerType.get(i) * singletonRoomTypesOfReservation.get(i).getNumberOfPersons()) {
                                ok = ok + 1;
                            }
                        }
                    }
                }

                if (ok == reservationTypes.size()) {
                    reservation.setStatus("Checked In");
                }
                else{
                    reservation.setStatus("Pending");
                }
            }
            //return OK message
            return ResponseEntity.ok(new MessageResponse("Reservation Type Updated Successfully"));
    }

    private void availabilityExists(Reservation reservation, List<TypeAvailability> typeAvailabilities, ReservationTypeRequest reservationTypeRequest, List<LocalDate> dateCollisions, List<Integer> virtualRoomCollisions, ReservationType oldReservationType) {
        //Iterate through TypeAvailability to check for availability
        //If user requested > availability than exists an appropriate message must be returned
        for (int i = 0; i < typeAvailabilities.size(); i++) {
            if (reservation.getCheckOutDate().isEqual(typeAvailabilities.get(i).getDate())) {
                break;
            }
            if (typeAvailabilities.get(i).getDate().isEqual(reservation.getCheckInDate()) || typeAvailabilities.get(i).getDate().isAfter(reservation.getCheckInDate())) {
                if (typeAvailabilities.get(i).getType().getType().equals(reservationTypeRequest.getRoomType())) {
                    if (oldReservationType.getNumberOfRooms()+typeAvailabilities.get(i).getVirtualAvailable() >= reservationTypeRequest.getNumberOfRooms()) {
                    } else {
                        dateCollisions.add(typeAvailabilities.get(i).getDate());
                        virtualRoomCollisions.add(typeAvailabilities.get(i).getVirtualAvailable());
                    }
                }
            }
        }
    }
}

