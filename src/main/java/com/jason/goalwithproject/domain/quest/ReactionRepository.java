package com.jason.goalwithproject.domain.quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findAllByQuest_Id(Long questId);

    List<Reaction> findAllByQuest_IdAndUser_Id(Long questId, Long userId);

    List<Reaction> findAllByQuestRecord_IdAndUser_Id(Long recordId, Long userId);

    Optional<Reaction> findByQuest_IdAndUser_IdAndReactionTypeIgnoreCase(Long questId, Long userId, String reactionType);

    Optional<Reaction> findByQuestRecord_IdAndUser_IdAndReactionTypeIgnoreCase(Long recordId, Long userId, String reactionType);
}
