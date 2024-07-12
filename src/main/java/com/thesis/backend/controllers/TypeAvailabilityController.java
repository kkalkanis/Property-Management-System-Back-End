package com.thesis.backend.controllers;

import com.thesis.backend.models.TourOperator;
import com.thesis.backend.models.TypeAvailability;
import com.thesis.backend.payload.request.TypeAvailabilityRequest;
import com.thesis.backend.repository.RoomTypeRepository;
import com.thesis.backend.repository.TourOperatorRepository;
import com.thesis.backend.repository.TypeAvailabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class TypeAvailabilityController {
    private int numberOfTypes;
    private List<TypeAvailability> typeAvailabilityList;
    private TypeAvailabilityRequest typeAvailabilityRequest, typeAvailabilityObject;
    private List<TypeAvailabilityRequest> typeAvailabilityRequestList;
    @Autowired
    private TypeAvailabilityRepository typeAvailRepo;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private TourOperatorRepository tourOperatorRepository;


    @GetMapping("/getTypeAvailability")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Page<TypeAvailabilityRequest> getTypeAvailability(Pageable pageable){
        TourOperator tourOperator;
        tourOperator = tourOperatorRepository.findByName("SELF");
        Page<TypeAvailability> page = typeAvailRepo.findByTourOperatorIdOrderByDateAsc(tourOperator.getId(),pageable);
        typeAvailabilityRequestList = new ArrayList<>();

        for (int i = 0; i < page.getContent().size(); i++) {
            if(page.getContent().get(i).getTourOperator().getName().equals("SELF")) {
                typeAvailabilityRequest = new TypeAvailabilityRequest();
                typeAvailabilityRequest.setAvailable(page.getContent().get(i).getAvailable());
                typeAvailabilityRequest.setDate(page.getContent().get(i).getDate());
                typeAvailabilityRequest.setType(page.getContent().get(i).getType().getType());
                typeAvailabilityRequest.setId(page.getContent().get(i).getId());
                typeAvailabilityRequestList.add(typeAvailabilityRequest);
            }
        }

        Page<TypeAvailabilityRequest> typeAvailabilities = new PageImpl<>(typeAvailabilityRequestList, pageable,page.getTotalElements());
        return typeAvailabilities;
    }
}
