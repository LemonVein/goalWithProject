package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.S3Uploader;
import com.jason.goalwithproject.domain.quest.*;
import com.jason.goalwithproject.domain.user.User;
import com.jason.goalwithproject.domain.user.UserRepository;
import com.jason.goalwithproject.dto.quest.*;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final JwtService jwtService;
    private final QuestRepository questRepository;
    private final QuestRecordRepository questRecordRepository;
    private final QuestVerificationRepository questVerificationRepository;
    private final RecordImageRepository recordImageRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    public QuestDto findQuests(String authentication) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authentication);
        Long userId = Long.valueOf(claims.get("userId").toString());

        // 사용자의 모든 퀘스트 조회
        List<Quest> userQuests = questRepository.findAllByUser_Id(userId);

        // 각 퀘스트마다 관련 데이터 조합
        List<QuestResponseDto> questResponseDtos = userQuests.stream().map(quest -> {
            Long questId = quest.getId();

            List<QuestRecord> questRecords = questRecordRepository.findAllByQuest_Id(questId);
            List<QuestVerification> questVerifications = questVerificationRepository.findAllByQuest_IdAndUser_Id(questId, userId);

            List<QuestVerificationDto> questVerificationDtos = questVerifications.stream()
                    .map(QuestVerificationDto::fromEntity)
                    .toList();

            List<QuestRecordDto> questRecordDtos = questRecords.stream().map(record -> {
                List<RecordImage> images = recordImageRepository.findByQuestRecord_Id(record.getId());
                List<String> imageUrls = images.stream()
                        .map(RecordImage::getUrl)
                        .toList();
                return QuestRecordDto.fromEntity(record, imageUrls, userId);
            }).toList();

            QuestResponseDto questResponseDto = new QuestResponseDto();
            questResponseDto.setTitle(quest.getTitle());
            questResponseDto.setId(questId);
            questResponseDto.setDescription(quest.getDescription());
            questResponseDto.setMain(quest.isMain());
            questResponseDto.setStartDate(quest.getStartDate());
            questResponseDto.setEndDate(quest.getEndDate());
            questResponseDto.setProcedure(quest.getQuestStatus());
            questResponseDto.setVerificationRequired(quest.isVerificationRequired());
            questResponseDto.setVerificationCount(quest.getVerificationCount());
            questResponseDto.setVerifications(questVerificationDtos);
            questResponseDto.setRecords(questRecordDtos);
            questResponseDto.setRequiredVerification(quest.getRequiredVerification());

            return questResponseDto;
        }).toList();

        QuestDto questDto = new QuestDto();
        questDto.setQuests(questResponseDtos);

        return questDto;
    }

    public Map<String, String> createQuest(@RequestHeader("Authorization") String authorization, QuestAddRequest questAddRequest) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        Quest newQuest = new Quest();
        newQuest.setTitle(questAddRequest.getTitle());
        newQuest.setDescription(questAddRequest.getDescription());
        newQuest.setStartDate(questAddRequest.getStartDate());
        newQuest.setEndDate(questAddRequest.getEndDate());
        newQuest.setDescription(questAddRequest.getDescription());
        newQuest.setQuestStatus(QuestStatus.PROGRESS);
        newQuest.setVerificationRequired(questAddRequest.isVerificationRequired());
        newQuest.setMain(questAddRequest.isMain());
        newQuest.setRequiredVerification(questAddRequest.getRequiredVerification());
        newQuest.setUser(user);
        newQuest.setTeam(null);

        try {
            questRepository.save(newQuest);
        } catch (Exception e) {
            return Map.of("status", "failure");
        }

        return Map.of("status", "success");

    }

    public Map<ReactionType, Integer> countReactions(Long questId) {
        Quest targetQuest = questRepository.findById(questId)
                .orElseThrow( () -> new IllegalArgumentException("해당 퀘스트를 찾을 수 없습니다."));

        List<Reaction> reactions = reactionRepository.findAllByQuest_Id(questId);
        Map<ReactionType, Integer> reactionMap = new HashMap<>();
        for (Reaction reaction : reactions) {
            ReactionType type = ReactionType.valueOf(reaction.getReactionType().toUpperCase());
            reactionMap.put(type, reactionMap.getOrDefault(type, 0) + 1);
        }

        for (ReactionType type : ReactionType.values()) {
            reactionMap.putIfAbsent(type, 0);
        }

        return reactionMap;
    }

    public Map<String, String> deleteQuestWithQuestId(Long questId) {
        Optional<Quest> target = questRepository.findById(questId);

        if (!target.isPresent()) {
            return Map.of("status", "failure");
        } else {
            questRepository.delete(target.get());
            return Map.of("status", "success");
        }
    }

    public List<QuestRecordDto> getQuestRecordsWithQuestId(String authorization, Long questId) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        List<QuestRecord> questRecords = questRecordRepository.findAllByQuest_Id(questId);
        List<QuestRecordDto> questRecordDtos = new ArrayList<>();
        for (QuestRecord questRecord : questRecords) {
            List<RecordImage> images = recordImageRepository.findByQuestRecord_Id(questRecord.getId());
            List<String> imageUrls = images.stream()
                    .map(RecordImage::getUrl)
                    .toList();

            QuestRecordDto dto = new QuestRecordDto(questRecord.getId(),
                    questRecord.getDate(),
                    questRecord.getText(),
                    imageUrls,
                    questRecord.getQuest().getId(),
                    questRecord.getCreatedAt(),
                    userId);
            questRecordDtos.add(dto);
        }

        return questRecordDtos;
    }

    @Transactional
    public Map<String, String> addQuestRecord(String authorization, Long questId, String text, List<MultipartFile> images) throws IOException {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        QuestRecord newQuestRecord = new QuestRecord();
        newQuestRecord.setText(text);
        newQuestRecord.setDate(LocalDateTime.now());
        newQuestRecord.setQuest(questRepository.findById(questId).get());
        questRecordRepository.save(newQuestRecord);

        // 여기에 이미지들을 S3Uploader를 통해 업로드 하고 url 들을 리턴해주기
        for (MultipartFile image : images) {
            String targetUrl = s3Uploader.upload(image, "record-image");
            try {
                RecordImage recordImage = new RecordImage();
                recordImage.setUrl(targetUrl);
                recordImage.setQuestRecord(newQuestRecord);
                recordImageRepository.save(recordImage);

            } catch (Exception e) {
                return Map.of("status", "failure");
            }
        }
        return Map.of("status", "success");
    }

    // 수정 필요 함 아직 작성안함.
    public QuestResponseDto updateQuest(String authorization, Long questId, QuestAddRequest questAddRequest) {
        return new QuestResponseDto();
    }

//    public Page<QuestVerifyResponseDto> getQuestVerifyWithPaging(@RequestParam(defaultValue = "0") int page) {
//        Pageable pageable = PageRequest.of(page, 8, Sort.by("startDate").descending());
//        Page<Quest> quests = questRepository.findAllByVerificationRequiredTrueAndQuestStatus_Verify(pageable);
//
//        return quests.map(quest -> {
//            // 사용자 정보
//            User user = quest.getUser(); // 퀘스트 작성자
//            QuestUserDto userDto = new QuestUserDto();
//            userDto.setUserId(user.getId());
//            userDto.setNickname(user.getNickName());
//            userDto.setLevel(user.getLevel());
//            userDto.setActionPoints(user.getActionPoint());
//            userDto.setUserType(user.getUserType().getName());
//            userDto.setAvatar(user.getAvatar());
//            userDto.setBadge(user.getBadge());
//
//            // 기록 정보
//            List<QuestRecord> records = questRecordRepository.findAllByQuest_Id(quest.getId());
//            List<QuestRecordDto> recordDtos = records.stream().map(record -> {
//                List<String> imageUrls = recordImageRepository.findByQuestRecord_Id(record.getId())
//                        .stream()
//                        .map(RecordImage::getUrl)
//                        .toList();
//                return QuestRecordDto.fromEntity(record, imageUrls, record.getUser().getId());
//            }).toList();
//
//            // 인증 정보
//            List<QuestVerification> verifications = questVerificationRepository.findAllByQuest_Id(quest.getId());
//            List<QuestVerificationDto> verificationDtos = verifications.stream()
//                    .map(QuestVerificationDto::fromEntity)
//                    .toList();
//
//            // 최종 DTO 조립
//            QuestVerifyResponseDto dto = new QuestVerifyResponseDto();
//            dto.setId(quest.getId());
//            dto.setTitle(quest.getTitle());
//            dto.setDescription(quest.getDescription());
//            dto.setMain(quest.isMain());
//            dto.setStartDate(quest.getStartDate());
//            dto.setEndDate(quest.getEndDate());
//            dto.setProcedure(quest.getQuestStatus());
//            dto.setVerificationRequired(quest.isVerificationRequired());
//            dto.setVerificationCount(verifications.size());
//            dto.setRequiredVerification(quest.getVerificationCount());
//            dto.setRecords(recordDtos);
//            dto.setVerifications(verificationDtos);
//            dto.setUser(userDto);
//
//            return dto;
//    }
}
