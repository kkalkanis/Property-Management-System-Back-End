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
@Table(name="availability")
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "room_id")
    private Room room;

    @NotNull
    private LocalDate date;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "type_id")
    private RoomType type;

    @NotNull
    private int available;

    @NotNull
    private int virtualAvailable;
}
