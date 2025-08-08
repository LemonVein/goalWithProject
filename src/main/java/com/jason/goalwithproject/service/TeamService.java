package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.custom.CharacterImage;
import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.quest.*;
import com.jason.goalwithproject.domain.team.Team;
import com.jason.goalwithproject.domain.team.TeamRepository;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.quest.QuestRecordDto;
import com.jason.goalwithproject.dto.quest.QuestResponseDto;
import com.jason.goalwithproject.dto.quest.QuestVerificationDto;
import com.jason.goalwithproject.dto.team.TeamAddRequestDto;
import com.jason.goalwithproject.dto.team.TeamResponseDto;
import com.jason.goalwithproject.dto.user.UserDto;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.nio.file.AccessDeniedException;
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
    private final QuestVerificationRepository questVerificationRepository;
    private final RecordImageRepository recordImageRepository;
    private final QuestRecordRepository questRecordRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserTeamRepository userTeamUserRepository;
    private final UserRepository userRepository;
    private final QuestRepository questRepository;
    private final JwtService jwtService;

    private final QuestService questService;

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
        Map<Integer, QuestResponseDto> teamQuestMap = quests.stream()
                .collect(Collectors.toMap(
                        q -> q.getTeam().getId(),
                        this::convertToQuestDto
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

    public Map<String, String> deleteTeam(String authorization, int teamId) throws AccessDeniedException {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Team targetTeam = teamRepository.findById(teamId);

        if (!targetTeam.getLeader().getId().equals(userId)) {
            throw new AccessDeniedException("팀 리더만 팀 삭제 요청을 할 수 있습니다.");
        }

        if (targetTeam == null) {
            return Map.of("status", "failure");
        } else {
            Quest quest = questRepository.findByTeam_Id(teamId).orElse(null);
            assert quest != null;
            try {
                questService.deleteQuestWithQuestId(quest.getId());
                teamRepository.delete(targetTeam);
            } catch (Exception e) {
                return Map.of("status", "failure");
            }
            return Map.of("status", "success");
        }
    }

    public Map<String, String> editTeam(String authorization, int teamId, TeamAddRequestDto teamAddRequestDto) throws AccessDeniedException {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Team targetTeam = teamRepository.findById(teamId);

        if (!targetTeam.getLeader().getId().equals(userId)) {
            throw new AccessDeniedException("팀 리더만 팀 수정 요청을 할 수 있습니다.");
        }

        if (targetTeam == null) {
            return Map.of("status", "failure");
        } else {
            targetTeam.setDescription(teamAddRequestDto.getDescription());
            targetTeam.setName(teamAddRequestDto.getName());
            targetTeam.setPublic(teamAddRequestDto.isPublic());

            teamRepository.save(targetTeam);
            return Map.of("status", "success");
        }
    }

    private UserDto convertToDto(User user) {
        if (user == null) return null;
        UserCharacter userCharacter = userCharacterRepository.findByUser_Id(user.getId());
        UserBadge userBadge = userBadgeRepository.findByUser_Id(user.getId());


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

    private QuestResponseDto convertToQuestDto(Quest quest) {
        if (quest == null) {
            return null;
        }

        List<QuestRecord> questRecords = questRecordRepository.findAllByQuest_Id(quest.getId());

        List<QuestRecordDto> questRecordDtos = questRecords.stream().map(record -> {
            List<RecordImage> images = recordImageRepository.findByQuestRecord_Id(record.getId());
            List<String> imageUrls = images.stream()
                    .map(RecordImage::getUrl)
                    .toList();
            return QuestRecordDto.fromEntity(record, imageUrls, record.getUser().getId());
        }).toList();

        List<QuestVerification> questVerifications = questVerificationRepository.findAllByQuest_IdAndUser_Id(quest.getId(), quest.getUser().getId());

        List<QuestVerificationDto> questVerificationDtos = questVerifications.stream()
                .map(QuestVerificationDto::fromEntity)
                .toList();

        return QuestResponseDto.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .isMain(quest.isMain())
                .startDate(quest.getStartDate())
                .endDate(quest.getEndDate())
                .procedure(quest.getQuestStatus())
                .verificationRequired(quest.isVerificationRequired())
                .verificationCount(quest.getVerificationCount())
                .requiredVerification(quest.getRequiredVerification())
                .records(questRecordDtos)
                .verifications(questVerificationDtos)
                .build();
    }
}
