package com.thesis.backend.payload.request;

import com.thesis.backend.models.Customer;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CheckInOutRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private List<Customer> customers = new ArrayList<>();

    private Long roomNumber;

    private Long reservationTypeId;

    private Long reservationId;

}