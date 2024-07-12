package com.thesis.backend.payload.request;

import java.time.LocalDate;

public class PricingRequest {
    private Long id;

    private int pricingOfTourOperator;

    private int pricingOfRoom;

    private LocalDate date;

    private Long tourOperatorId;

    private Long roomId;
}
