package com.thesis.backend.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="roomType")
@Getter
@Setter
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 2)
    private String type;

    @NotNull
    @Size(max = 50)
    private String description;

    @NotNull
    @Max(20)
    private int numberOfPersons;

    @OneToMany(mappedBy = "roomType",cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Room> roomItems = new HashSet<>();

    @OneToMany(mappedBy = "type",cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<TypeAvailability> typeAvailabilityItems = new HashSet<>();

}
