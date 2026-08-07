package com.crm.repository;

import com.crm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findFirstByUserEmail(String userEmail);
    default Optional<User> findByUserEmail(String userEmail) {
        return findFirstByUserEmail(userEmail);
    }
    Optional<User> findFirstByUsername(String username);
    default Optional<User> findByUsername(String username) {
        return findFirstByUsername(username);
    }
    boolean existsByUserEmail(String userEmail);
    List<User> findByRole(String role);
}
