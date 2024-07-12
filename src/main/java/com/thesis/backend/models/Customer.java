package com.thesis.backend.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name="customer")
@Getter
@Setter
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customerId")
    private Long customerId;

    private String firstName;

    private String lastName;

    private String passport;

    private String contactPhone;

    private String email;

    private String city;

    private String country;

    private String address;

    private String zipCode;


   /* @OneToMany(mappedBy = "customer",cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Reservation> reservationItems = new HashSet<>();

    public void add(Reservation reservation) {

        if (reservation != null) {

            if (reservationItems == null) {
                reservationItems = new HashSet<>();
            }

            reservationItems.add(reservation);
            reservation.setCustomer(this);
        }
    } */
}
