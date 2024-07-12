package com.thesis.backend.models;

import lombok.Getter;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name="customerPricing")
@Getter
@Setter
public class CustomerPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    private int roomNumber;

    @NotNull
    private String description;

    @NotNull
    private float price;

    @NotNull
    private LocalDate date;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "customerId")
    private Customer customer;

}
