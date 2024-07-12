package com.thesis.backend.payload.request;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Setter
public class ContractRoomTypeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

     private String roomType;

     private String terms;

     private int numberOfRooms;

    private int numberOfPersons;

     private Long contractId;

    private float rentPrice;

    private float dinnerPrice;

    private float lunchPrice;

    private float breakfastPrice;
}
