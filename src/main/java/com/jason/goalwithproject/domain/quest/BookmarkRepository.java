package com.jason.goalwithproject.domain.quest;

import com.jason.goalwithproject.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Book;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findById(Long id);

    Optional<Bookmark> findByQuestAndUser(Quest quest, User user);

    Optional<Bookmark> findByUser_IdAndQuest_Id(Long userId, Long questId);

    List<Bookmark> findAllByUser(User user);

    boolean existsByUser_IdAndQuest_Id(Long userId, Long questId);

    @Query("SELECT DISTINCT b.quest FROM Bookmark b WHERE b.user.id = :userId")
    Page<Quest> findDistinctQuestsBookmarkedByUser(@Param("userId") Long userId, Pageable pageable);
}
