package com.crm.repository;

import com.crm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserEmail(String userEmail);
    Optional<User> findByUsername(String username);
    boolean existsByUserEmail(String userEmail);
    List<User> findByRole(String role);
}
