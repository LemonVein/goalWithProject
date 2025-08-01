package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.quest.Quest;
import com.jason.goalwithproject.domain.quest.QuestRepository;
import com.jason.goalwithproject.domain.team.TeamRepository;
import com.jason.goalwithproject.domain.user.User;
import com.jason.goalwithproject.domain.user.UserTeam;
import com.jason.goalwithproject.domain.user.UserTeamRepository;
import com.jason.goalwithproject.dto.team.TeamResponseDto;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;
    private final QuestRepository questRepository;
    private final JwtService jwtService;

    public List<TeamResponseDto> findAllUserTeams(String authorization) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());
        List<TeamResponseDto> result = new ArrayList<>();

        List<UserTeam> userTeams = userTeamRepository.findByUser_Id(userId);
        for (UserTeam userTeam : userTeams) {
            TeamResponseDto teamResponseDto = new TeamResponseDto();
            teamResponseDto.setId(userTeam.getTeam().getId());
            teamResponseDto.setName(userTeam.getTeam().getName());
            teamResponseDto.setDescription(userTeam.getTeam().getDescription());
            teamResponseDto.setLeader(userTeam.getTeam().getLeader());

            List<UserTeam> userTeams1 = userTeamRepository.findByTeam_Id(userTeam.getTeam().getId());
            List<User> users = userTeams1.stream().map(u -> u.getUser()).collect(Collectors.toList());
            teamResponseDto.setMembers(users);

            Optional<Quest> teamQuest = questRepository.findByTeam_Id(userTeam.getTeam().getId());
            teamResponseDto.setTeamQuest(teamQuest.isPresent() ? teamQuest.get() : null);

            result.add(teamResponseDto);

        }
        return result;
    }
}
