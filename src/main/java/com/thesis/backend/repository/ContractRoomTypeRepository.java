package com.thesis.backend.repository;

import com.thesis.backend.models.Contract;
import com.thesis.backend.models.ContractRoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ContractRoomTypeRepository extends JpaRepository<ContractRoomType,Long> {

    List<ContractRoomType> findByContractId(Contract currentContract);

    List<ContractRoomType> findAllByContractId(Contract contract);

}
