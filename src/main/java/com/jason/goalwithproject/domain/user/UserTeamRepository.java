package com.jason.goalwithproject.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {
    List<UserTeam> findByUser_Id(Long userId);
    List<UserTeam> findByTeam_Id(int teamId);
    List<UserTeam> findByTeam_IdIn(List<Integer> teamIds);
}
