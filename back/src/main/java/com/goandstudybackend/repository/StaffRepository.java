package com.goandstudybackend.repository;

import com.goandstudybackend.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StaffRepository extends MongoRepository<Staff, String> {

    Optional<Staff> findByEmail(String email);

    Optional<Staff> findByEmailIgnoreCase(String email);

    Page<Staff> findByIsActive(boolean isActive, Pageable pageable);

    long countByIsActive(boolean isActive);
}
