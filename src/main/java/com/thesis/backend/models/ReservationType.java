package com.thesis.backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class ReservationType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private int numberOfRooms;

    private String terms;

    private int numberOfChildren;

    private int numberOfAdults;


    @ManyToOne
    @JoinColumn(name="reservationId")
    @JsonBackReference
    private Reservation reservation;

    @ManyToOne
    @JoinColumn(name="roomType")
    @JsonBackReference
    private RoomType roomTypeId;
}
