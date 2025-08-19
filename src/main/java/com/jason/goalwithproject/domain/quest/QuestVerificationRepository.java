package com.jason.goalwithproject.domain.quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestVerificationRepository extends JpaRepository<QuestVerification, Long> {
    List<QuestVerification> findAllByQuest_IdAndUser_Id(Long questId, Long userId);
    List<QuestVerification> findAllByQuestRecord_Id(Long questRecordId);
    List<QuestVerification> findAllByQuest_Id(Long questId);

    void deleteByQuestRecord_Id(Long id);
    void deleteAllByQuestRecord_Id(Long id);
}
