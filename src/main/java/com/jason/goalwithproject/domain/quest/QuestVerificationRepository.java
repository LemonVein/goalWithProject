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
public interface QuestVerificationRepository extends JpaRepository<QuestVerification, Long>, QuestVerificationRepositoryCustom {
    List<QuestVerification> findAllByQuest_IdAndUser_Id(Long questId, Long userId);
    List<QuestVerification> findAllByQuestRecord_Id(Long questRecordId);
    List<QuestVerification> findAllByQuest_Id(Long questId);
    Optional<QuestVerification> findById(Long id);

    int countByQuest_Id(Long questId);

    void deleteByQuestRecord_Id(Long id);
    void deleteAllByQuestRecord_Id(Long id);

    // 대댓글이 아닌 댓글들만 불러오기
    List<QuestVerification> findByQuest_IdAndParentIsNullOrderByCreatedAtAsc(Long questRecordId);

    // 대댓글이 아닌 댓글들만 불러오기(팀일때 레코드)
    List<QuestVerification> findByQuestRecord_IdAndParentIsNullOrderByCreatedAtAsc(Long questRecordId);

    // 대댓글 계수 세기
    long countByParent_Id(Long parentId);

    // 부모가 가진 대댓글들 가져오기
    List<QuestVerification> findByParent_IdOrderByCreatedAtAsc(Long parentId);

    boolean existsByUser_IdAndQuest_Id(Long userId, Long questId);
}
