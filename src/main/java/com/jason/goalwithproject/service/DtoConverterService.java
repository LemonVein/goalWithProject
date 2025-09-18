package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.quest.*;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.peer.RequesterDto;
import com.jason.goalwithproject.dto.quest.QuestRecordDto;
import com.jason.goalwithproject.dto.quest.QuestResponseDto;
import com.jason.goalwithproject.dto.quest.QuestVerificationDto;
import com.jason.goalwithproject.dto.quest.QuestVerifyResponseDto;
import com.jason.goalwithproject.dto.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DtoConverterService {
    private final UserCharacterRepository userCharacterRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final QuestRepository questRepository;
    private final UserRepository userRepository;
    private final QuestVerificationRepository questVerificationRepository;
    private final QuestRecordRepository questRecordRepository;
    private final RecordImageRepository recordImageRepository;

    // User 객체를 UserDto 로 변환해주는 메서드
    public UserDto convertToDto(User user) {
        if (user == null) return null;
        Optional<UserCharacter> userCharacter = userCharacterRepository.findByUser_IdAndIsEquippedTrue(user.getId());
        UserBadge userBadge = userBadgeRepository.findByUser_Id(user.getId());


        return UserDto.builder()
                .id(user.getId())
                .nickname(user.getNickName())
                .email(user.getEmail())
                .level(user.getLevel())
                .actionPoints(user.getActionPoint())
                .userType(user.getUserType().getName())
                .character(userCharacter.get().getCharacterImage().getImage())
                .badge(userBadge.getBadge().getImageUrl())
                .exp(user.getExp())
                .build();
    }

    public RequesterDto convertToRequesterDto(User user) {
        if (user == null) return null;

        Optional<UserCharacter> userCharacter = userCharacterRepository.findByUser_IdAndIsEquippedTrue(user.getId());

        return RequesterDto.builder()
                .id(user.getId())
                .name(user.getNickName())
                .level(user.getLevel())
                .userType(user.getUserType().getName())
                .character(userCharacter.get().getCharacterImage().getImage())
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
    public QuestVerifyResponseDto convertToQuestVerifyResponseDto(Quest quest) {
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

        List<QuestVerification> questVerifications = questVerificationRepository.findAllByQuest_IdAndUser_Id(quest.getId(), quest.getUser().getId());

        List<QuestVerificationDto> questVerificationDtos = questVerifications.stream()
                .map(QuestVerificationDto::fromEntity)
                .toList();

        return QuestVerifyResponseDto.builder()
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
                .user(userDto)
                .build();
    }
}
