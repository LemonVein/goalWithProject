package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.custom.CharacterImage;
import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.quest.Quest;
import com.jason.goalwithproject.domain.quest.QuestRepository;
import com.jason.goalwithproject.domain.team.Team;
import com.jason.goalwithproject.domain.team.TeamRepository;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.team.TeamAddRequestDto;
import com.jason.goalwithproject.dto.team.TeamResponseDto;
import com.jason.goalwithproject.dto.user.UserDto;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterImageRepository characterImageRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserTeamRepository userTeamUserRepository;
    private final UserRepository userRepository;
    private final QuestRepository questRepository;
    private final JwtService jwtService;

    public Map<String, Integer> createTeam(String authorization, TeamAddRequestDto teamAddRequestDto) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        User leader = userRepository.findById(userId).orElse(null);



        Team team = Team.builder()
                .description(teamAddRequestDto.getDescription())
                .name(teamAddRequestDto.getName())
                .leader(leader)
                .isPublic(teamAddRequestDto.isPublic())
                .build();

        Team target = teamRepository.save(team);

        UserTeam userTeam = UserTeam.builder()
                .team(target)
                .user(leader)
                .build();

        userTeamUserRepository.save(userTeam);

        return Map.of("teamId", target.getId());
    }

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

        return userTeams.stream()
                .map(ut -> {
                    Team team = ut.getTeam();
                    return TeamResponseDto.builder()
                            .id(team.getId())
                            .name(team.getName())
                            .description(team.getDescription())
                            .leader(convertToDto(team.getLeader()))
                            .members(teamMembersMap.getOrDefault(team.getId(), List.of())
                                    .stream()
                                    .map(this::convertToDto)
                                    .toList())
                            .teamQuest(teamQuestMap.get(team.getId()))
                            .createdAt(team.getCreatedAt())
                            .build();
                })
                .toList();
    }
    private UserDto convertToDto(User user) {
        if (user == null) return null;
        UserCharacter userCharacter = userCharacterRepository.findById(user.getId()).orElse(null);
        UserBadge userBadge = userBadgeRepository.findByUser_Id(user.getId());

        assert userCharacter != null;
        return UserDto.builder()
                .id(user.getId())
                .nickname(user.getNickName())
                .email(user.getEmail())
                .level(user.getLevel())
                .actionPoints(user.getActionPoint())
                .userType(user.getUserType().getName())
                .character(userCharacter.getCharacterImage().getImage())
                .badge(userBadge.getBadge().getImageUrl())
                .build();
    }
}
