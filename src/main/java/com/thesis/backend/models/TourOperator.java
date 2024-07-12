package com.thesis.backend.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="tourOperator")
@Getter
@Setter
public class TourOperator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 50)
    private String name;

    @OneToMany(mappedBy = "tourOperator",cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Reservation> reservationItems = new HashSet<>();

    public void add(Reservation reservation) {

        if (reservation != null) {

            if (reservationItems == null) {
                reservationItems = new HashSet<>();
            }

            reservationItems.add(reservation);
            reservation.setTourOperator(this);
        }
    }
}
