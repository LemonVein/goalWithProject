package com.jason.goalwithproject.domain.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, TeamRepositoryCustom {
    Team findById(int id);

    Page<Team> findByNameContaining(String name, Pageable pageable);
}
