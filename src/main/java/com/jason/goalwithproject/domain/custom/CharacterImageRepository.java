package com.jason.goalwithproject.domain.custom;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterImageRepository extends JpaRepository<CharacterImage, Integer> {
    @Cacheable(value = "characterImages", key = "#id", unless = "#result == null")
    CharacterImage findById(int id);
}
