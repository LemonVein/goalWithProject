package com.jason.goalwithproject.domain.quest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestRepository extends JpaRepository<Quest, Long> {
    Optional<Quest> findByUser_IdAndIsMainTrue(Long id);
    List<Quest> findAllByUser_Id(Long id);
    Optional<Quest> findById(Long id);
    Optional<Quest> findByTeam_Id(int teamId);
    List<Quest> findByTeam_IdIn(List<Integer> teamIds);
    Page<Quest> findAllByVerificationRequiredTrueAndQuestStatus(QuestStatus questStatus, Pageable pageable);

}
