package com.thesis.backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name="reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    private LocalDate checkInDate ;

    @NotNull
    private LocalDate checkOutDate ;

    @NotNull
    private String reservationName ;

    @NotNull
    private String status ;

    private String contactPhone;


    //private int numberOfChildren;

    //private String status;

    /*
    @ManyToOne
    @JoinColumn(name="customerId")
    @JsonBackReference
    private Customer customer; */

    @ManyToOne
    @JoinColumn(name="tourOperatorId")
    @JsonBackReference
    private TourOperator tourOperator;

    /*
    @ManyToOne
    @JoinColumn(name="roomId")
    @JsonBackReference
    private Room roomId; */
}
