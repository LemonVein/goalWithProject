package com.jason.goalwithproject.domain.custom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterImageRepository extends JpaRepository<CharacterImage, Integer> {
}
