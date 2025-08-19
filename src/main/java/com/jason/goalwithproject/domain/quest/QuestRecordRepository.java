package com.jason.goalwithproject.domain.quest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestRecordRepository extends JpaRepository<QuestRecord, Long> {
    Optional<QuestRecord> findByQuest_Id(Long id);
    Page<QuestRecord> findAllByQuest_Id(Long id, Pageable pageable);
    List<QuestRecord> findAllByQuest_Id(Long id);
}
