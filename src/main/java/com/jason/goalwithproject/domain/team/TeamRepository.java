package com.jason.goalwithproject.domain.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Team findById(int id);

    Page<Team> findByNameContaining(String name, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Team t LEFT JOIN Quest q ON q.team = t " +
            "WHERE t.name LIKE %:keyword% OR q.title LIKE %:keyword%")
    Page<Team> findByNameOrQuestTitle(@Param("keyword") String keyword, Pageable pageable);
}
