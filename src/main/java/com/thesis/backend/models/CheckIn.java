package com.thesis.backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name="checkIn")
@Getter
@Setter
public class CheckIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="customerId")
    @JsonBackReference
    private Customer customer;

    @ManyToOne
    @JoinColumn(name="roomId")
    @JsonBackReference
    private Room room;

    @ManyToOne
    @JoinColumn(name="reservationId")
    @JsonBackReference
    private Reservation reservation;

    @OneToOne
    @JoinColumn(name="reservationTypeId")
    private ReservationType reservationType;
}
