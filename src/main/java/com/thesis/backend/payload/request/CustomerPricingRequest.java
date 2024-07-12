package com.thesis.backend.payload.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CustomerPricingRequest {
    private Long id;

    private int roomNumber;

    private String description;

    private float price;

    private LocalDate date;

    private Long customerId;
}
