package com.jason.goalwithproject.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {
    UserCharacter findByUser_Id(Long id);

    UserCharacter findById(long id);

    Optional<UserCharacter> findByUser_IdAndIsEquippedTrue(Long userId);

    List<UserCharacter> findAllByUser_Id(Long userId);

    Page<UserCharacter> findAllByUser_Id(Long userId, Pageable pageable);
}
