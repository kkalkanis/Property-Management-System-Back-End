package com.thesis.backend.controllers;

import com.thesis.backend.models.CheckIn;
import com.thesis.backend.models.Reservation;
import com.thesis.backend.models.ReservationType;
import com.thesis.backend.payload.request.ReservationRequest;
import com.thesis.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class CheckOutController {
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

    @GetMapping("/getDepartures/{date}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<ReservationRequest> getDepartures(@PathVariable String date) {
        List<Reservation> departures = new ArrayList<>();
        departures = reservationRepository.findByCheckOutDate(LocalDate.parse(date));

        List<ReservationRequest> reservationRequests = new ArrayList<>();
        ReservationRequest reservationRequest;
        for (int i = 0; i < departures.size(); i++) {
            reservationRequest = new ReservationRequest();
            reservationRequest.setReservationName(departures.get(i).getReservationName());
            reservationRequest.setCheckInDate(departures.get(i).getCheckInDate());
            reservationRequest.setCheckOutDate(departures.get(i).getCheckOutDate());
            reservationRequest.setId(departures.get(i).getId());
            reservationRequest.setStatus(departures.get(i).getStatus());
            reservationRequest.setTourOperatorName(departures.get(i).getTourOperator().getName());
            reservationRequests.add(reservationRequest);
        }
        return reservationRequests;
    }

    @GetMapping("/getAllDepartureRooms/{date}/{reservationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void getAllDepartureRooms(@PathVariable Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        List<ReservationType> reservationTypes = new ArrayList<>();
        List<CheckIn> checkIns = new ArrayList<>();
        reservationTypes = reservationTypeRepository.findAllByReservation(reservation);
        for (int i = 0; i < reservationTypes.size(); i++) {
            checkIns = checkInRepository.findAllByReservationType(reservationTypes.get(i));
        }
    }
}