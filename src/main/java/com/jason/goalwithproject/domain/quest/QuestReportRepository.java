package com.jason.goalwithproject.domain.quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestReportRepository extends JpaRepository<QuestReport, Long> {
    boolean existsByReporter_IdAndQuest_Id(Long reporterId, Long questId);
}
