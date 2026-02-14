package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.quest.*;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.peer.RequesterDto;
import com.jason.goalwithproject.dto.quest.*;
import com.jason.goalwithproject.dto.user.UserDto;
import com.jason.goalwithproject.dto.user.UserInfoForAdmin;
import com.jason.goalwithproject.dto.user.UserInformationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DtoConverterService {
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterImageRepository characterImageRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final QuestRepository questRepository;
    private final UserRepository userRepository;
    private final QuestVerificationRepository questVerificationRepository;
    private final QuestRecordRepository questRecordRepository;
    private final RecordImageRepository recordImageRepository;
    private final BookmarkRepository bookmarkRepository;

    // User 객체를 UserDto 로 변환해주는 메서드
    public UserDto convertToDto(User user) {
        if (user == null) return null;
        Optional<UserCharacter> userCharacter = userCharacterRepository.findByUser_IdAndIsEquippedTrue(user.getId());
        UserBadge userBadge = userBadgeRepository.findByUser_IdAndEquippedTrue(user.getId()).get();


        return UserDto.builder()
                .id(user.getId())
                .nickname(user.getNickName())
                .email(user.getEmail())
                .level(user.getLevel())
                .actionPoints(user.getActionPoint())
                .userType(user.getUserType().getName())
                .character(userCharacter.get().getCharacterImage().getImage())
                .badge(userBadge.getBadge().getName())
                .exp(user.getExp())
                .build();
    }

    public RequesterDto convertToRequesterDto(User user) {
        if (user == null) return null;

        String characterUrl = userCharacterRepository.findByUser_IdAndIsEquippedTrue(user.getId())
                .map(uc -> uc.getCharacterImage().getImage())
                .orElse("default.png");

        return RequesterDto.builder()
                .id(user.getId())
                .name(user.getNickName())
                .level(user.getLevel())
                .userType(user.getUserType().getName())
                .character(characterUrl)
                .build();

    }

    // Quest 객체를 QuestResponseDto 로 변환해주는 메서드
    public QuestResponseDto convertToQuestDto(Quest quest) {
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

    // quest를 인증받을 퀘스트로 변환하는 메서드
    public UserQuestVerifyResponseDto convertToQuestVerifyResponseDto(Quest quest) {
        if (quest == null) {
            return null;
        }

        User user = quest.getUser();
        UserDto userDto = convertToDto(user);

        List<QuestRecord> questRecords = questRecordRepository.findAllByQuest_Id(quest.getId());

        List<QuestRecordDto> questRecordDtos = questRecords.stream().map(record -> {
            List<RecordImage> images = recordImageRepository.findByQuestRecord_Id(record.getId());
            List<String> imageUrls = images.stream()
                    .map(RecordImage::getUrl)
                    .toList();
            return QuestRecordDto.fromEntity(record, imageUrls, record.getUser().getId());
        }).toList();

//        List<QuestVerification> questVerifications = questVerificationRepository.findAllByQuest_Id(quest.getId());
//
//        List<RecordCommentDto> questVerificationDtos = questVerifications.stream()
//                .map(questVerification -> {
//                    Optional<UserCharacter> uc = userCharacterRepository.findByUser_IdAndIsEquippedTrue(questVerification.getUser().getId());
//                    return RecordCommentDto.from(questVerification, uc.get().getCharacterImage().getImage());
//                })
//                .toList();

        return UserQuestVerifyResponseDto.builder()
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
                .user(userDto)
                .build();
    }

    public UserQuestVerifyResponseDto convertToQuestVerifyResponseDtoPersonal(Quest quest, User targetUser) {
        if (quest == null) {
            return null;
        }

        User user = quest.getUser();
        UserDto userDto = convertToDto(user);

        List<QuestRecord> questRecords = questRecordRepository.findAllByQuest_Id(quest.getId());

        List<QuestRecordDto> questRecordDtos = questRecords.stream().map(record -> {
            List<RecordImage> images = recordImageRepository.findByQuestRecord_Id(record.getId());
            List<String> imageUrls = images.stream()
                    .map(RecordImage::getUrl)
                    .toList();
            return QuestRecordDto.fromEntity(record, imageUrls, record.getUser().getId());
        }).toList();

//        List<QuestVerification> questVerifications = questVerificationRepository.findAllByQuest_Id(quest.getId());
//
//        List<RecordCommentDto> questVerificationDtos = questVerifications.stream()
//                .map(questVerification -> {
//                    Optional<UserCharacter> uc = userCharacterRepository.findByUser_IdAndIsEquippedTrue(questVerification.getUser().getId());
//                    return RecordCommentDto.from(questVerification, uc.get().getCharacterImage().getImage());
//                })
//                .toList();
        boolean bookmarked = bookmarkRepository.existsByUser_IdAndQuest_Id(targetUser.getId(), quest.getId());
        boolean verified = questVerificationRepository.existsByUser_IdAndQuest_Id(targetUser.getId(), quest.getId());

        return UserQuestVerifyResponseDto.builder()
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
                .user(userDto)
                .bookmarked(bookmarked)
                .verified(verified)
                .build();
    }

    public UserInformationDto convertToUserInformationDto(User user) {
        if (user == null) {
            return null;
        }

        String characterImageUrl = userCharacterRepository.findByUser_IdAndIsEquippedTrue(user.getId())
                .map(uc -> uc.getCharacterImage().getImage())
                .orElse(null);

        String badgeImageUrl = userBadgeRepository.findByUser_IdAndEquippedTrue(user.getId())
                .get().getBadge().getName();

        SingleQuestDto mainQuestDto = questRepository.findByUser_IdAndIsMainTrueAndTeamIsNullAndQuestStatus(user.getId(), QuestStatus.PROGRESS)
                .map(SingleQuestDto::from)
                .orElse(null);

        return UserInformationDto.builder()
                .id(user.getId())
                .nickname(user.getNickName())
                .email(user.getEmail())
                .level(user.getLevel())
                .actionPoints(user.getActionPoint())
                .exp(user.getExp())
                .userType(user.getUserType().getName())
                .character(characterImageUrl)
                .badge(badgeImageUrl)
                .mainQuest(mainQuestDto)
                .build();
    }

    public QuestVerifyDto convertToQuestVerifyDto(Quest quest) {
        List<QuestRecord> questRecords = questRecordRepository.findAllByQuest_Id(quest.getId());

        List<QuestRecordDto> questRecordDtos = questRecords.stream().map(record -> {
            List<RecordImage> images = recordImageRepository.findByQuestRecord_Id(record.getId());
            List<String> imageUrls = images.stream()
                    .map(RecordImage::getUrl)
                    .toList();
            return QuestRecordDto.fromEntity(record, imageUrls, record.getUser().getId());
        }).toList();

        List<QuestVerification> questVerifications = questVerificationRepository.findAllByQuest_Id(quest.getId());

        List<RecordCommentDto> questVerificationDtos = questVerifications.stream()
                .map(questVerification -> {
                    Optional<UserCharacter> uc;

                    if (questVerification.getUser() == null) {
                        uc = userCharacterRepository.findById(1);
                    } else {
                        uc = userCharacterRepository.findByUser_IdAndIsEquippedTrue(questVerification.getUser().getId());
                    }

                    return RecordCommentDto.from(questVerification, uc.get().getCharacterImage().getImage(), 0L);
                })
                .toList();

        return QuestVerifyDto.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .isMain(quest.isMain())
                .startDate(quest.getStartDate())
                .endDate(quest.getEndDate())
                .procedure(quest.getQuestStatus())

                // 인증 관련 필드
                .verificationRequired(quest.isVerificationRequired())
                .requiredVerification(quest.getRequiredVerification())
                .verificationCount(quest.getVerificationCount())

                .records(questRecordDtos)
                .verifications(questVerificationDtos)

                // 유저 정보 변환
                .user(convertToDto(quest.getUser()))
                .build();
    }

    // 어드민 용 정보
    public UserInfoForAdmin convertToAdminDto(User user) {
        return UserInfoForAdmin.builder()
                .id(user.getId())
                .nickname(user.getNickName())
                .email(user.getEmail())
                .character(getEquippedCharacterName(user.getId()))
                .level(user.getLevel())
                .actionPoints(user.getActionPoint())
                .badge(getEquippedBadgeName(user.getId()))
                .userType(user.getUserType() != null ? user.getUserType().getName() : "none")
                .createdAt(user.getCreatedAt())
                .userStatus(user.getUserStatus().name())
                .build();
    }

    // 정말 단일 퀘스트의 작은 정보만 원할 때 사용
    public QuestDto convertToSingleQuestDto(Quest quest) {
        return QuestDto.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .isMain(quest.isMain())
                .questStatus(quest.getQuestStatus())
                .startDate(quest.getStartDate())
                .endDate(quest.getEndDate())
                .user(convertToDto(quest.getUser()))
                .build();
    }

    private String getEquippedCharacterName(Long userId) {
        return userCharacterRepository.findByUser_IdAndIsEquippedTrue(userId)
                .map(userCharacter -> userCharacter.getCharacterImage().getName())
                .orElse("없음");
    }

    private String getEquippedBadgeName(Long userId) {
        return userBadgeRepository.findByUser_IdAndEquippedTrue(userId)
                .map(userBadge -> userBadge.getBadge().getName()) // 있으면 이름 꺼내기
                .orElse("없음");
    }

}
