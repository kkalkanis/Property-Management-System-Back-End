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
@Table(name="type_availability")
public class TypeAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    private LocalDate date;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "type_id")
    private RoomType type;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "tour_operator_id")
    private TourOperator tourOperator;

    @NotNull
    private int available;

    @NotNull
    private int virtualAvailable;

}
