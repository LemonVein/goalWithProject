package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.quest.*;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.quest.QuestRecordDto;
import com.jason.goalwithproject.dto.quest.QuestResponseDto;
import com.jason.goalwithproject.dto.quest.QuestVerificationDto;
import com.jason.goalwithproject.dto.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DtoConverterService {
    private final UserCharacterRepository userCharacterRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final QuestRepository questRepository;
    private final QuestVerificationRepository questVerificationRepository;
    private final QuestRecordRepository questRecordRepository;
    private final RecordImageRepository recordImageRepository;

    public UserDto convertToDto(User user) {
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
                .exp(user.getExp())
                .build();
    }

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
}
