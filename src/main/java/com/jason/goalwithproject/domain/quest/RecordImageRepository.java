package com.jason.goalwithproject.domain.quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordImageRepository extends JpaRepository<RecordImage, Long> {
    List<RecordImage> findByQuestRecord_Id(Long id);
}
