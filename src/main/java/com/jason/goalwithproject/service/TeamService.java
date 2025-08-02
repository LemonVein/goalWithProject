package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.quest.Quest;
import com.jason.goalwithproject.domain.quest.QuestRepository;
import com.jason.goalwithproject.domain.team.Team;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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

        List<UserTeam> userTeams = userTeamRepository.findByUser_Id(userId);
        List<Integer> teamIds = userTeams.stream()
                .map(ut -> ut.getTeam().getId())
                .toList();

        List<UserTeam> allUserTeamsForTeams = userTeamRepository.findByTeam_IdIn(teamIds);
        Map<Integer, List<User>> teamMembersMap = allUserTeamsForTeams.stream()
                .collect(Collectors.groupingBy(
                        ut -> ut.getTeam().getId(),
                        Collectors.mapping(UserTeam::getUser, Collectors.toList())
                ));

        List<Quest> quests = questRepository.findByTeam_IdIn(teamIds);
        Map<Integer, Quest> teamQuestMap = quests.stream()
                .collect(Collectors.toMap(
                        q -> q.getTeam().getId(),
                        Function.identity()
                ));

        // 빌더 패턴으로 코드 리팩토링
            List<TeamResponseDto> result = userTeams.stream()
                    .map(ut -> {
                        Team team = ut.getTeam();
                        return TeamResponseDto.builder()
                                .id(team.getId())
                                .name(team.getName())
                                .description(team.getDescription())
                                .leader(team.getLeader())
                                .members(teamMembersMap.getOrDefault(team.getId(), List.of()))
                                .teamQuest(teamQuestMap.get(team.getId()))
                                .build();
                    })
                    .toList();
        return result;
    }
}
