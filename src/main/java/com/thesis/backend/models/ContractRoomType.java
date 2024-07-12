package com.thesis.backend.models;

import lombok.Getter;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name="contractRoomType")
public class ContractRoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private int numberOfRooms;

    private int numberOfPersons;

    private String terms;

    private float rentPrice;

    private float dinnerPrice;

    private float lunchPrice;

    private float breakfastPrice;

    @ManyToOne
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "contractId")
    private Contract contractId;
}
