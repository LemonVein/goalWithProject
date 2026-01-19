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
public interface QuestRepository extends JpaRepository<Quest, Long>, QuestRepositoryCustom {
    Optional<Quest> findById(Long id);
    Optional<Quest> findByTeam_Id(int teamId);
    List<Quest> findByTeam_IdIn(List<Integer> teamIds);
    List<Quest> findAllByVerificationRequiredTrueAndQuestStatus(QuestStatus questStatus);
    List<Quest> findAllByUser_IdAndTeamIsNull(Long id);
    Optional<Quest> findByUser_IdAndIsMainTrueAndTeamIsNull(Long id);
    Optional<Quest> findByUser_IdAndIsMainTrueAndTeamIsNullAndQuestStatus(Long id, QuestStatus questStatus);
    Optional<Quest> findByTeam_IdAndQuestStatus(int teamId, QuestStatus questStatus);

//    @Query("SELECT q FROM Quest q WHERE " +
//            "q.questStatus = :status AND q.verificationRequired = true AND " +
//            "(LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(q.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
//    List<Quest> findVerifiableQuestsByKeyword(
//            @Param("status") QuestStatus status,
//            @Param("keyword") String keyword);
//
//    @Query("SELECT q FROM Quest q WHERE " +
//            "q.user.id IN :peerIds AND " +
//            "q.questStatus = :status AND " +
//            "q.verificationRequired = true")
//    Page<Quest> findPeerQuestsForVerification(
//            @Param("peerIds") List<Long> peerIds,
//            @Param("status") QuestStatus status,
//            Pageable pageable);

    // 사용자의 퀘스트 수 반환 (업적용)
    long countByUser_Id(Long userId);

    // 특정 상태의 퀘스트 상태들 조회
    long countByUser_IdAndQuestStatus(Long userId, QuestStatus questStatus);


}
