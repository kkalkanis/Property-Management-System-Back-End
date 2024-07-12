package com.thesis.backend.controllers;

import com.thesis.backend.models.*;
import com.thesis.backend.payload.request.ContractRequest;
import com.thesis.backend.payload.request.ContractRoomTypeRequest;
import com.thesis.backend.payload.response.MessageResponse;
import com.thesis.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ContractController {
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private RoomTypeRepository typeRepository;

    @Autowired
    private ContractRoomTypeRepository contractRoomTypeRepository;

    @Autowired
    private TourOperatorRepository tourOperatorRepository;

    @Autowired
    private TypeAvailabilityRepository typeAvailabilityRepository;


    @PostMapping("/saveContract")
    public ResponseEntity<?> saveContract(@RequestBody ContractRequest contractRequest) {
        List<Contract> tourOperatorContractsByDate = new ArrayList<>();
        String message = "";
        Contract contract = new Contract();

        TourOperator tourOperator = tourOperatorRepository.findByName(contractRequest.getTourOperator());
        try {
            if (tourOperator.getId() != null) {
                tourOperatorContractsByDate = contractRepository.findAllByTourOperator(tourOperator);
                for (int i = 0; i < tourOperatorContractsByDate.size(); i++) {
                    if(contractRequest.getCheckInDate().isBefore(tourOperatorContractsByDate.get(i).getCheckOutDate()) && contractRequest.getCheckOutDate().isAfter(tourOperatorContractsByDate.get(i).getCheckInDate())){
                        return ResponseEntity
                                .badRequest()
                                .body(new MessageResponse("Cant create contract because there is another contract for same tour operator which contains overlapping dates"));
                    }



                }
                contract.setTourOperator(tourOperator);
                contract.setCheckInDate(contractRequest.getCheckInDate());
                contract.setCheckOutDate(contractRequest.getCheckOutDate());
                Contract response = contractExists(contract);

                message = "Contract created successfully!";
                contractRepository.save(contract);
            }
        } catch (Exception e) {
            TourOperator newOperator = new TourOperator();
            newOperator.setName(contractRequest.getTourOperator());
            tourOperatorRepository.save(newOperator);
            contract.setTourOperator(newOperator);
            contract.setCheckInDate(contractRequest.getCheckInDate());
            contract.setCheckOutDate(contractRequest.getCheckOutDate());
            contractRepository.save(contract);

            message = "Contract created successfully!";
        }

            return ResponseEntity.ok(new MessageResponse("Contract created successfully!"));

    }

    private Contract contractExists(Contract contract) {
        List<Contract> contracts = contractRepository.findAll();
        for (int i = 0; i < contracts.size(); i++) {
            if (contracts.get(i).getTourOperator().getId() == contract.getTourOperator().getId()) {
                if (contracts.get(i).getCheckInDate().isEqual(contract.getCheckInDate())) {
                    if (contracts.get(i).getCheckOutDate().isEqual(contract.getCheckOutDate())) {
                        return contracts.get(i);
                    }
                }
            }
        }
        return null;
    }

    @GetMapping("/contracts")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Page<ContractRequest> getPage(Pageable pageable) {

        Page<Contract> page = contractRepository.findAll(pageable);
        List<ContractRoomType> page2 = contractRoomTypeRepository.findAll();
        List<TourOperator> page3 = tourOperatorRepository.findAll();

        List<ContractRequest> newContracts = new ArrayList<>();
        for (int i = 0; i < page.getContent().size(); i++) {
            ContractRequest tempContract = new ContractRequest();
            tempContract.setId(page.getContent().get(i).getId());
            tempContract.setCheckInDate(page.getContent().get(i).getCheckInDate());
            tempContract.setCheckOutDate(page.getContent().get(i).getCheckOutDate());
            for (int k = 0; k < page3.size(); k++) {
                if (page.getContent().get(i).getTourOperator().getId() == page3.get(k).getId()) {
                    tempContract.setTourOperator(page3.get(k).getName());
                }
            }
            newContracts.add(tempContract);
        }
        Page<ContractRequest> contracts = new PageImpl<>(newContracts, pageable, page.getTotalElements());

        return contracts;
    }

    @DeleteMapping("/delContract/{delId}")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void deleteContract(@PathVariable Long delId) {
        List<ContractRoomType> contractRoomTypes = new ArrayList<>();
        contractRoomTypes = contractRoomTypeRepository.findAll();

        //Calculate sum of virtualRooms per type per contract
        //Because virtual availability may contain rooms
        ///from other contracts , so we don't want to delete whole row in typeAvailability
        List<Integer> contractSumOfVirtualRooms = new ArrayList<>();
        List<RoomType> roomTypes = new ArrayList<>();
        for (int i = 0; i < contractRoomTypes.size(); i++) {
            if (contractRoomTypes.get(i).getContractId().getId() == delId) {
                roomTypes.add(contractRoomTypes.get(i).getRoomType()); //Add roomTypes
                contractSumOfVirtualRooms.add(contractRoomTypes.get(i).getNumberOfRooms()); //Add number of rooms
                contractRoomTypeRepository.delete(contractRoomTypes.get(i));
            }
        }

        //Set contract we want to delete
        Contract contract = new Contract();
        contract = contractRepository.findById(delId).orElse(null);

        //Now we can delete rows from TypeAvailability
        List<TypeAvailability> typeAvailabilities = new ArrayList<>();
        typeAvailabilities = typeAvailabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperator(contract.getCheckOutDate(), contract.getCheckInDate(), contract.getTourOperator());

        for (int i = 0; i < typeAvailabilities.size(); i++) {
            for (int j = 0; j < roomTypes.size(); j++) {
                if (roomTypes.get(j).equals(typeAvailabilities.get(i).getType())) {
                    typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() - contractSumOfVirtualRooms.get(j));
                    if (typeAvailabilities.get(i).getVirtualAvailable() == 0) {
                        typeAvailabilityRepository.delete(typeAvailabilities.get(i));
                    }
                }
            }
        }

        contractRepository.delete(contract);
    }

    @PutMapping("/updateContract")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> updateContract(@Valid @RequestBody ContractRequest contractRequest) {
        Contract contract = new Contract();
        Contract oldContract = contractRepository.findById(contractRequest.getId()).orElse(null);
        TourOperator tourOperator;
        tourOperator = tourOperatorRepository.findById(oldContract.getTourOperator().getId()).orElse(null);
        List<Contract> tourOperatorContractsByDate = new ArrayList<>();
        //Check for overlapping dates
        tourOperatorContractsByDate = contractRepository.findAllByTourOperator(tourOperator);
        for (int i = 0; i < tourOperatorContractsByDate.size(); i++) {
            //leftside overlapping
            if(contractRequest.getCheckInDate().isBefore(tourOperatorContractsByDate.get(i).getCheckOutDate()) && contractRequest.getCheckOutDate().isAfter(tourOperatorContractsByDate.get(i).getCheckInDate())){
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Cant update contract because there is another contract for same tour operator which contains overlapping dates"));
            }



        }


        contract.setId(oldContract.getId());
        contract.setCheckInDate(contractRequest.getCheckInDate());
        contract.setCheckOutDate(contractRequest.getCheckOutDate());
        contract.setTourOperator(tourOperator);

        //If date range remain the same for same tour operator(no update requested) then just return a message and terminate API
        if (oldContract.getCheckInDate().isEqual(contract.getCheckInDate())) {
            if (oldContract.getCheckOutDate().isEqual(contract.getCheckOutDate())) {
                if (tourOperator == oldContract.getTourOperator()) {
                    return ResponseEntity
                            .badRequest()
                            .body(new MessageResponse("Contract remains with same date range as asked!"));
                }
            }
        }

        //Now we have to check if there are saved Contract Room Types to update TypeAvailability table
        List<ContractRoomType> contractRoomTypes = new ArrayList<>();
        List<ContractRoomType> contractRoomTypeList = new ArrayList<>();
        contractRoomTypes = contractRoomTypeRepository.findAll();
        int contractRoomTypesCounter = 0;
        for (int i = 0; i < contractRoomTypes.size(); i++) {
            if (contractRoomTypes.get(i).getContractId().getId() == contractRequest.getId()) {
                contractRoomTypesCounter++;
                contractRoomTypeList.add(contractRoomTypes.get(i)); //save contract room types to use them later
            }
        }

        //If contractRoomTypesCounter >0 this means that we have to update TypeAvailability table too
        if (contractRoomTypesCounter > 0) {
            List<LocalDate> datesMustUnshackle = new ArrayList<>();
            List<LocalDate> datesToCreate = new ArrayList<>();
            List<RoomType> differentRoomTypes = new ArrayList<>();
            datesToCreate = reservationDateExistsInOldReservation(contract, oldContract, datesMustUnshackle);

            //Method starts Here
            List<RoomType> roomTypes = new ArrayList<>();

            for (int i = 0; i < contractRoomTypeList.size(); i++) {
                roomTypes.add(contractRoomTypeList.get(i).getRoomType());
            }

            List<RoomType> sumOfDifferentContractTypes = new ArrayList<>(new HashSet<>(roomTypes)); //remove duplicates , sumOfDifferentContract

            List<Integer> sumOfRoomTypes = Arrays.asList(new Integer[sumOfDifferentContractTypes.size()]);

            for (int i = 0; i < sumOfRoomTypes.size(); i++) {
                sumOfRoomTypes.set(i, 0);
            }

            for (int i = 0; i < contractRoomTypeList.size(); i++) {
                for (int j = 0; j < sumOfDifferentContractTypes.size(); j++) {
                    if (contractRoomTypeList.get(i).getRoomType().getType().equals(sumOfDifferentContractTypes.get(j).getType())) {
                        sumOfRoomTypes.set(j, sumOfRoomTypes.get(j) + contractRoomTypeList.get(i).getNumberOfRooms());
                    }
                }
            }
            //Method ends Here

            if(datesToCreate.size()>0) {
                List<TypeAvailability> selfAvailabilities = new ArrayList<>();
                TourOperator selfTourOperator = tourOperatorRepository.findByName("SELF");
                selfAvailabilities = typeAvailabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperator(contractRequest.getCheckOutDate(),contractRequest.getCheckInDate(),selfTourOperator);
                TypeAvailability typeAvailability;
                List<TypeAvailability> newAvailabilities = new ArrayList<>();

                for (int i = 0; i < datesToCreate.size(); i++) {
                    for (int j = 0; j < sumOfDifferentContractTypes.size(); j++) {
                            typeAvailability = new TypeAvailability();
                            typeAvailability.setDate(datesToCreate.get(i));
                            typeAvailability.setTourOperator(tourOperator);
                            typeAvailability.setType(sumOfDifferentContractTypes.get(j));
                            typeAvailability.setVirtualAvailable(sumOfRoomTypes.get(j));
                            newAvailabilities.add(typeAvailability);
                            typeAvailabilityRepository.save(typeAvailability);

                    }
                }
                for (int i = 0; i < newAvailabilities.size(); i++) {
                    for (int j = 0; j < selfAvailabilities.size(); j++) {
                        if(newAvailabilities.get(i).getDate().isEqual(selfAvailabilities.get(j).getDate())){
                            if(sumOfDifferentContractTypes.contains(selfAvailabilities.get(j).getType())){
                                newAvailabilities.get(i).setAvailable(selfAvailabilities.get(j).getAvailable());
                                typeAvailabilityRepository.save(newAvailabilities.get(i));
                            }
                        }
                    }

                }
            }
                if (datesMustUnshackle.size() > 0) {
                    List<TypeAvailability> typeAvailabilities = new ArrayList<>();
                    typeAvailabilities = typeAvailabilityRepository.findAllByTourOperatorOrderByDateAsc(tourOperator);
                    for (int i = 0; i < typeAvailabilities.size(); i++) {
                        if (datesMustUnshackle.contains(typeAvailabilities.get(i).getDate())){
                            for (int j = 0; j < sumOfDifferentContractTypes.size(); j++) {
                                if(typeAvailabilities.get(i).getType().getType().equals( sumOfDifferentContractTypes.get(j).getType())){
                                    typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable()-sumOfRoomTypes.get(j));
                                    if(typeAvailabilities.get(i).getVirtualAvailable()<=0){
                                        typeAvailabilityRepository.delete(typeAvailabilities.get(i));
                                    }
                                }
                            }
                        }
                    }
                }
            }



        contractRepository.save(contract);

        return ResponseEntity.ok(new MessageResponse("Contract Room Type updated successfully!"));

    }




    private List<LocalDate> reservationDateExistsInOldReservation(Contract contract, Contract oldContract, List<LocalDate> datesMustUnshackle) {

        List<LocalDate> reservationDates = new ArrayList<>();
        List<LocalDate> oldReservationDates = new ArrayList<>();

        for (LocalDate date = contract.getCheckInDate(); date.isBefore(contract.getCheckOutDate()); date = date.plusDays(1)) {
            reservationDates.add(date);
        }
        for (LocalDate date = oldContract.getCheckInDate(); date.isBefore(oldContract.getCheckOutDate()); date = date.plusDays(1)) {
            oldReservationDates.add(date);
        }
        //Check for out of bounds date
        if ((contract.getCheckOutDate().isBefore(oldContract.getCheckInDate()) || contract.getCheckOutDate().isEqual(oldContract.getCheckInDate()) || (contract.getCheckInDate().isAfter(oldContract.getCheckOutDate()) || contract.getCheckInDate().isEqual(oldContract.getCheckOutDate())))) {
            datesMustUnshackle.addAll(oldReservationDates);
            return reservationDates;
        }
        //Check for overlapping dates
        if ((contract.getCheckInDate().isEqual(oldContract.getCheckInDate()) || contract.getCheckInDate().isAfter(oldContract.getCheckInDate())) && (contract.getCheckOutDate().isBefore(oldContract.getCheckOutDate()) || contract.getCheckOutDate().isEqual(oldContract.getCheckOutDate()))) {   //Calculate dates must set back available
            Set<LocalDate> ad = new HashSet<LocalDate>(oldReservationDates);
            Set<LocalDate> tempNewRes = new HashSet<LocalDate>(reservationDates);
            datesMustUnshackle.addAll(ad);
            datesMustUnshackle.removeAll(tempNewRes);
            System.out.println("Overlapping dates detected");
            return new ArrayList<>();
        }
        // Print the result
        Set<LocalDate> ad = new HashSet<LocalDate>(oldReservationDates);
        Set<LocalDate> tempOldRes = new HashSet<LocalDate>(oldReservationDates);
        Set<LocalDate> bd = new HashSet<LocalDate>(reservationDates);
        Set<LocalDate> tempNewRes = new HashSet<LocalDate>(reservationDates);
        List<LocalDate> datesToCheck = new ArrayList<>();

        bd.removeAll(tempOldRes);
        datesToCheck.addAll(bd);
        datesMustUnshackle.addAll(ad);
        datesMustUnshackle.removeAll(tempNewRes);

        System.out.println("Size dates must unshackle "+datesMustUnshackle.size());
  
        return datesToCheck;
    }



    ////////////////////////////////Room Type Contract APIS //////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PostMapping("/saveRoomTypeContract")
    public ResponseEntity<?> saveRoomTypeContract(@RequestBody ContractRoomTypeRequest contractRoomTypeRequest) {
        if(contractRoomTypeRequest.getNumberOfRooms()==0){
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("You can't create contract type with 0 number of rooms"));
        }
        String message="";
        RoomType roomType = roomTypeRepository.findByType(contractRoomTypeRequest.getRoomType());

        List<Contract> contracts = new ArrayList<>();
        contracts = contractRepository.findAll();

        Contract currentContract = new Contract();
        currentContract = contractRepository.findById(contractRoomTypeRequest.getContractId()).orElse(null);
        List<ContractRoomType> contractRoomTypes = new ArrayList<>();
        contractRoomTypes = contractRoomTypeRepository.findByContractId(currentContract);
        int sum=0;
        for (int i = 0; i < contractRoomTypes.size(); i++) {
            if(contractRoomTypes.get(i).getRoomType().getType().equals(contractRoomTypeRequest.getRoomType())){
                sum+=contractRoomTypes.get(i).getNumberOfRooms();
            }
        }


        //Iterate through contracts to save new type to its belonging contract
        for (int i = 0; i < contracts.size(); i++) {
            //save typeContract to specific contract id
            if (contracts.get(i).getId() == contractRoomTypeRequest.getContractId()) {
                    ContractRoomType contractRoomType = new ContractRoomType();
                    contractRoomType.setRoomType(roomType);
                    contractRoomType.setNumberOfPersons(contractRoomTypeRequest.getNumberOfPersons());
                    contractRoomType.setTerms(contractRoomTypeRequest.getTerms());
                    contractRoomType.setRentPrice(contractRoomTypeRequest.getRentPrice());
                    contractRoomType.setDinnerPrice(contractRoomTypeRequest.getDinnerPrice());
                    contractRoomType.setLunchPrice(contractRoomTypeRequest.getLunchPrice());
                    contractRoomType.setBreakfastPrice(contractRoomTypeRequest.getBreakfastPrice());
                    contractRoomType.setContractId(contracts.get(i));
                    if(contractRoomType.getContractId().getTourOperator().getName().equals("SELF")){
                        boolean availabilityExists = true;
                        List<TypeAvailability> typeAvailabilityList = new ArrayList<>();
                        typeAvailabilityList = typeAvailabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperatorAndType(contractRoomType.getContractId().getCheckOutDate(),contractRoomType.getContractId().getCheckInDate(),contractRoomType.getContractId().getTourOperator(),contractRoomType.getRoomType());
                        for (int j = 0; j < typeAvailabilityList.size(); j++) {
                                if (contractRoomTypeRequest.getNumberOfRooms() > typeAvailabilityList.get(j).getAvailable()  ) {
                                    availabilityExists=false;
                                    int temp= typeAvailabilityList.get(j).getAvailable()-sum;
                                    return ResponseEntity
                                            .badRequest()
                                            .body(new MessageResponse("There is no availability, " + temp + " rooms left! " + "Collision Found at " + typeAvailabilityList.get(j).getDate() + "!"));
                                }
                            if (contractRoomTypeRequest.getNumberOfRooms()+sum >typeAvailabilityList.get(j).getAvailable() ) {
                                availabilityExists=false;
                                int temp= typeAvailabilityList.get(j).getAvailable()-sum;
                                return ResponseEntity
                                        .badRequest()
                                        .body(new MessageResponse("There is no availability, " + temp + " rooms left! " + "Collision Found at " + typeAvailabilityList.get(j).getDate() + "!"));
                            }

                        }
                            if(availabilityExists){
                                contractRoomType.setNumberOfRooms(contractRoomTypeRequest.getNumberOfRooms());
                                contractRoomTypeRepository.save(contractRoomType);
                                return ResponseEntity.ok(new MessageResponse("Contract Room Type created successfully!"));
                            }


                    }
                    contractRoomType.setNumberOfRooms(contractRoomTypeRequest.getNumberOfRooms());
                    contractRoomTypeRepository.save(contractRoomType);

                    //See if tour operator have already typeAvailability for same dates
                    // If yes we don't have to create new records but just update virtual availability
                    // This method returns a List with already created dates in typeAvailability table
                    // in order to not create them again
                    List<LocalDate> existingDates = new ArrayList<>();
                    existingDates=checkForSameDatesByTourOperator(contracts.get(i).getTourOperator(),contracts.get(i).getCheckInDate(),contracts.get(i).getCheckOutDate(),roomType);


                    // If operator is not SELF we have to create some rows for typeAvailability for specific range and roomType
                    if (!contracts.get(i).getTourOperator().getName().equals("SELF")) {
                        TypeAvailability typeAvailability;
                        for (LocalDate date = contracts.get(i).getCheckInDate(); date.isBefore(contracts.get(i).getCheckOutDate()); date = date.plusDays(1)) {
                            if(!existingDates.contains(date)){
                                typeAvailability = new TypeAvailability();
                                typeAvailability.setDate(date);
                                typeAvailability.setTourOperator(contracts.get(i).getTourOperator());
                                typeAvailabilityRepository.save(typeAvailability);
                                message = "Created successfully!";
                            }
                        }

                    //and then we have to update TypeAvailability table
                    TourOperator tourOperator = contracts.get(i).getTourOperator();
                    List<TypeAvailability> typeAvailabilities = new ArrayList<>();
                    typeAvailabilities = typeAvailabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperator(contracts.get(i).getCheckOutDate(),contracts.get(i).getCheckInDate(),tourOperator);

                    for (int j = 0; j < typeAvailabilities.size(); j++) {
                                try {
                                    //if true means that it's not a new row but a row that we want to update its virtual available field
                                    //and at the same time we have to make sure that we update rows with ContractRoomType's room type only
                                    if (typeAvailabilities.get(j).getType().getId() != null && typeAvailabilities.get(j).getType() == roomType){
                                        int newVirtual = typeAvailabilities.get(j).getVirtualAvailable()+contractRoomType.getNumberOfRooms();
                                        typeAvailabilities.get(j).setVirtualAvailable(newVirtual);
                                        typeAvailabilityRepository.save(typeAvailabilities.get(j));
                                    }
                                    //If type is null it means that we talk about a new record in typeAvailability table
                                    //and in this case code inside catch will execute
                                    //in order to set new virtualAvailable field and room type appropriately
                                    else if (typeAvailabilities.get(j).getType().getId() == null) {
                                        ;
                                    }
                                } catch (Exception e) {
                                    typeAvailabilities.get(j).setType(contractRoomType.getRoomType());
                                    typeAvailabilities.get(j).setVirtualAvailable(contractRoomType.getNumberOfRooms());
                                    //real availability means that we need to retrieve
                                    //availability for specific dates
                                    //with tour operator id = self
                                    //and the RoomType we need
                                    TourOperator operator = new TourOperator();
                                    operator= tourOperatorRepository.findByName("SELF");
                                    List<TypeAvailability> selfAvailability = new ArrayList<>();
                                    selfAvailability = typeAvailabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperatorAndType(contracts.get(i).getCheckOutDate(),contracts.get(i).getCheckInDate(),operator,contractRoomType.getRoomType());
                                    for (int k = 0; k < selfAvailability.size(); k++) {
                                        if(selfAvailability.get(k).getDate().isEqual(typeAvailabilities.get(j).getDate())){
                                            typeAvailabilities.get(j).setAvailable(selfAvailability.get(k).getAvailable());
                                            break;
                                        }
                                    }
                                    typeAvailabilityRepository.save(typeAvailabilities.get(j));

                                }
                    }
                    }
                    break; //This breaks 1st for loop when everything is ready and not iterate again through contract table
            }
        }
            return ResponseEntity.ok(new MessageResponse("Contract Room Type created successfully!"));

    }

    private List<LocalDate> checkForSameDatesByTourOperator(TourOperator tourOperator, LocalDate checkInDate, LocalDate checkOutDate, RoomType roomType) {
        List<TypeAvailability> typeAvailabilities = new ArrayList<>();
        List<LocalDate> existingDates = new ArrayList<>();
        typeAvailabilities = typeAvailabilityRepository.findAllByTourOperatorOrderByDateAsc(tourOperator);
        for (int i = 0; i < typeAvailabilities.size(); i++) {
            if(typeAvailabilities.get(i).getDate().isEqual(checkOutDate)){
                break;
            }
            if(typeAvailabilities.get(i).getDate().isEqual(checkInDate) || typeAvailabilities.get(i).getDate().isBefore(checkOutDate) ){
                if(typeAvailabilities.get(i).getType() == roomType){
                    existingDates.add(typeAvailabilities.get(i).getDate()); // Here we add existing dates in
                }   //typeAvailability table in order to not create them again later
            }
        }
        return existingDates;
    }

    @GetMapping("/getContractRoomTypes")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<ContractRoomTypeRequest> getContractRoomTypes(@RequestParam Long contractId) {
        List<Contract> contracts = contractRepository.findAll();
        Contract contract = new Contract();
        //Find the contract you want to return it's ContractRoomTypes
        for (int i = 0; i < contracts.size(); i++) {
            if (contracts.get(i).getId() == contractId) {
                contract = contracts.get(i);
                break;
            }
        }

        //After we find it we have to return its types as well
        List<ContractRoomType> contractRoomTypes = new ArrayList<>();
        List<ContractRoomTypeRequest> newContractRoomTypes = new ArrayList<>();

        contractRoomTypes = contractRoomTypeRepository.findAll();
        for (int i = 0; i < contractRoomTypes.size(); i++) {
            if (contractRoomTypes.get(i).getContractId() == contract) {
                ContractRoomTypeRequest contractRoomTypeRequest = new ContractRoomTypeRequest();
                contractRoomTypeRequest.setId(contractRoomTypes.get(i).getId());
                contractRoomTypeRequest.setRoomType(contractRoomTypes.get(i).getRoomType().getType());
                contractRoomTypeRequest.setNumberOfRooms(contractRoomTypes.get(i).getNumberOfRooms());
                contractRoomTypeRequest.setNumberOfPersons(contractRoomTypes.get(i).getNumberOfPersons());
                contractRoomTypeRequest.setRentPrice(contractRoomTypes.get(i).getRentPrice());
                contractRoomTypeRequest.setDinnerPrice(contractRoomTypes.get(i).getDinnerPrice());
                contractRoomTypeRequest.setLunchPrice(contractRoomTypes.get(i).getLunchPrice());
                contractRoomTypeRequest.setBreakfastPrice(contractRoomTypes.get(i).getBreakfastPrice());
                contractRoomTypeRequest.setTerms(contractRoomTypes.get(i).getTerms());
                newContractRoomTypes.add(contractRoomTypeRequest); // we hold types we want
            }
        }
        return newContractRoomTypes;
    }

    @DeleteMapping("/delContractRoomType/{delId}")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteContractRoomType(@PathVariable Long delId) {

        List<ContractRoomType> contractRoomTypes = new ArrayList<>();
        contractRoomTypes = contractRoomTypeRepository.findAll();
        ContractRoomType contractRoomType = new ContractRoomType();

        //Find contractRoomType object
        for (int i = 0; i < contractRoomTypes.size(); i++) {
            if(contractRoomTypes.get(i).getId().equals( delId)){
                contractRoomType = contractRoomTypes.get(i);
                break;
            }
        }
        if(contractRoomType.getContractId().getTourOperator().getName().equals("SELF")){
            contractRoomTypeRepository.delete(contractRoomType);
            return ResponseEntity.ok(new MessageResponse("Contract Room Type deleted successfully!"));
        }
        //Find Contract Object to get its tour operator to delete its rows from TypeAvailability
        List<Contract> contracts = new ArrayList<>();
        contracts = contractRepository.findAll();
        Contract contract = new Contract();
        for (int i = 0; i < contracts.size(); i++) {
            if(contracts.get(i).getId() == contractRoomType.getContractId().getId()){
                contract = contracts.get(i);
                break;
            }
        }
        //Now we can delete rows from TypeAvailability
        List<TypeAvailability> typeAvailabilities = new ArrayList<>();
        typeAvailabilities = typeAvailabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperatorAndType(contract.getCheckOutDate(),contract.getCheckInDate(),contract.getTourOperator(),contractRoomType.getRoomType());

        for (int i = 0; i < typeAvailabilities.size(); i++) {
                    typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable()-contractRoomType.getNumberOfRooms());
                    if(typeAvailabilities.get(i).getVirtualAvailable()==0){
                        typeAvailabilityRepository.delete(typeAvailabilities.get(i));
                    }

        }
        contractRoomTypeRepository.delete(contractRoomType);
        return ResponseEntity.ok(new MessageResponse("Contract Room Type deleted successfully!"));
    }

    @PutMapping("/updateRoomTypeContract")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateRoomTypeContract(@Valid @RequestBody ContractRoomTypeRequest contractRoomTypeRequest) {
        if(contractRoomTypeRequest.getNumberOfRooms()==0){
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("You can't update contract type with zero number of rooms "));
        }
        List<ContractRoomType> contractRoomTypes = new ArrayList<>();
        contractRoomTypes = contractRoomTypeRepository.findAll();
        Contract contract = new Contract();
        ContractRoomType oldContractRoomType = new ContractRoomType();
        String oldRoomType= new String("");
        for (int i = 0; i < contractRoomTypes.size(); i++) { //Find contract to assign its room type
            if(contractRoomTypes.get(i).getId().equals(contractRoomTypeRequest.getId())){
                contract = contractRoomTypes.get(i).getContractId();
                oldContractRoomType = contractRoomTypes.get(i); // old contract type info we want to update
                oldRoomType= oldContractRoomType.getRoomType().getType(); // keep room type as string too
                break;
            }
        }
        if(contract.getTourOperator().getName().equals("SELF")){
            boolean availabilityExists = true;
            RoomType roomType = roomTypeRepository.findByType(contractRoomTypeRequest.getRoomType());
            List<TypeAvailability> selfAvailabilities = new ArrayList<>();
            selfAvailabilities = typeAvailabilityRepository.findAllByDateLessThanEqualAndDateGreaterThanEqualAndTourOperatorAndType(contract.getCheckOutDate(),contract.getCheckInDate(),contract.getTourOperator(),roomType);
            for (int j = 0; j < selfAvailabilities.size(); j++) {
                if (contractRoomTypeRequest.getNumberOfRooms() > selfAvailabilities.get(j).getAvailable()) {
                    availabilityExists= false;
                    return ResponseEntity
                            .badRequest()
                            .body(new MessageResponse("There is no availability, " + selfAvailabilities.get(j).getAvailable() + " rooms left! " + "Collision Found at " + selfAvailabilities.get(j).getDate() + "!"));
                }
            }
                if(availabilityExists){
                    ContractRoomType contractRoomType= new ContractRoomType();
                    contractRoomType.setId(contractRoomTypeRequest.getId());
                    contractRoomType.setNumberOfRooms(contractRoomTypeRequest.getNumberOfRooms());
                    contractRoomType.setRoomType(roomType);
                    contractRoomType.setContractId(contract);
                    contractRoomType.setTerms(contractRoomTypeRequest.getTerms());
                    contractRoomType.setNumberOfPersons(contractRoomTypeRequest.getNumberOfPersons());
                    contractRoomType.setRentPrice(contractRoomTypeRequest.getRentPrice());
                    contractRoomType.setBreakfastPrice(contractRoomTypeRequest.getBreakfastPrice());
                    contractRoomType.setDinnerPrice(contractRoomTypeRequest.getDinnerPrice());
                    contractRoomType.setLunchPrice(contractRoomTypeRequest.getLunchPrice());
                    contractRoomTypeRepository.save(contractRoomType);
                    return ResponseEntity.ok(new MessageResponse("Contract Room Type updated successfully!"));
                }
            }

        ////////////////////////////////////////////////////////////////////////////////
        //Here we calculate if we have to add or decrease virtual availability in case
        //that user only change numberOfRooms and keep same roomType for contractRoomType
        int newVirtualAvailability=0;
        char sign;
        //If user want to decrease contract room type's numberOfRooms
        if(oldContractRoomType.getNumberOfRooms()>contractRoomTypeRequest.getNumberOfRooms()){
            newVirtualAvailability = oldContractRoomType.getNumberOfRooms() - contractRoomTypeRequest.getNumberOfRooms();
            sign='-';
        }
        else{ //means that he want to add rooms on virtual availability
            newVirtualAvailability = contractRoomTypeRequest.getNumberOfRooms()-oldContractRoomType.getNumberOfRooms();
            sign='+';
        }
        /////////////////////////////////////////////////////////////////////////////////

        //If user choose to change room type we want to know that, in order to update typeAvailability respectively
        boolean roomTypeChanged = false;
        if(!oldContractRoomType.getRoomType().getType().equals(contractRoomTypeRequest.getRoomType())){
            roomTypeChanged = true; // If true means that room type has changed
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        //Update Contract Room Type record and save it
        RoomType roomType = roomTypeRepository.findByType(contractRoomTypeRequest.getRoomType());
        ContractRoomType contractRoomType = new ContractRoomType();
        contractRoomType.setId(contractRoomTypeRequest.getId());
        contractRoomType.setRoomType(roomType);
        contractRoomType.setNumberOfRooms(contractRoomTypeRequest.getNumberOfRooms());
        contractRoomType.setNumberOfPersons(contractRoomTypeRequest.getNumberOfPersons());
        contractRoomType.setTerms(contractRoomTypeRequest.getTerms());
        contractRoomType.setRentPrice(contractRoomTypeRequest.getRentPrice());
        contractRoomType.setDinnerPrice(contractRoomTypeRequest.getDinnerPrice());
        contractRoomType.setLunchPrice(contractRoomTypeRequest.getLunchPrice());
        contractRoomType.setBreakfastPrice(contractRoomTypeRequest.getBreakfastPrice());
        contractRoomType.setContractId(contract);
        contractRoomTypeRepository.save(contractRoomType);
        /////////////////////////////////////////////////////////////////////////////////////////////////
        //And in the end we have to update TypeAvailability table too
        boolean available_type = false;
        List<TypeAvailability> typeAvailabilities = new ArrayList<>();
        typeAvailabilities = typeAvailabilityRepository.findAllByTourOperatorOrderByDateAsc(contract.getTourOperator());
        //Iterate through type availability table
        for (int i = 0; i < typeAvailabilities.size(); i++) {
            if(typeAvailabilities.get(i).getDate().equals(contract.getCheckOutDate())){ //If rich the last day we break
                break;
            }
            if (typeAvailabilities.get(i).getDate().isEqual(contract.getCheckInDate()) || typeAvailabilities.get(i).getDate().isAfter(contract.getCheckInDate()) ) {
                //we have to add new room number
                if(roomTypeChanged) { // If user chose to change room type
                    if (typeAvailabilities.get(i).getType().equals(contractRoomType.getRoomType())) { //Go to current typeAvailability rows
                        typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() + contractRoomType.getNumberOfRooms()); //we have to add new room number
                        typeAvailabilityRepository.save(typeAvailabilities.get(i)); //and save it
                        available_type = true; //this means that there is availability records for new type that user want to update, so we don't have to create it later
                    }
                    if (typeAvailabilities.get(i).getType().getType() == oldRoomType) { //Go to previous (before update) typeAvailability rows
                        typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable() - oldContractRoomType.getNumberOfRooms()); //we have to decrease virtual availability because user chose to change room type
                        typeAvailabilityRepository.save(typeAvailabilities.get(i)); //after update we have to save it

                        if (typeAvailabilities.get(i).getVirtualAvailable() <= 0 ) { //If there is no virtual availability then remove whole typeAvailability row
                            typeAvailabilityRepository.delete(typeAvailabilities.get(i));
                        }
                    }
                }
                else{ //If user didn't change room type
                    available_type = true;
                    if(typeAvailabilities.get(i).getType().equals(contractRoomType.getRoomType())){ //Go to current typeAvailability rows
                        if(sign == '+'){
                            typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable()+newVirtualAvailability); //we have to add new room number
                            typeAvailabilityRepository.save(typeAvailabilities.get(i)); //and save it
                        }
                        else {
                            typeAvailabilities.get(i).setVirtualAvailable(typeAvailabilities.get(i).getVirtualAvailable()-newVirtualAvailability); //we have to decrease virtual availability
                            typeAvailabilityRepository.save(typeAvailabilities.get(i)); //and save it
                        }
                    }
                }
            }
        }
        // if available_type remain false, means that there is no availability
        // for new type we want to update, so we have to create it!
        if (!available_type) {
            TypeAvailability typeAvailability;
            List<TypeAvailability> typeAvailabilityList = new ArrayList<>();
            TourOperator operator;
            operator = tourOperatorRepository.findByName("SELF"); //find rows with real availability
            typeAvailabilityList = typeAvailabilityRepository.findAllByTourOperatorOrderByDateAsc(operator);
            for (LocalDate date = contract.getCheckInDate(); date.isBefore(contract.getCheckOutDate()); date = date.plusDays(1)) {
                typeAvailability = new TypeAvailability();
                typeAvailability.setDate(date);
                typeAvailability.setVirtualAvailable(contractRoomTypeRequest.getNumberOfRooms());
                typeAvailability.setType(roomType);
                typeAvailability.setTourOperator(contract.getTourOperator());
                //Here we calculate real availability column (columns with operator = SELF)
                for (int i = 0; i < typeAvailabilityList.size(); i++) {
                    if(typeAvailabilityList.get(i).getDate().isEqual(typeAvailability.getDate())) {
                        if (typeAvailabilityList.get(i).getType().equals(roomType)) {
                            typeAvailability.setAvailable(typeAvailabilityList.get(i).getAvailable());
                            typeAvailabilityRepository.save(typeAvailability);
                            break;
                        }
                    }
                } // End of (For) real availability calculation
            }//End of (For) new typeAvailability rows creation
        }
        return ResponseEntity.ok(new MessageResponse("Contract Room Type updated successfully!"));
    }
}




