package com.thesis.backend.repository;

import com.thesis.backend.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer,Long> {

    Customer findByContactPhone(String contactPhone);

    List<Customer> findAllByOrderByLastNameAsc();

    List<Customer> findAllByOrderByLastNameDesc();


    Customer findByPassport(String passport);
}
