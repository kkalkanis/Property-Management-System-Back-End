package com.thesis.backend.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name="HotelSpecifics")
@Getter
@Setter
public class HotelSpecifics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@CreationTimestamp
    @NotNull
    private LocalDate startDate ;

    @NotNull
    @Size(max=50)
    private String name;

    @Email
    @NotNull
    private String email;

    @NotNull
    @Size(max=50)
    private String address;

    private int roomCounter;

}
