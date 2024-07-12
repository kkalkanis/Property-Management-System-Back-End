package com.thesis.backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="room")
@Getter
@Setter
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Min(1)
    @Max(999)
    private int roomNumber;

    @NotNull
    @Size(max = 50)
    private String description;

    @Size(max = 50)
    private String status;

    //@Column(name="type",insertable=false, updatable=false)

    @ManyToOne
    @JoinColumn(name="type")
    @JsonBackReference
    private RoomType roomType;

    @OneToMany(mappedBy="room",cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Availability> availabilityItems = new HashSet<>();


    /*
    @OneToMany(mappedBy="roomId",cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Reservation> reservations = new HashSet<>(); */


}

