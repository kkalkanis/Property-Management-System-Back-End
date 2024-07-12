package com.thesis.backend.payload.request;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
public class ReservationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    private String tourOperatorName;

    @NotNull
    private LocalDate checkInDate ;

    @NotNull
    private LocalDate checkOutDate ;

    @NotNull
    private String reservationName ;

    private String status ;

    private String contactPhone ;

    // private Long customerId;

    /*
    private int numberOfChildren;

    private String status;

    @NotNull
    private int roomNumber;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 10,min = 10)
    private String contactPhone;

    private String email; */

}
