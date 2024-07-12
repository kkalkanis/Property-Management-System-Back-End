package com.thesis.backend.controllers;

import com.thesis.backend.models.*;
import com.thesis.backend.payload.request.CheckInOutRequest;
import com.thesis.backend.payload.request.CheckInSingleCustomerRequest;
import com.thesis.backend.payload.request.ReservationRequest;
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
public class CheckInOutController {
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

    @Autowired
    public PricingRepository pricingRepository;

    @Autowired
    public CustomerPricingRepository customerPricingRepository;

    @GetMapping("/getAvailabilitiesByRoomAndDate/{date}/{roomNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Customer> getAvailabilitiesByRoomAndDate(@PathVariable String date, @PathVariable int roomNumber) {
        Room room = roomRepository.findByRoomNumber(roomNumber);
        List<Reservation> reservations = new ArrayList<>();
        List<Reservation> newReservations = new ArrayList<>();
        reservations = reservationRepository.findAll();
        for (int i = 0; i < reservations.size(); i++) {
            if(reservations.get(i).getCheckInDate().isEqual(LocalDate.parse(date)) || reservations.get(i).getCheckInDate().isBefore(LocalDate.parse(date)) && reservations.get(i).getCheckOutDate().isAfter(LocalDate.parse(date))  ){
                newReservations.add(reservations.get(i));
            }
        }
        List<ReservationType> reservationTypes = new ArrayList<>();
        List<ReservationType> newReservationTypes = new ArrayList<>();
            for (int i = 0; i < newReservations.size(); i++) {
                reservationTypes = reservationTypeRepository.findAllByReservation(newReservations.get(i));
                for (int j = 0; j < reservationTypes.size(); j++) {
                    if (reservationTypes.get(j).getRoomTypeId()==room.getRoomType()) {
                        newReservationTypes.add(reservationTypes.get(j));
                    }
                }
            }
        List<CheckIn> checkIns = new ArrayList<>();
        List<CheckIn> newCheckIns = new ArrayList<>();
        for (int i = 0; i < newReservationTypes.size(); i++) {
            checkIns = checkInRepository.findAllByReservationType(newReservationTypes.get(i));
            for (int j = 0; j < checkIns.size(); j++) {
                if (checkIns.get(j).getRoom().getRoomNumber() == room.getRoomNumber()) {
                    newCheckIns.add(checkIns.get(j));
                }
            }
        }
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < newCheckIns.size(); i++) {
            customers.add(newCheckIns.get(i).getCustomer());
        }
        return customers;
    }

    @GetMapping("/getArrivals/{date}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<ReservationRequest> getArrivals(@PathVariable String date) {
        List<Reservation> arrivals = new ArrayList<>();
        arrivals = reservationRepository.findByCheckInDate(LocalDate.parse(date));

        List<ReservationRequest> reservationRequests = new ArrayList<>();
        ReservationRequest reservationRequest;
        for (int i = 0; i < arrivals.size(); i++) {
            reservationRequest = new ReservationRequest();
            reservationRequest.setReservationName(arrivals.get(i).getReservationName());
            reservationRequest.setCheckInDate(arrivals.get(i).getCheckInDate());
            reservationRequest.setCheckOutDate(arrivals.get(i).getCheckOutDate());
            reservationRequest.setId(arrivals.get(i).getId());
            reservationRequest.setStatus(arrivals.get(i).getStatus());
            reservationRequest.setTourOperatorName(arrivals.get(i).getTourOperator().getName());
            reservationRequests.add(reservationRequest);
        }
        return reservationRequests;
    }

    @PostMapping("/postCheckIn")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void postCheckIn(@Valid @RequestBody CheckInOutRequest checkInOutRequest) {
        ReservationType reservationType = reservationTypeRepository.findById(checkInOutRequest.getReservationTypeId()).orElse(null);
        Reservation reservation = reservationRepository.findById(reservationType.getReservation().getId()).orElse(null);
        Room room = roomRepository.findByRoomNumber(checkInOutRequest.getRoomNumber().intValue());
        Customer customer;
        CheckIn checkInObject;

        customer = customerRepository.findByPassport(checkInOutRequest.getCustomers().get(0).getPassport());
        try{
            if(customer.getCustomerId()!=null){
                checkInObject = new CheckIn();
                checkInObject.setCustomer(customer);
                checkInObject.setRoom(room);
                checkInObject.setReservationType(reservationType);
                checkInObject.setReservation(reservation);
                checkInRepository.save(checkInObject);
            }
        }catch(Exception e){
            Customer customer1 = new Customer();
            customer1.setFirstName(checkInOutRequest.getCustomers().get(0).getFirstName());
            customer1.setLastName(checkInOutRequest.getCustomers().get(0).getLastName());
            customer1.setAddress(checkInOutRequest.getCustomers().get(0).getAddress());
            customer1.setCity(checkInOutRequest.getCustomers().get(0).getCity());
            customer1.setContactPhone(checkInOutRequest.getCustomers().get(0).getContactPhone());
            customer1.setCountry(checkInOutRequest.getCustomers().get(0).getCountry());
            customer1.setEmail(checkInOutRequest.getCustomers().get(0).getEmail());
            customer1.setPassport(checkInOutRequest.getCustomers().get(0).getPassport());
            customer1.setZipCode(checkInOutRequest.getCustomers().get(0).getZipCode());
            customerRepository.save(customer1);
            checkInObject = new CheckIn();
            checkInObject.setCustomer(customer1);
            checkInObject.setRoom(room);
            checkInObject.setReservationType(reservationType);
            checkInObject.setReservation(reservation);
            checkInRepository.save(checkInObject);
        }

        List<CheckIn> requestedRoomCheckIn = new ArrayList<>();
        List<Customer> checkedInCustomers = new ArrayList<>();
        requestedRoomCheckIn = checkInRepository.findAllByRoomAndReservationType(room,reservationType);
        for (int i = 0; i < requestedRoomCheckIn.size(); i++) {
            checkedInCustomers.add(requestedRoomCheckIn.get(i).getCustomer());
        }


        List<Availability> availabilities = new ArrayList<>();
        List<TypeAvailability> typeAvailabilities = new ArrayList<>();

        availabilities = availabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndRoom(reservation.getCheckOutDate().minusDays(1), reservation.getCheckInDate(), room);
        typeAvailabilities = typeAvailabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperatorAndType(reservation.getCheckOutDate().minusDays(1), reservation.getCheckInDate(), reservation.getTourOperator(), reservationType.getRoomTypeId());
        if (checkedInCustomers.size()==room.getRoomType().getNumberOfPersons()) {
            for (int i = 0; i < availabilities.size(); i++) {
                availabilities.get(i).setAvailable(0);
                availabilityRepository.save(availabilities.get(i));
            }
            for (int i = 0; i < typeAvailabilities.size(); i++) {
                typeAvailabilities.get(i).setAvailable(typeAvailabilities.get(i).getAvailable() - 1);
                typeAvailabilityRepository.save(typeAvailabilities.get(i));
            }
            //Check if reservation is checked in to change status from pending to checked in
            List<ReservationType> reservationTypes = new ArrayList<>();
            reservationTypes = reservationTypeRepository.findAllByReservation(reservation);

            List<CheckIn> checkIns = new ArrayList<>();
            List<CheckIn> newCheckIns = new ArrayList<>();
            for (int i = 0; i < reservationTypes.size(); i++) {
                checkIns = checkInRepository.findAllByReservationType(reservationTypes.get(i));
                for (int j = 0; j < checkIns.size(); j++) {
                    newCheckIns.add(checkIns.get(j));
                }
            }

            int cnt = 0, flag = 0;
            for (int i = 0; i < reservationTypes.size(); i++) {
                for (int j = 0; j < newCheckIns.size(); j++) {
                    if (reservationTypes.get(i).getId() == newCheckIns.get(j).getReservationType().getId()) {
                        cnt++;
                        if (cnt == reservationTypes.get(i).getNumberOfRooms() * reservationTypes.get(i).getNumberOfAdults()) {
                            cnt = 0;
                            flag++;
                            if (flag == reservationTypes.size()) {
                                reservation.setStatus("Checked In");
                                reservationRepository.save(reservation);
                            }
                        }
                    }
                }
            }
        }
    }
    @GetMapping("/isReservationTypesFulfilled/{reservationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<ReservationType> isReservationTypesFulfilled(@PathVariable Long reservationId){
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        List<ReservationType> reservationTypes = new ArrayList<>();
        reservationTypes = reservationTypeRepository.findAllByReservation(reservation);

        List<CheckIn> checkIns;
        List<ReservationType> completedReservationTypes = new ArrayList<>();
        for (int i = 0; i < reservationTypes.size(); i++) {
            checkIns = new ArrayList<>();
            checkIns = checkInRepository.findAllByReservationType(reservationTypes.get(i));
            if(checkIns.size() == reservationTypes.get(i).getNumberOfRooms()*reservationTypes.get(i).getNumberOfAdults()){
                completedReservationTypes.add(reservationTypes.get(i));
            }
        }
        return completedReservationTypes;
    }

    @GetMapping("/getReservationTypeCheckIns/{reservationTypeId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<CheckInSingleCustomerRequest> getReservationTypeCheckIns(@PathVariable Long reservationTypeId){
        ReservationType reservationType = reservationTypeRepository.findById(reservationTypeId).orElse(null);
        List<CheckIn> checkIns = new ArrayList<>();
        checkIns = checkInRepository.findAllByReservationType(reservationType);

        List<CheckInSingleCustomerRequest> checkInSingleCustomerRequests = new ArrayList<>();
        CheckInSingleCustomerRequest checkInSingleCustomerRequest;
        Customer customer;
        for (int i = 0; i < checkIns.size(); i++) {
            checkInSingleCustomerRequest = new CheckInSingleCustomerRequest();
            checkInSingleCustomerRequest.setId(checkIns.get(i).getId());
            checkInSingleCustomerRequest.setRoomNumber((long) checkIns.get(i).getRoom().getRoomNumber());
            customer = new Customer();
            customer = checkIns.get(i).getCustomer();
            checkInSingleCustomerRequest.setCustomer(customer);
            checkInSingleCustomerRequests.add(checkInSingleCustomerRequest);
        }
        return checkInSingleCustomerRequests;
    }

    @PostMapping("/postDayCharge")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void postDayCharge(@Valid @RequestBody String currentDate) {
        LocalDate currentLocalDate = LocalDate.parse(currentDate);
        List<Reservation> reservations = new ArrayList<>();
        List<Reservation> checkedInReservations = new ArrayList<>();
        List<Contract> contractList = new ArrayList<>();
        List<Contract> requestedContracts = new ArrayList<>();
        reservations = reservationRepository.findAll();
        contractList = contractRepository.findAll();

        //Retrieve checked in reservations
        for (int i = 0; i < reservations.size(); i++) {
            if (reservations.get(i).getCheckInDate().isEqual(currentLocalDate) || currentLocalDate.isAfter(reservations.get(i).getCheckInDate()) && currentLocalDate.isBefore(reservations.get(i).getCheckOutDate())) {
                if (reservations.get(i).getStatus().equals("Checked In")) {
                    checkedInReservations.add(reservations.get(i));
                }
            }
        }

        //Retrieve contracts
        for (int i = 0; i < contractList.size(); i++) {
            if (contractList.get(i).getCheckInDate().isEqual(currentLocalDate) || currentLocalDate.isAfter(contractList.get(i).getCheckInDate()) && currentLocalDate.isBefore(contractList.get(i).getCheckOutDate())) {
                requestedContracts.add(contractList.get(i));
            }
        }

        List<Pricing> pricings = new ArrayList<>();
        Pricing pricing;
        ReservationType reservationType;
        List<CheckIn> newCheckIns = new ArrayList<>();
        List<ReservationType> reservationTypes = new ArrayList<>();

        List<Pricing> currentDatePricings = new ArrayList<>();
        currentDatePricings = pricingRepository.findAllByDate(currentLocalDate);
        List<Integer> roomNumbers = new ArrayList<>();

        //Keep daily room numbers that exists on table pricing
        if (currentDatePricings.size() > 0) {
            for (int i = 0; i < currentDatePricings.size(); i++) {
                roomNumbers.add(currentDatePricings.get(i).getRoom().getRoomNumber());
            }
        }
        for (int i = 0; i < checkedInReservations.size(); i++) {
            reservationTypes = reservationTypeRepository.findAllByReservation(checkedInReservations.get(i));
            for (int j = 0; j < reservationTypes.size(); j++) {
                reservationType = reservationTypes.get(j);
                newCheckIns = checkInRepository.findAllByReservationType(reservationType);
                for (int k = 0; k < reservationType.getNumberOfRooms(); k++) {
                    if(!roomNumbers.contains(newCheckIns.get(0).getRoom().getRoomNumber())) {
                        pricing = new Pricing();
                        pricing.setReservationType(reservationType);
                        pricing.setRoom(newCheckIns.get(0).getRoom());
                        pricing.setTourOperator(checkedInReservations.get(i).getTourOperator());
                        pricing.setPricingOfRoom(0);
                        pricing.setPricingOfTourOperator(0);
                        pricing.setDate(currentLocalDate);
                        pricings.add(pricing);
                        pricingRepository.save(pricing);
                    }
                    else{
                        pricing = new Pricing();
                        pricing = pricingRepository.findByDateAndRoom(currentLocalDate,newCheckIns.get(0).getRoom());
                        List<CustomerPricing> customerPricings  = new ArrayList<>();
                        customerPricings = customerPricingRepository.findAllByRoomNumberAndDate(newCheckIns.get(0).getRoom().getRoomNumber(),currentLocalDate);
                        try{
                            float customerAllPrice=0;
                            for (int l = 0; l < customerPricings.size(); l++) {
                                customerAllPrice+=customerPricings.get(l).getPrice();
                            }
                            pricing.setPricingOfRoom(customerAllPrice);
                            pricingRepository.save(pricing);
                        }catch(Exception e){}
                    }
                }
            }
        }
        ReservationType reservationType1;
        Contract contract;
        List<ContractRoomType> contractRoomTypes = new ArrayList<>();
        List<ReservationType> reservationTypesList = new ArrayList<>();
        ContractRoomType contractRoomType;
        float tourOperatorPricePerDay=0;
        String term="";

        for (int i = 0; i < pricings.size(); i++) {
            reservationType1 = reservationTypeRepository.findById(pricings.get(i).getReservationType().getId()).orElse(null);
            term = reservationType1.getTerms();
            for (int j = 0; j < requestedContracts.size(); j++) {
                if(pricings.get(i).getTourOperator() == requestedContracts.get(j).getTourOperator()){
                    contract = new Contract();
                    contract = requestedContracts.get(j);
                    contractRoomTypes = contractRoomTypeRepository.findByContractId(contract);
                    for (int k = 0; k < contractRoomTypes.size(); k++) {
                        if(contractRoomTypes.get(k).getRoomType() == pricings.get(i).getRoom().getRoomType() && term.equals(contractRoomTypes.get(k).getTerms())){
                            contractRoomType = new ContractRoomType();
                            contractRoomType = contractRoomTypes.get(k);
                            tourOperatorPricePerDay = tourOperatorPricePerDay + contractRoomType.getNumberOfRooms() * contractRoomType.getRentPrice();
                            tourOperatorPricePerDay = tourOperatorPricePerDay + contractRoomType.getLunchPrice() + contractRoomType.getDinnerPrice() + contractRoomType.getBreakfastPrice();
                        }

                    }
                }
            }
            List<CustomerPricing> customerPricingList  = new ArrayList<>();
            customerPricingList = customerPricingRepository.findAllByRoomNumberAndDate(pricings.get(i).getRoom().getRoomNumber(),currentLocalDate);
            try{
                for (int index = 0; index < customerPricingList.size(); index++) {
                    pricings.get(i).setPricingOfRoom(customerPricingList.get(index).getPrice());
                }

            }catch(Exception e){}

            pricings.get(i).setPricingOfTourOperator(tourOperatorPricePerDay);
            pricingRepository.save(pricings.get(i));
            tourOperatorPricePerDay = 0 ;
        }
    }
    @GetMapping("/getInsideCheckIns/{currentDate}/{roomNumber}")
    public ResponseEntity<MessageResponse> getInsideCheckIns(@PathVariable String currentDate, @PathVariable int roomNumber){
        boolean flag=false;
        Room room = new Room();
        room = roomRepository.findByRoomNumber(roomNumber);
        LocalDate currentLocalDate = LocalDate.parse(currentDate);
        List<CheckIn> allCheckIns = new ArrayList<>();
        allCheckIns = checkInRepository.findAllByRoom(room);
        for (int i = 0; i < allCheckIns.size(); i++) {
            if(currentLocalDate.isEqual(allCheckIns.get(i).getReservation().getCheckInDate()) || currentLocalDate.isAfter(allCheckIns.get(i).getReservation().getCheckInDate()) && currentLocalDate.isBefore(allCheckIns.get(i).getReservation().getCheckOutDate()) ){
                flag = true;
                break;
            }
        }
        if(flag){
            //and return OK message
            return ResponseEntity.ok(new MessageResponse("Rooms is checked in right now"));
        }else{
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("You can't charge room "+roomNumber+" because it's not checked in for requested date "+currentLocalDate+"."));
        }
    }
    }