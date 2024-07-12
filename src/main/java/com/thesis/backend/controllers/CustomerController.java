package com.thesis.backend.controllers;

import com.thesis.backend.models.CheckIn;
import com.thesis.backend.models.Customer;
import com.thesis.backend.models.Room;
import com.thesis.backend.payload.response.MessageResponse;
import com.thesis.backend.repository.CheckInRepository;
import com.thesis.backend.repository.CustomerRepository;
import com.thesis.backend.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class CustomerController {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping("/getAllCustomers/{orderSelection}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Customer> getCustomers(@PathVariable String orderSelection) {
        List<Customer> customers = new ArrayList<>();
        if (orderSelection.equals("Ascending"))
            customers = customerRepository.findAllByOrderByLastNameAsc();
        else
            customers = customerRepository.findAllByOrderByLastNameDesc();
        return customers;
    }

    @GetMapping("/getSpecificCustomers/{fullName}/{orderSelection}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Customer> getSCustomersLive(@PathVariable String fullName, @PathVariable String orderSelection) {
        List<Customer> customers = new ArrayList<>();
        String[] array = fullName.split(" +");
        System.out.println(orderSelection);
        for (int i = 0; i < array.length; i++) {
            System.out.println(array[i]);
        }
        List<Customer> newCustomers = new ArrayList<>();
        if (orderSelection.equals("Ascending") && array.length == 1) {
            customers = customerRepository.findAllByOrderByLastNameAsc();
            for (int i = 0; i < customers.size(); i++) {
                if (customers.get(i).getFirstName().startsWith(array[0])) {
                    newCustomers.add(customers.get(i));
                }
            }
        } else if (orderSelection.equals("Descending") && array.length == 1) {
            customers = customerRepository.findAllByOrderByLastNameDesc();
            for (int i = 0; i < customers.size(); i++) {
                if (customers.get(i).getFirstName().startsWith(array[0])) {
                    newCustomers.add(customers.get(i));
                }
            }
        } else if (array.length == 2 && orderSelection.equals("Ascending")) {
            customers = customerRepository.findAllByOrderByLastNameDesc();
            for (int i = 0; i < customers.size(); i++) {
                if (customers.get(i).getFirstName().startsWith(array[0]) || customers.get(i).getLastName().startsWith(array[1])) {
                    newCustomers.add(customers.get(i));
                }
            }
        } else if (array.length == 2 && orderSelection.equals("Descending")) {
            customers = customerRepository.findAllByOrderByLastNameDesc();
            for (int i = 0; i < customers.size(); i++) {
                if (customers.get(i).getFirstName().startsWith(array[0]) || customers.get(i).getLastName().startsWith(array[1])) {
                    newCustomers.add(customers.get(i));
                }
            }
        }
        return newCustomers;
    }

    @DeleteMapping("/deleteCustomer/{customerId}")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void deleteCustomer(@PathVariable Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        customerRepository.delete(customer);
    }

    @PostMapping("/saveCustomer")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> setReservation(@Valid @RequestBody Customer customer) {
        List<Customer> customers = new ArrayList<>();
        customers = customerRepository.findAll();

            for (int i = 0; i <customers.size(); i++) {
                if(customers.get(i).getPassport().equals(customer.getPassport())){
                    return ResponseEntity
                            .badRequest()
                            .body(new MessageResponse("Customer Already Exists!"));
                }
            }

        customerRepository.save(customer);
        return ResponseEntity.ok(new MessageResponse("Customer Created Successfully"));
    }

    @PutMapping("/updateCustomer")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateCustomer(@Valid @RequestBody Customer customer) {
        List<Customer> customers = new ArrayList<>();
        customers = customerRepository.findAll();
        for (int i = 0; i < customers.size(); i++) {
            if(customers.get(i).getPassport().equals(customer.getPassport()) && customers.get(i).getCustomerId()!=customer.getCustomerId()){
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Customer with same passport found!"));
            }
        }
        customerRepository.save(customer);
        return ResponseEntity.ok(new MessageResponse("Customer Updated Successfully"));
    }

    @GetMapping("/getCustomersByRoom/{roomNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void getCustomersByRoom(@PathVariable int roomNumber){
        Room room = roomRepository.findByRoomNumber(roomNumber);
        List<Customer> customersByRoom = new ArrayList<>();
        List<CheckIn> checkInByRoom = new ArrayList<>();
        checkInByRoom = checkInRepository.findAllByRoom(room);

    }

}
