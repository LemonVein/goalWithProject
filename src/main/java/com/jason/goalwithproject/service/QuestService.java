package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.S3Uploader;
import com.jason.goalwithproject.domain.custom.Badge;
import com.jason.goalwithproject.domain.custom.BadgeRepository;
import com.jason.goalwithproject.domain.custom.CharacterImage;
import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.quest.*;
import com.jason.goalwithproject.domain.team.Team;
import com.jason.goalwithproject.domain.team.TeamRepository;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.common.ReportRequestDto;
import com.jason.goalwithproject.dto.quest.*;
import com.jason.goalwithproject.dto.user.UserDto;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestService {

    private final JwtService jwtService;
    private final QuestRepository questRepository;
    private final TeamRepository teamRepository;
    private final QuestRecordRepository questRecordRepository;
    private final QuestVerificationRepository questVerificationRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final RecordImageRepository recordImageRepository;
    private final ReactionRepository reactionRepository;
    private final PeerShipRepository peerShipRepository;
    private final UserRepository userRepository;
    private final CharacterImageRepository characterImageRepository;
    private final S3Uploader s3Uploader;
    private final DtoConverterService dtoConverterService;
    private final BookmarkRepository bookmarkRepository;
    private final UserService userService;
    private final UserBadgeRepository userBadgeRepository;
    private final BadgeRepository badgeRepository;
    private final QuestReportRepository questReportRepository;
    private final QuestVerificationReportRepository questVerificationReportRepository;

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

    public Map<String, String> createQuest(@RequestHeader("Authorization") String authorization, QuestAddRequest questAddRequest) throws Exception {
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

            checkFirstQuestAchievement(user); // 1번 도전과제 추가
        } catch (Exception e) {
            throw new Exception();
        }

        return Map.of("status", "success");

    }

    public ReactionCountDto countReactions(String authorization, Long questId) {
        Quest targetQuest = questRepository.findById(questId)
                .orElseThrow( () -> new IllegalArgumentException("해당 퀘스트를 찾을 수 없습니다."));

        Long userId = jwtService.UserIdFromToken(authorization);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        List<Reaction> allReactions = reactionRepository.findAllByQuest_Id(questId);
        Map<ReactionType, Integer> counts = new EnumMap<>(ReactionType.class);
        // 모든 ReactionType에 대해 카운트를 0으로 초기화
        for (ReactionType type : ReactionType.values()) {
            counts.put(type, 0);
        }
        // 조회된 리액션으로 카운트 업데이트
        allReactions.forEach(reaction -> {
            try {
                ReactionType type = ReactionType.valueOf(reaction.getReactionType().toUpperCase());
                counts.put(type, counts.get(type) + 1);
            } catch (IllegalArgumentException e) {
                // 잘못된 reactionType 데이터는 무시
            }
        });

        // 자신이 남긴 리액션 정보를 조회
        Map<ReactionType, Boolean> myReactionMap = new EnumMap<>(ReactionType.class);
        // 모든 ReactionType에 대해 false로 초기화
        for (ReactionType type : ReactionType.values()) {
            myReactionMap.put(type, false);
        }

        // 로그인 상태일 경우에만 내 리액션 정보를 조회하여 업데이트
        if (userId != null) {
            List<Reaction> myReactions = reactionRepository.findAllByQuest_IdAndUser_Id(questId, userId);
            myReactions.forEach(reaction -> {
                try {
                    ReactionType type = ReactionType.valueOf(reaction.getReactionType().toUpperCase());
                    myReactionMap.put(type, true); // 내가 남긴 리액션은 true로 변경
                } catch (IllegalArgumentException e) {
                    // 잘못된 reactionType 데이터는 무시
                }
            });
        }

        return ReactionCountDto.builder()
                .support(counts.get(ReactionType.SUPPORT))
                .amazing(counts.get(ReactionType.AMAZING))
                .together(counts.get(ReactionType.TOGETHER))
                .perfect(counts.get(ReactionType.PERFECT))
                .myReaction(myReactionMap)
                .build();
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

        User user = userRepository.findById(userId).get();

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

        checkConsecutiveRecordAchievement(user);

        return Map.of("status", "success");
    }

    @Transactional
    public Map<String, String> addQuestRecord(String authorization, Long questId, String text, List<MultipartFile> images) throws IOException {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        User user = userRepository.findById(userId).get();

        QuestRecord newQuestRecord = new QuestRecord();
        newQuestRecord.setText(text);
        newQuestRecord.setDate(LocalDateTime.now());
        newQuestRecord.setQuest(questRepository.findById(questId).get());
        newQuestRecord.setUser(userRepository.findById(userId).get());
        questRecordRepository.save(newQuestRecord);

        // 여기에 이미지들을 S3Uploader를 통해 업로드 하고 url 들을 리턴해주기
        if (images != null && !images.isEmpty()) {
            List<RecordImage> recordImages = images.parallelStream()
                    .filter(image -> !image.isEmpty())
                    .map(image -> {
                        try {
                            // S3 업로드 (제미나이의 도움, 병렬로 실행됨)
                            String targetUrl = s3Uploader.upload(image, "record-image");

                            RecordImage recordImage = new RecordImage();
                            recordImage.setUrl(targetUrl);
                            recordImage.setQuestRecord(newQuestRecord);
                            return recordImage;
                        } catch (IOException e) {
                            throw new RuntimeException("이미지 업로드 중 오류 발생", e);
                        }
                    })
                    .collect(Collectors.toList());

            recordImageRepository.saveAll(recordImages); // DB에 한 번에 저장
        }
        checkConsecutiveRecordAchievement(user);

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
            // 해당 기록에 달린 모든 인증(즉 댓글)들을 조회
            List<QuestVerification> verifications = questVerificationRepository.findByQuestRecord_IdAndParentIsNullOrderByCreatedAtAsc(record.getId());
            List<RecordCommentDto> verificationDtos = verifications.stream()
                    .map(verification -> {
                        User user = verification.getUser();
                        String imageUrl = null;

                        if (user != null) {
                            UserCharacter userCharacter = userCharacterRepository.findByUser_IdAndIsEquippedTrue(user.getId()).get();
                            if (userCharacter != null && userCharacter.getCharacterImage() != null) {
                                imageUrl = userCharacter.getCharacterImage().getImage();
                            }
                        }
                        Long replyCount = questVerificationRepository.countByParent_Id(verification.getId());

                        return RecordCommentDto.from(verification, imageUrl, replyCount);
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

        User verifier = userRepository.findById(userId).get();

        Optional<QuestRecord> targetRecord = questRecordRepository.findById(recordId);

        Quest target = questRepository.findById(targetRecord.get().getQuest().getId()).orElse(null);

        Optional<User> targetUser = userRepository.findById(userId);

        questVerificationRepository.save(QuestVerification.builder().questRecord(targetRecord.get())
                .user(targetUser.get())
                .quest(target)
                .comment(text)
                .createdAt(LocalDateTime.now())
                .build());

        if (!targetRecord.get().getUser().getId().equals(userId)) {
            checkVerificationOnOthersQuestsAchievement(verifier);
        }

        // 4번 도전과제 스마일 피코 체크 (첫 피인증시)
        User recordUser = targetRecord.get().getUser();
        checkFirstVerificationReceivedAchievement(recordUser);

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
            // DB의 이미지가 클라이언트가 보낸 유지할 이미지 목록에 포함되어 있지 않다면
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

    // 퀘스트를 신고하는 메서드
    @Transactional
    public void reportQuest(String authorization, Long questId, ReportRequestDto reportRequestDto) throws AccessDeniedException {
        Long userId = jwtService.UserIdFromToken(authorization);
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new EntityNotFoundException("게시물을 찾을 수 없습니다."));

        if (quest.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 게시물은 신고할 수 없습니다.");
        }

        if (questReportRepository.existsByReporter_IdAndQuest_Id(userId, questId)) {
            throw new IllegalStateException("이미 신고한 게시물입니다.");
        }

        QuestReport report = QuestReport.builder()
                .reporter(reporter)
                .quest(quest)
                .reason(reportRequestDto.getReason())
                .build();

        questReportRepository.save(report);
    }

    // 인증 댓글 신고
    @Transactional
    public void reportVerification(String authorization, Long verificationId, ReportRequestDto reportRequestDto) throws AccessDeniedException {
        Long userId = jwtService.UserIdFromToken(authorization);
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        QuestVerification verification = questVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));

        if (verification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 댓글은 신고할 수 없습니다.");
        }

        if (questVerificationReportRepository.existsByReporter_IdAndVerification_Id(userId, verificationId)) {
            throw new IllegalStateException("이미 신고한 댓글입니다.");
        }

        QuestVerificationReport report = QuestVerificationReport.builder()
                .reporter(reporter)
                .verification(verification)
                .reason(reportRequestDto.getReason())
                .build();

        questVerificationReportRepository.save(report);
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
    public void completeQuest(String authorization, Long questId) throws AccessDeniedException {
        Long userId = jwtService.UserIdFromToken(authorization);

        Quest targetQuest = questRepository.findById(questId)
                .orElseThrow(() -> new EntityNotFoundException("해당 퀘스트를 찾을 수 없습니다. ID: " + questId));
        User questOwner = targetQuest.getUser();

        Optional<Quest> target = questRepository.findById(questId);
        if (!target.get().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("이 퀘스트를 완료시킬 권한이 없습니다.");
        }

        if (target.get().getTeam() != null) {
            if (!target.get().getTeam().getLeader().equals(userRepository.findById(userId).get())) {
                throw new AccessDeniedException("팀 리더만 팀 퀘스트를 완료시킬 수 있습니다.");
            }
        }

        boolean isSuccess = false;
        if (target.get().isVerificationRequired()) {
            if (target.get().getQuestStatus() == QuestStatus.VERIFY) {
                if (target.get().getVerificationCount() < target.get().getRequiredVerification()) {
                    throw new AccessDeniedException("인증 수가 모자랍니다.");
                }
                // action point 계산
                int actionScore = 0;
                actionScore += 10;

                // exp score 계산 방식
                int score = 0;
                Duration duration = Duration.between(target.get().getStartDate(), target.get().getEndDate());
                long durationDays = duration.toDays();
                score += (int) (durationDays * 3);
                score += (questRecordRepository.countByQuest_Id(target.get().getId()) * 5);
                score += 10;

                if (target.get().isMain()) {
                    score *= 2;
                    actionScore += 20;
                }

                target.get().setQuestStatus(QuestStatus.COMPLETE);
                isSuccess = true;

                // 새로운 레벨업 메서드로 대체
                // target.get().getUser().setExp(target.get().getUser().getExp() + score);
                userService.addExpAndProcessLevelUp(target.get().getUser(), score);
                target.get().getUser().setActionPoint(target.get().getUser().getActionPoint() + actionScore);

            }
            else if (target.get().getVerificationCount() >= target.get().getRequiredVerification()) {
                target.get().setQuestStatus(QuestStatus.COMPLETE);

                // action point 계산
                int actionScore = 0;
                actionScore += 10;


                // exp score 계산 방식
                int score = 0;
                Duration duration = Duration.between(target.get().getStartDate(), target.get().getEndDate());
                long durationDays = duration.toDays();
                score += (int) (durationDays * 3);
                score += (questRecordRepository.countByQuest_Id(target.get().getId()) * 5);
                score += 10;

                if (target.get().isMain()) {
                    score *= 2;
                    actionScore += 20;
                }

                // 최소 인증수를 넘긴 사람들을 위한 계산식
                score += (target.get().getRequiredVerification() * 5) + (target.get().getVerificationCount() - target.get().getRequiredVerification());
                actionScore += (target.get().getRequiredVerification() * 2) + (target.get().getVerificationCount() - target.get().getRequiredVerification());

                // 새로운 레벨업 방법으로 대체
                // target.get().getUser().setExp(target.get().getUser().getExp() + score);
                userService.addExpAndProcessLevelUp(target.get().getUser(), score);
                target.get().getUser().setActionPoint(target.get().getUser().getActionPoint() + actionScore);
            } else {
                target.get().setQuestStatus(QuestStatus.VERIFY);
            }
        } else {
            // action point 계산
            int actionScore = 0;
            actionScore += 10;

            // exp score 계산 방식
            int score = 0;
            Duration duration = Duration.between(target.get().getStartDate(), target.get().getEndDate());
            long durationDays = duration.toDays();
            score += (int) (durationDays * 3);
            score += (questRecordRepository.countByQuest_Id(target.get().getId()) * 5);
            score += 10;

            if (target.get().isMain()) {
                score *= 2;
                actionScore += 20;
            }

            target.get().setQuestStatus(QuestStatus.COMPLETE);
            isSuccess = true;
            // 새로운 레벨업 방법으로 대체
            // target.get().getUser().setExp(target.get().getUser().getExp() + score);
            userService.addExpAndProcessLevelUp(target.get().getUser(), score);
            target.get().getUser().setActionPoint(target.get().getUser().getActionPoint() + actionScore);
        }

        if (isSuccess) {
            checkFirstQuestCompletionAchievement(questOwner);
        }

        questRepository.save(target.get());

    }

    @Transactional
    public void verifyQuest(String authorization, Long questId, CommentDto commentDto) throws AccessDeniedException {
        Long userId = jwtService.UserIdFromToken(authorization);

        Quest targetQuest = questRepository.findById(questId)
                .orElseThrow(() -> new EntityNotFoundException("해당 퀘스트를 찾을 수 없습니다. ID: " + questId));

        User verifyingUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        QuestVerification questVerification = new QuestVerification().builder()
                .questRecord(null)
                .createdAt(LocalDateTime.now())
                .quest(targetQuest)
                .comment(commentDto.getComment())
                .user(verifyingUser)
                .build();

        questVerificationRepository.save(questVerification);

        targetQuest.setVerificationCount(targetQuest.getVerificationCount() + 1);

        // 새로운 레벨업 방식, 경험치 지급 방법으로 대체
        // verifyingUser.setExp(verifyingUser.getExp() + 10);
        userService.addExpAndProcessLevelUp(verifyingUser, 10);

        // 인정드림 뱃지 체크
        if (!targetQuest.getUser().getId().equals(userId)) {
            checkVerificationOnOthersQuestsAchievement(verifyingUser);
        }

        // 4번 도전과제 스마일 피코 체크 (첫 피인증시)
        User questOwner = targetQuest.getUser();
        checkFirstVerificationReceivedAchievement(questOwner);

        // 시작 피코 체크
        checkFirstVerificationOnOthersQuestAchievement(verifyingUser);
    }

    // 인증 상태인 추천 게시물들 불러오기
    @Transactional(readOnly = true)
    public Page<UserQuestVerifyResponseDto> getRecommendedQuestsForVerification(String authorization, String search, Pageable pageable) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // ✅ [수정] 검색어가 적용된 후보군 리스트를 DB에서 가져옵니다.
        // (search가 null이면 전체, 있으면 필터링된 결과가 옵니다)
        List<Quest> candidates = questRepository.findCandidatesForRecommendation(QuestStatus.VERIFY, search);

        // 각 퀘스트의 '추천 점수'를 계산하고, 퀘스트와 점수를 함께 저장
        List<QuestWithScore> scoredQuests = candidates.stream()
                // 본인이 작성한 퀘스트는 제외
                .filter(quest -> !quest.getUser().getId().equals(currentUserId))
                .map(quest -> {
                    double score = calculateRecommendationScore(currentUser, quest.getUser());
                    return new QuestWithScore(quest, score);
                })
                .collect(Collectors.toList());

        // 추천 점수가 높은 순서대로 정렬
        scoredQuests.sort(Comparator.comparingDouble(QuestWithScore::getScore).reversed());

        // 메모리 페이징 처리 (기존 로직 유지)
        int start = (int) pageable.getOffset();

        // 페이지 범위를 벗어나면 빈 페이지 반환
        if (start >= scoredQuests.size()) {
            return Page.empty(pageable);
        }

        int end = Math.min((start + pageable.getPageSize()), scoredQuests.size());

        List<Quest> pagedResult = scoredQuests.subList(start, end).stream()
                .map(QuestWithScore::getQuest)
                .collect(Collectors.toList());

        Page<Quest> questPage = new PageImpl<>(pagedResult, pageable, scoredQuests.size());

        return questPage.map(quest ->
                dtoConverterService.convertToQuestVerifyResponseDtoPersonal(quest, currentUser));
    }

    @Transactional
    public void editVerification(String authorization, Long verificationId, CommentDto commentDto) throws AccessDeniedException {
        Long userId = jwtService.UserIdFromToken(authorization);
        Optional<QuestVerification> questVerification = questVerificationRepository.findById(verificationId);

        if (questVerification.isEmpty()) {
            throw new IllegalArgumentException("해당 댓글이 존재하지 않습니다.");
        }

        if (!Objects.equals(questVerification.get().getUser().getId(), userId)) {
            throw new AccessDeniedException("댓글을 수정할 권한이 없습니다.");
        }

        questVerification.get().setComment(commentDto.getComment());
        questVerificationRepository.save(questVerification.get());
    }

    // 인증 삭제 메서드
    @Transactional
    public void deleteVerification(String authorization, Long verificationId) throws AccessDeniedException {
        Long userId = jwtService.UserIdFromToken(authorization);
        Optional<QuestVerification> questVerification = questVerificationRepository.findById(verificationId);

        if (questVerification.isEmpty()) {
            throw new IllegalArgumentException("해당 댓글이 존재하지 않습니다.");
        }

        if (!Objects.equals(questVerification.get().getUser().getId(), userId)) {
            throw new AccessDeniedException("댓글을 삭제할 권한이 없습니다.");
        }

        questVerificationRepository.deleteById(verificationId);
    }

    // 키워드로 추천 퀘스트 검색하기
    public Page<UserQuestVerifyResponseDto> searchRecommendQuestsForVerification(String authorization, String keyword, Pageable pageable) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 키워드에 맞는 인증 퀘스트 후보군을 모두 가져옴
        List<Quest> candidates = questRepository.findVerifiableQuestsByKeyword(QuestStatus.VERIFY, keyword);

        List<QuestWithScore> scoredQuests = candidates.stream()
                // 본인이 작성한 퀘스트는 제외
                .filter(quest -> !quest.getUser().getId().equals(currentUserId))
                .map(quest -> {
                    // 이전에 만든 점수 계산 헬퍼 메서드를 재사용합니다.
                    double score = calculateRecommendationScore(currentUser, quest.getUser());
                    return new QuestWithScore(quest, score);
                })
                .collect(Collectors.toList());

        scoredQuests.sort(Comparator.comparingDouble(QuestWithScore::getScore).reversed());

        // 수동 페이지네이션
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), scoredQuests.size());

        // subList는 end 인덱스가 리스트 크기를 초과하면 에러가 나므로 방어 코드 추가
        if (start >= scoredQuests.size()) {
            return Page.empty(pageable);
        }

        List<Quest> pagedResult = scoredQuests.subList(start, end).stream()
                .map(QuestWithScore::getQuest)
                .collect(Collectors.toList());

        Page<Quest> questPage = new PageImpl<>(pagedResult, pageable, scoredQuests.size());

        return questPage.map(quest ->
                dtoConverterService.convertToQuestVerifyResponseDtoPersonal(quest, currentUser)
        );


    }

    // 친구들의 인증 게시물들을 불러오기 (최신순)
    @Transactional(readOnly = true)
    public Page<UserQuestVerifyResponseDto> getPeerQuestsForVerification(String authorization, String search, Pageable pageable) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 동료들의 ID 목록 가져오기
        List<PeerShip> myPeers = peerShipRepository.findMyPeers(currentUserId, PeerStatus.ACCEPTED);
        List<Long> peerIds = myPeers.stream()
                .map(peerShip -> peerShip.getRequester().getId().equals(currentUserId)
                        ? peerShip.getAddressee().getId()
                        : peerShip.getRequester().getId())
                .collect(Collectors.toList());

        // 친구가 한 명도 없으면 빈 페이지 반환
        if (peerIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 친구들의 인증 퀘스트 가져오기 (search 파라미터 추가 전달)
        Page<Quest> questPage = questRepository.findPeerQuestsForVerification(
                peerIds,
                QuestStatus.VERIFY,
                search,
                pageable
        );

        return questPage.map(quest ->
                dtoConverterService.convertToQuestVerifyResponseDtoPersonal(quest, user));
    }

    @Transactional
    public void addReaction(String authorization, Long questId, ReactionRequestDto reactionRequestDto) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new EntityNotFoundException("퀘스트를 찾을 수 없습니다."));

        Reaction newReaction = new Reaction();
        String upperCaseString = reactionRequestDto.getReactionType().toUpperCase();
        ReactionType reactionType = ReactionType.valueOf(upperCaseString);

        List<Reaction> existingReactions = reactionRepository.findAllByQuest_IdAndUser_Id(questId, userId);

        boolean alreadyExists = existingReactions.stream()
                .anyMatch(reaction -> reaction.getReactionType().equalsIgnoreCase(reactionType.name()));

        if (alreadyExists) {
            throw new IllegalArgumentException("이미 '" + reactionType.name() + "' 리액션을 남겼습니다.");
        }

        newReaction.setQuest(quest);
        newReaction.setUser(user);
        newReaction.setReactionType(reactionType.name());

        checkReactionOnOthersQuestsAchievement(user);

        reactionRepository.save(newReaction);

    }

    // 리액션 삭제 (퀘스트 전용)
    @Transactional
    public void deleteReaction(String authorization, Long questId, String reactionType) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Quest quset = questRepository.findById(questId).orElseThrow(EntityNotFoundException::new);

        Reaction reactionToDelete = reactionRepository.findByQuest_IdAndUser_IdAndReactionTypeIgnoreCase(questId, userId, reactionType)
                .orElseThrow(() -> new EntityNotFoundException("해당 리액션을 찾을 수 없거나 삭제할 권한이 없습니다."));

        reactionRepository.delete(reactionToDelete);
    }

    @Transactional
    public void addReactionRecord(String authorization, Long recordId, ReactionRequestDto reactionRequestDto) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        QuestRecord questRecord = questRecordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("퀘스트를 찾을 수 없습니다."));

        Reaction newReaction = new Reaction();
        String upperCaseString = reactionRequestDto.getReactionType().toUpperCase();
        ReactionType reactionType = ReactionType.valueOf(upperCaseString);

        List<Reaction> existingReactions = reactionRepository.findAllByQuestRecord_IdAndUser_Id(recordId, userId);

        boolean alreadyExists = existingReactions.stream()
                .anyMatch(reaction -> reaction.getReactionType().equalsIgnoreCase(reactionType.name()));

        if (alreadyExists) {
            throw new IllegalArgumentException("이미 '" + reactionType.name() + "' 리액션을 남겼습니다.");
        }

        newReaction.setQuestRecord(questRecord);
        newReaction.setUser(user);
        newReaction.setReactionType(reactionType.name());

        reactionRepository.save(newReaction);

    }

    @Transactional
    public void deleteReactionRecord(String authorization, Long recordId, String reactionType) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        QuestRecord questRecord = questRecordRepository.findById(recordId).orElseThrow(EntityNotFoundException::new);

        Reaction reactionToDelete = reactionRepository.findByQuestRecord_IdAndUser_IdAndReactionTypeIgnoreCase(recordId, userId, reactionType)
                .orElseThrow(() -> new EntityNotFoundException("해당 리액션을 찾을 수 없거나 삭제할 권한이 없습니다."));

        reactionRepository.delete(reactionToDelete);
    }

    // 내가 댓글 달았던 퀘스트 목록 불러오기
    @Transactional(readOnly = true)
    public Page<UserQuestVerifyResponseDto> getMyVerify(@RequestHeader("Authorization") String authorization, Pageable pageable) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);

        Page<Quest> questPage = questVerificationRepository.findDistinctQuestsCommentedByUser(userId, pageable);

        return questPage.map(quest ->
                dtoConverterService.convertToQuestVerifyResponseDtoPersonal(quest, user));

    }

    // 내가 리액션한 퀘스트들 불러오기
    @Transactional(readOnly = true)
    public Page<UserQuestVerifyResponseDto> getMyReaction(@RequestHeader("Authorization") String authorization, Pageable pageable) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);

        Page<Quest> questPage = reactionRepository.findDistinctQuestsReactedByUser(userId, pageable);

        return questPage.map(quest ->
                dtoConverterService.convertToQuestVerifyResponseDtoPersonal(quest, user));
    }

    // 북마크 추가
    @Transactional
    public void addBookmark(String authorization, Long questId) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);

        Quest quest = questRepository.findById(questId).orElseThrow(EntityNotFoundException::new);

        if (bookmarkRepository.findByQuestAndUser(quest, user).isPresent()) {
            throw new IllegalArgumentException("이미 북마크한 퀘스트입니다");
        }

        Bookmark bookmark = Bookmark.builder()
                .quest(quest)
                .user(user)
                .date(LocalDateTime.now())
                .build();

        bookmarkRepository.save(bookmark);

    }

    // 북마크 한 퀘스트들 불러오기
    public Page<UserQuestVerifyResponseDto> getMyBookmarks(String authorization, Pageable pageable) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);

        Page<Quest> bookmarks = bookmarkRepository.findDistinctQuestsBookmarkedByUser(userId, pageable);

        return bookmarks.map(quest -> dtoConverterService.convertToQuestVerifyResponseDtoPersonal(quest, user));
    }

    public void cancelBookmark(String authorization, Long questId) {
        Long userId = jwtService.UserIdFromToken(authorization);

        Optional<Bookmark> bookmark = bookmarkRepository.findByUser_IdAndQuest_Id(userId, questId);

        if (bookmark.isPresent()) {
            bookmarkRepository.delete(bookmark.get());
        } else {
            throw new EntityNotFoundException("북마크가 존재하지 않습니다.");
        }
    }

    public QuestSummationDto getQuestSummation(String authorization, Long questId) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);

        Quest quest = questRepository.findById(questId).orElseThrow(EntityNotFoundException::new);

        List<QuestRecord> questRecords = questRecordRepository.findAllByQuest_Id(quest.getId());

        List<QuestRecordDto> questRecordDtos = questRecords.stream().map(record -> {
            List<RecordImage> images = recordImageRepository.findByQuestRecord_Id(record.getId());
            List<String> imageUrls = images.stream()
                    .map(RecordImage::getUrl)
                    .toList();
            return QuestRecordDto.fromEntity(record, imageUrls, record.getUser().getId());
        }).toList();

        List<QuestVerification> questVerifications = questVerificationRepository.findByQuest_IdAndParentIsNullOrderByCreatedAtAsc(quest.getId());

        List<RecordCommentDto> questVerificationDtos = questVerifications.stream()
                .map(questVerification -> {
                    Optional<UserCharacter> uc = userCharacterRepository.findByUser_IdAndIsEquippedTrue(questVerification.getUser().getId());

                    long replyCount = questVerificationRepository.countByParent_Id(questVerification.getId());

                    return RecordCommentDto.from(questVerification, uc.get().getCharacterImage().getImage(), replyCount);
                })
                .toList();

        return QuestSummationDto.builder()
                .id(quest.getId())
                .startDate(quest.getStartDate())
                .endDate(quest.getEndDate())
                .id(quest.getId())
                .title(quest.getTitle())
                .records(questRecordDtos)
                .verifications(questVerificationDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecordCommentDto> getReplies(String authorization, Long parentId) {
        List<QuestVerification> replies = questVerificationRepository.findByParent_IdOrderByCreatedAtAsc(parentId);

        // 2. DTO 변환
        return replies.stream()
                .map(reply -> {
                    String characterUrl = userCharacterRepository.findByUser_IdAndIsEquippedTrue(reply.getUser().getId())
                            .map(uc -> uc.getCharacterImage().getImage())
                            .orElse(null);

                    return RecordCommentDto.from(reply, characterUrl, 0L);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void addReply(String authorization, Long parentId, CommentDto commentDto) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        QuestVerification parentVerification = questVerificationRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("부모 댓글을 찾을 수 없습니다."));

        QuestVerification reply = new QuestVerification();
        reply.setUser(user);
        if (parentVerification.getQuest() != null) {
            reply.setQuest(parentVerification.getQuest());
        } else {
            reply.setQuestRecord(parentVerification.getQuestRecord());
        }
        reply.setParent(parentVerification);
        reply.setComment(commentDto.getComment());
        reply.setCreatedAt(LocalDateTime.now());

        questVerificationRepository.save(reply);
    }

    // 1번 도전과제 퀘스트 첫 생성
    private void checkFirstQuestAchievement(User user) {
        // '첫 걸음' 뱃지의 ID
        final int BADGE_ID_FIRST_QUEST = 2;

        boolean alreadyHasBadge = userBadgeRepository.existsByUser_IdAndBadge_Id(user.getId(), BADGE_ID_FIRST_QUEST);

        if (alreadyHasBadge) {
            return; // 이미 달성했으면 종료
        }

        // 현재까지의 총 퀘스트 횟수 조회
        long questCount = questRepository.countByUser_Id(user.getId());

        // 첫 퀘스트 확인
        if (questCount == 1) {
            Badge firstQuestBadge = badgeRepository.findById(BADGE_ID_FIRST_QUEST)
                    .orElse(null);

            if (firstQuestBadge != null) {
                UserBadge newUserBadge = new UserBadge();
                newUserBadge.setUser(user);
                newUserBadge.setBadge(firstQuestBadge);
                newUserBadge.setEquipped(false);
                userBadgeRepository.save(newUserBadge);
            }

//            // 경험치 보상 지급 (예: 50 EXP) 추후에 추가 예정
//            userService.addExpAndProcessLevelUp(user, 50);
        }
    }

    // 럭키7 사용자가 퀘스트 레코드 7일 연속 작성
    private void checkConsecutiveRecordAchievement(User user) {
        // '럭키7' 뱃지의 ID 3번
        final int BADGE_ID_7_DAY_STREAK = 3;
        final int STREAK_GOAL = 7; // 목표 연속일수

        // 뱃지를 이미 가지고 있는지 확인
        boolean alreadyHasBadge = userBadgeRepository.existsByUser_IdAndBadge_Id(user.getId(), BADGE_ID_7_DAY_STREAK);
        if (alreadyHasBadge) {
            return; // 이미 달성했으면 종료
        }

        // 오늘 날짜 및 7일 전 날짜 계산
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = today.minusDays(STREAK_GOAL - 1).atStartOfDay(); // 6일 전 00:00 (오늘 포함 7일)

        Set<LocalDate> uniqueDates = questRecordRepository.findDistinctRecordDatesByUserSince(user.getId(), startDate);

        // 7일 연속인지 확인
        int consecutiveDays = 0;
        for (int i = 0; i < STREAK_GOAL; i++) {
            if (uniqueDates.contains(today.minusDays(i))) {
                consecutiveDays++;
            } else {
                // 연속이 깨졌으므로 중단
                break;
            }
        }

        // 7일 연속 달성 시 보상 지급
        if (consecutiveDays == STREAK_GOAL) {
            Badge streakBadge = badgeRepository.findById(BADGE_ID_7_DAY_STREAK)
                    .orElse(null);

            if (streakBadge != null) {
                UserBadge newUserBadge = new UserBadge();
                newUserBadge.setUser(user);
                newUserBadge.setBadge(streakBadge);
                newUserBadge.setEquipped(false);
                userBadgeRepository.save(newUserBadge);

                log.info("ACHIEVEMENT UNLOCKED: User {} 님이 '7일 연속 기록' 뱃지를 획득했습니다.", user.getId());
            }
        }
    }

    // 3번 (따봉요정) 도전과제 달성
    private void checkReactionOnOthersQuestsAchievement(User user) {
        // 따봉 요정 4번
        final int BADGE_ID_REACTION_100 = 4;
        final int GOAL_COUNT = 100;

        // 뱃지를 이미 가지고 있는지 확인
        boolean alreadyHasBadge = userBadgeRepository.existsByUser_IdAndBadge_Id(user.getId(), BADGE_ID_REACTION_100);
        if (alreadyHasBadge) {
            return; // 이미 달성했으면 종료
        }

        long reactionCount = reactionRepository.countReactionsOnOthersQuests(user.getId());

        // 3. 100회 달성 시 보상 지급
        if (reactionCount >= GOAL_COUNT) {

            log.info("ACHIEVEMENT UNLOCKED: User {} 님이 '타인 글 리액션 100회' 도전과제를 달성했습니다.", user.getId());

            Badge reactionBadge = badgeRepository.findById(BADGE_ID_REACTION_100)
                    .orElse(null);

            if (reactionBadge != null) {
                UserBadge newUserBadge = new UserBadge();
                newUserBadge.setUser(user);
                newUserBadge.setBadge(reactionBadge);
                newUserBadge.setEquipped(false); // 칭호는 지급하지만 바로 장착X
                userBadgeRepository.save(newUserBadge);
            }

        }
    }

    // 5번 (인정드림) 도전과제 달성
    private void checkVerificationOnOthersQuestsAchievement(User user) {
        final int BADGE_ID_VERIFY_100 = 6;
        final int GOAL_COUNT = 100;

        boolean alreadyHasBadge = userBadgeRepository.existsByUser_IdAndBadge_Id(user.getId(), BADGE_ID_VERIFY_100);
        if (alreadyHasBadge) return;

        // 현재까지 '타인의 글'에 남긴 총 인증/댓글 횟수 조회
        long verificationCount = questVerificationRepository.countVerificationsOnOthersContent(user.getId());

        // 100회 달성 시 보상 지급
        if (verificationCount >= GOAL_COUNT) {

            log.info("ACHIEVEMENT UNLOCKED: User {} 님이 '타인 글 인증 100회' 도전과제를 달성했습니다.", user.getId());


            Badge verifyBadge = badgeRepository.findById(BADGE_ID_VERIFY_100)
                    .orElse(null);

            if (verifyBadge != null) {
                UserBadge newUserBadge = new UserBadge();
                newUserBadge.setUser(user);
                newUserBadge.setBadge(verifyBadge);
                newUserBadge.setEquipped(false);
                userBadgeRepository.save(newUserBadge);
            }

        }
    }

    // 첫 퀘스트 완료 캐릭터 지급
    private void checkFirstQuestCompletionAchievement(User user) {
        // 완료 피코 캐릭터 아이디
        final int REWARD_CHARACTER_ID = 3;

        boolean flag = userCharacterRepository.existsByUser_IdAndCharacterImage_Id(user.getId(), REWARD_CHARACTER_ID);
        if (flag) return;

        // 현재 완료된 퀘스트 개수 조회
        long completedCount = questRepository.countByUser_IdAndQuestStatus(user.getId(), QuestStatus.COMPLETE);

        if (completedCount == 1) {

            // 캐릭터 지급
            CharacterImage rewardCharacter = characterImageRepository.findById(REWARD_CHARACTER_ID);

            if (rewardCharacter != null) {
                // 이미 가지고 있는지 이중 체크
                boolean alreadyHas = userCharacterRepository.findAllByUser_Id(user.getId()).stream()
                        .anyMatch(uc -> uc.getCharacterImage().getId() == REWARD_CHARACTER_ID);

                if (!alreadyHas) {
                    UserCharacter newUserCharacter = new UserCharacter();
                    newUserCharacter.setUser(user);
                    newUserCharacter.setCharacterImage(rewardCharacter);
                    newUserCharacter.setEquipped(false); // 지급만 하고 장착은 안 함
                    userCharacterRepository.save(newUserCharacter);

                    log.info("ACHIEVEMENT UNLOCKED: User {} 님이 첫 퀘스트 완료 보상으로 캐릭터({})를 획득했습니다.", user.getId(), rewardCharacter.getName());
                }
            }
        }
    }

    // 첫 인증 받기 메서드
    private void checkFirstVerificationReceivedAchievement(User user) {
        // 스마일 피코
        final int REWARD_CHARACTER_ID = 4;

        // 이미 해당 캐릭터를 가지고 있는지 확인 (중복 지급 방지)
        boolean alreadyHas = userCharacterRepository.findAllByUser_Id(user.getId()).stream()
                .anyMatch(uc -> uc.getCharacterImage().getId() == REWARD_CHARACTER_ID);

        if (alreadyHas) {
            return;
        }

        // 현재까지 '받은' 총 인증/댓글 개수 조회
        long receivedCount = questVerificationRepository.countVerificationsReceivedByUser(user.getId());

        // 방금 받았으므로 개수가 1개
        if (receivedCount >= 1) {

            CharacterImage rewardCharacter = characterImageRepository.findById(REWARD_CHARACTER_ID);

            if (rewardCharacter != null) {
                UserCharacter newUserCharacter = new UserCharacter();
                newUserCharacter.setUser(user);
                newUserCharacter.setCharacterImage(rewardCharacter);
                newUserCharacter.setEquipped(false);
                userCharacterRepository.save(newUserCharacter);

                log.info("ACHIEVEMENT UNLOCKED: User {} 님이 첫 인증/댓글을 받아 캐릭터({})를 획득했습니다.", user.getId(), rewardCharacter.getName());
            }
        }
    }

    // 시작 피코 도전과제 메서드
    private void checkFirstVerificationOnOthersQuestAchievement(User user) {

        final int REWARD_CHARACTER_ID = 6;

        boolean alreadyHas = userCharacterRepository.existsByUser_IdAndCharacterImage_Id(
                user.getId(), REWARD_CHARACTER_ID);

        if (alreadyHas) {
            return;
        }

        // 현재까지 타인 퀘스트에 남긴 총 인증 횟수 조회
        long count = questVerificationRepository.countQuestVerificationsOnOthers(user.getId());

        if (count == 1) {

            CharacterImage rewardCharacter = characterImageRepository.findById(REWARD_CHARACTER_ID);

            if (rewardCharacter != null) {
                UserCharacter newUserCharacter = new UserCharacter();
                newUserCharacter.setUser(user);
                newUserCharacter.setCharacterImage(rewardCharacter);
                newUserCharacter.setEquipped(false);
                userCharacterRepository.save(newUserCharacter);

                log.info("ACHIEVEMENT UNLOCKED: User {} 님이 첫 인증/댓글을 해서 캐릭터({})를 획득했습니다.", user.getId(), rewardCharacter.getName());
            }
        }
    }



    // 추천 점수 계산 헬퍼 메서드
    private double calculateRecommendationScore(User currentUser, User questOwner) {
        double score = 0;

        // 레벨 유사도 점수 (차이가 적을수록 높음, 최대 50점)
        int levelDifference = Math.abs(currentUser.getLevel() - questOwner.getLevel());
        score += Math.max(0, 50 - (levelDifference * 5));

        // 유저 타입 일치 점수
        if (currentUser.getUserType().getId() == questOwner.getUserType().getId()) {
            score += 30;
        }

        // 액션 포인트 점수 (10점당 1점)
        score += (questOwner.getActionPoint() / 10.0);

        return score;
    }
}
