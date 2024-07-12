package com.thesis.backend.payload.request;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDate;

@Getter
@Setter
public class ContractRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private Long contractTypeId;

    private String name;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private String tourOperator;
}
