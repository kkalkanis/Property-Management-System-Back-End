package com.thesis.backend.payload.request;

import com.thesis.backend.models.Customer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInSingleCustomerRequest {
    private Long id;

    private Customer customer;

    private Long roomNumber;

    private Long reservationTypeId;
}
