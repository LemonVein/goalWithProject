package com.jason.goalwithproject.domain.quest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<Quest> findAllByVerificationRequiredTrueAndQuestStatus(QuestStatus questStatus);
    List<Quest> findAllByUser_IdAndTeamIsNull(Long id);
    Optional<Quest> findByTeam_IdAndQuestStatus(int teamId, QuestStatus questStatus);

    @Query("SELECT q FROM Quest q WHERE " +
            "q.questStatus = :status AND q.verificationRequired = true AND " +
            "(LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(q.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Quest> findVerifiableQuestsByKeyword(
            @Param("status") QuestStatus status,
            @Param("keyword") String keyword);


}
