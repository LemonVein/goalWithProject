package com.jason.goalwithproject.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickName(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByUserType(UserType userType);

    Page<User> findByNickNameContaining(String name, Pageable pageable);
}
