package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.S3Uploader;
import com.jason.goalwithproject.domain.quest.*;
import com.jason.goalwithproject.domain.team.Team;
import com.jason.goalwithproject.domain.team.TeamRepository;
import com.jason.goalwithproject.domain.user.User;
import com.jason.goalwithproject.domain.user.UserCharacter;
import com.jason.goalwithproject.domain.user.UserCharacterRepository;
import com.jason.goalwithproject.domain.user.UserRepository;
import com.jason.goalwithproject.dto.quest.*;
import com.jason.goalwithproject.dto.user.UserDto;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final JwtService jwtService;
    private final QuestRepository questRepository;
    private final TeamRepository teamRepository;
    private final QuestRecordRepository questRecordRepository;
    private final QuestVerificationRepository questVerificationRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final RecordImageRepository recordImageRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;
    private final DtoConverterService dtoConverterService;

    public QuestListDto findQuests(String authentication) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authentication);
        Long userId = Long.valueOf(claims.get("userId").toString());

        // 사용자의 모든 퀘스트 조회
        List<Quest> userQuests = questRepository.findAllByUser_IdAndTeamIsNull(userId);

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

            return QuestResponseDto.builder()
                    .id(questId)
                    .title(quest.getTitle())
                    .description(quest.getDescription())
                    .isMain(quest.isMain())
                    .startDate(quest.getStartDate())
                    .endDate(quest.getEndDate())
                    .procedure(quest.getQuestStatus())
                    .verificationRequired(quest.isVerificationRequired())
                    .verificationCount(quest.getVerificationCount())
                    .requiredVerification(quest.getRequiredVerification())
                    .verifications(questVerificationDtos)
                    .records(questRecordDtos)
                    .build();
        }).toList();

        QuestListDto questListDto = new QuestListDto();
        questListDto.setQuests(questResponseDtos);

        return questListDto;
    }

    public Map<String, String> createQuest(@RequestHeader("Authorization") String authorization, QuestAddRequest questAddRequest) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        Quest newQuest = new Quest();

        // 팀이 있다면 team_id 없다면 -1 로 처리
        if (questAddRequest.getTeamId() == -1) {
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
        } else {
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

            Team team = teamRepository.findById(questAddRequest.getTeamId());
            newQuest.setTeam(team);
        }


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

    @Transactional
    public Map<String, String> deleteQuestWithQuestId(Long questId) {
        Optional<Quest> target = questRepository.findById(questId);

        if (!target.isPresent()) {
            return Map.of("status", "failure");
        } else {
            List<QuestRecord> questRecords = questRecordRepository.findAllByQuest_Id(questId);

            for (QuestRecord record : questRecords) {
                List<RecordImage> images = recordImageRepository.findByQuestRecord_Id(record.getId());

                // 이미지 S3에서 삭제
                for (RecordImage image : images) {
                    s3Uploader.deleteFile(image.getUrl());
                }

                // 이미지 DB에서 삭제
                recordImageRepository.deleteAll(images);
            }
            questRecordRepository.deleteAll(questRecords);

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


    // 팀 포스트를 위한 팀 아이디를 제공 받음
    public Map<String, String> addQuestTeamRecord(String authorization, int teamId, String text, List<MultipartFile> images) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Optional<Quest> targetQuest = questRepository.findByTeam_IdAndQuestStatus(teamId, QuestStatus.PROGRESS);
        if (!targetQuest.isPresent()) {
            return Map.of("status", "failure");
        }

        QuestRecord newQuestRecord = new QuestRecord();
        newQuestRecord.setText(text);
        newQuestRecord.setDate(LocalDateTime.now());
        newQuestRecord.setQuest(targetQuest.get());
        newQuestRecord.setUser(userRepository.findById(userId).get());
        questRecordRepository.save(newQuestRecord);

        // 여기에 이미지들을 S3Uploader를 통해 업로드 하고 url 들을 리턴해주기
        if (images != null && !images.isEmpty()) {
            List<RecordImage> recordImages = new ArrayList<>();
            try {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        String targetUrl = s3Uploader.upload(image, "record-image");
                        RecordImage recordImage = new RecordImage();
                        recordImage.setUrl(targetUrl);
                        recordImage.setQuestRecord(newQuestRecord);
                        recordImages.add(recordImage);
                    }
                }
                recordImageRepository.saveAll(recordImages);

            } catch (IOException e) {
                return Map.of("status", "failure");
            }
        }

        return Map.of("status", "success");
    }

    @Transactional
    public Map<String, String> addQuestRecord(String authorization, Long questId, String text, List<MultipartFile> images) throws IOException {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        QuestRecord newQuestRecord = new QuestRecord();
        newQuestRecord.setText(text);
        newQuestRecord.setDate(LocalDateTime.now());
        newQuestRecord.setQuest(questRepository.findById(questId).get());
        newQuestRecord.setUser(userRepository.findById(userId).get());
        questRecordRepository.save(newQuestRecord);

        // 여기에 이미지들을 S3Uploader를 통해 업로드 하고 url 들을 리턴해주기
        if (images != null && !images.isEmpty()) {
            List<RecordImage> recordImages = new ArrayList<>();
            try {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        String targetUrl = s3Uploader.upload(image, "record-image");
                        RecordImage recordImage = new RecordImage();
                        recordImage.setUrl(targetUrl);
                        recordImage.setQuestRecord(newQuestRecord);
                        recordImages.add(recordImage);
                    }
                }
                recordImageRepository.saveAll(recordImages);

            } catch (IOException e) {
                return Map.of("status", "failure");
            }
        }

        return Map.of("status", "success");
    }

    @Transactional
    public Page<TeamQuestRecordDto> getTeamQuestRecords(String authorization, int teamId, Pageable pageable) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Quest teamQuest = questRepository.findByTeam_IdAndQuestStatus(teamId, QuestStatus.PROGRESS)
                .orElseThrow(() -> new EntityNotFoundException("Team quest not found for teamId: " + teamId));

        Page<QuestRecord> recordPage = questRecordRepository.findAllByQuest_Id(teamQuest.getId(), pageable);

        return recordPage.map(record -> {
            // 해당 기록에 달린 모든 인증(Verification)들을 조회합니다.
            List<QuestVerification> verifications = questVerificationRepository.findAllByQuestRecord_Id(record.getId());
            List<RecordCommentDto> verificationDtos = verifications.stream()
                    .map(verification -> {
                        User user = verification.getUser();
                        String imageUrl = null;

                        if (user != null) {
                            UserCharacter userCharacter = userCharacterRepository.findByUser_Id(user.getId());
                            if (userCharacter != null && userCharacter.getCharacterImage() != null) {
                                imageUrl = userCharacter.getCharacterImage().getImage();
                            }
                        }

                        return RecordCommentDto.from(verification, imageUrl);
                    })
                    .collect(Collectors.toList());

            List<RecordImage> imageRecords = recordImageRepository.findByQuestRecord_Id(record.getId());
            List<String> images = new ArrayList<>();
            for (RecordImage imageRecord : imageRecords) {
                images.add(imageRecord.getUrl());
            }

            UserDto userDto = dtoConverterService.convertToDto(record.getUser());

            return TeamQuestRecordDto.builder()
                    .id(record.getId())
                    .text(record.getText())
                    .createdAt(record.getCreatedAt())
                    .user(userDto)
                    .verifications(verificationDtos)
                    .images(images)
                    .build();
        });
    }

    // 코멘트 작성
    public Map<String, String> addRecordComment(String authorization, Long recordId, String text) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Optional<QuestRecord> targetRecord = questRecordRepository.findById(recordId);

        Quest target = questRepository.findById(targetRecord.get().getQuest().getId()).orElse(null);

        Optional<User> targetUser = userRepository.findById(userId);

        questVerificationRepository.save(QuestVerification.builder().questRecord(targetRecord.get())
                .user(targetUser.get())
                .quest(target)
                .comment(text)
                .createdAt(LocalDateTime.now())
                .build());

        return Map.of("status", "success");

    }

    // 댓글 수정
    public Map<String, String> updateComment(String authorization, Long commentId, String text) throws AccessDeniedException {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Optional<QuestVerification> targetQuestVerification = questVerificationRepository.findById(commentId);

        if (!targetQuestVerification.get().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("이 코멘트를 수정할 권한이 없습니다.");
        }

        targetQuestVerification.get().setComment(text);

        questVerificationRepository.save(targetQuestVerification.get());

        return Map.of("status", "success");

    }

    // 댓글 삭제
    public Map<String, String> deleteComment(String authorization, Long commentId) throws AccessDeniedException {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Optional<QuestVerification> targetQuestVerification = questVerificationRepository.findById(commentId);

        if (!targetQuestVerification.get().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("이 코멘트를 삭제할 권한이 없습니다.");
        }

        questVerificationRepository.delete(targetQuestVerification.get());
        return Map.of("status", "success");
    }

    @Transactional
    public Map<String, String> updateRecord(String Authorization, Long recordId, RecordUpdateDto dto, List<MultipartFile> newImages) throws IOException {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(Authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Optional<QuestRecord> targetRecord = questRecordRepository.findById(recordId);

        if (!targetRecord.get().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("이 기록을 수정할 권한이 없습니다.");
        }

        targetRecord.get().setText(dto.getText());

        List<RecordImage> currentImages = recordImageRepository.findByQuestRecord_Id(targetRecord.get().getId());
        List<RecordImage> imagesToDelete = new ArrayList<>();

        for (RecordImage dbImage : currentImages) {
            // DB의 이미지가 클라이언트가 보낸 "유지할 이미지 목록"에 포함되어 있지 않다면
            if (!dto.getExistingImages().contains(dbImage.getUrl())) {
                imagesToDelete.add(dbImage);
            }
        }

        for (RecordImage image : imagesToDelete) {
            s3Uploader.deleteFile(image.getUrl());
            recordImageRepository.delete(image);
        }

        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile imageFile : newImages) {
                // S3에 새 이미지 업로드
                String imageUrl = s3Uploader.upload(imageFile, "record-image");

                // DB에 새 이미지 정보 저장
                RecordImage newRecordImage = new RecordImage();
                newRecordImage.setUrl(imageUrl);
                newRecordImage.setQuestRecord(targetRecord.get());
                recordImageRepository.save(newRecordImage);
            }
        }
        return Map.of("status", "success");


    }

    // 레코드 삭제 메서드 (사용자 인증 정보, 레코드 아이디 필요)
    @Transactional
    public Map<String, String> deleteRecord(String authorization, Long recordId) throws AccessDeniedException {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Optional<QuestRecord> targetRecord = questRecordRepository.findById(recordId);


        if (!targetRecord.get().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("이 기록을 삭제할 권한이 없습니다.");
        }

        List<RecordImage> images = recordImageRepository.findByQuestRecord_Id(recordId);
        for (RecordImage image : images) {
            s3Uploader.deleteFile(image.getUrl());
        }

        questVerificationRepository.deleteAllByQuestRecord_Id(targetRecord.get().getId());
        recordImageRepository.deleteAllByQuestRecord_Id(targetRecord.get().getId());
        questRecordRepository.delete(targetRecord.get());
        return Map.of("status", "success");

    }



    // 수정 필요 함 아직 작성안함.
    public Map<String, String> updateQuest(String authorization, Long questId, QuestAddRequest questAddRequest) {
        Optional<Quest> target = questRepository.findById(questId);

        if (target.isPresent()) {
            Quest quest = target.get();
            quest.updateFrom(questAddRequest);
            questRepository.save(quest);
            return Map.of("status", "success");
        }

        return Map.of("status", "failure");
    }

    @Transactional
    public Map<String, String> completeQuest(String authorization, Long questId) throws AccessDeniedException {
        Long userId = jwtService.UserIdFromToken(authorization);

        Quest targetQuest = questRepository.findById(questId)
                .orElseThrow(() -> new EntityNotFoundException("해당 퀘스트를 찾을 수 없습니다. ID: " + questId));

        Optional<Quest> target = questRepository.findById(questId);
        if (!target.get().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("이 퀘스트를 완료시킬 권한이 없습니다.");
        }

        if (target.get().isVerificationRequired()) {
            if (target.get().getVerificationCount() >= target.get().getRequiredVerification()) {
                target.get().setQuestStatus(QuestStatus.COMPLETE);
                target.get().getUser().setExp(target.get().getUser().getExp() + 50);
                target.get().getUser().setActionPoint(target.get().getUser().getActionPoint() + 50);
            } else {
                target.get().setQuestStatus(QuestStatus.VERIFY);
            }
        } else {
            target.get().setQuestStatus(QuestStatus.COMPLETE);
            target.get().getUser().setExp(target.get().getUser().getExp() + 50);
            target.get().getUser().setActionPoint(target.get().getUser().getActionPoint() + 50);
        }

        questRepository.save(target.get());
        return Map.of("status", "success");

    }

    @Transactional
    public void verifyQuest(String authorization, Long questId) {
        Long userId = jwtService.UserIdFromToken(authorization);

        Quest targetQuest = questRepository.findById(questId)
                .orElseThrow(() -> new EntityNotFoundException("해당 퀘스트를 찾을 수 없습니다. ID: " + questId));

        User verifyingUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        targetQuest.setVerificationCount(targetQuest.getVerificationCount() + 1);

        verifyingUser.setExp(verifyingUser.getExp() + 5);
    }

//    // 인증할 만 한 퀘스트들 불러오기. 근데 추천 알고리즘 문제로 잠시 보류
//    public Page<QuestVerifyResponseDto> getVerifyQuestWithPaging(String authorization, Pageable pageable) {
//        Long userId = jwtService.UserIdFromToken(authorization);
//    }
}
