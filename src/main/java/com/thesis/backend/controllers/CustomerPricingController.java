package com.thesis.backend.controllers;

import com.thesis.backend.models.Customer;
import com.thesis.backend.models.CustomerPricing;
import com.thesis.backend.payload.request.CustomerPricingRequest;
import com.thesis.backend.repository.CustomerPricingRepository;
import com.thesis.backend.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class CustomerPricingController {

    @Autowired
    private CustomerPricingRepository customerPricingRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/postCustomerDailyPricing")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void postCustomerDailyPricing(@Valid @RequestBody CustomerPricingRequest customerPricingRequest){
        Customer customer = new Customer();
        customer = customerRepository.findById(customerPricingRequest.getCustomerId()).orElse(null);
        CustomerPricing customerPricingObject= new CustomerPricing();

        customerPricingObject = new CustomerPricing();
        customerPricingObject.setPrice(customerPricingRequest.getPrice());
        customerPricingObject.setDescription(customerPricingRequest.getDescription());
        customerPricingObject.setDate(customerPricingRequest.getDate());
        customerPricingObject.setRoomNumber(customerPricingRequest.getRoomNumber());
        customerPricingObject.setCustomer(customer);

        customerPricingRepository.save(customerPricingObject);
    }
}
