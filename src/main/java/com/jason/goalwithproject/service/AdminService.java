package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.S3Uploader;
import com.jason.goalwithproject.domain.custom.Badge;
import com.jason.goalwithproject.domain.custom.BadgeRepository;
import com.jason.goalwithproject.domain.custom.CharacterImage;
import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.quest.*;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.quest.*;
import com.jason.goalwithproject.dto.user.SingleUserDtoForAdmin;
import com.jason.goalwithproject.dto.user.UserCreateRequestDtoForAdmin;
import com.jason.goalwithproject.dto.user.UserInfoForAdmin;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final QuestRepository questRepository;
    private final UserTypeRepository userTypeRepository;
    private final CharacterImageRepository characterImageRepository;
    private final QuestVerificationRepository questVerificationRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final RecordImageRepository recordImageRepository;
    private final S3Uploader s3Uploader;
    private final BadgeRepository badgeRepository;
    private final QuestRecordRepository questRecordRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final DtoConverterService dtoConverterService;

    // 유저들 불러오기
    public Page<UserInfoForAdmin> getAllUsersForAdmin(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);

        return userPage.map(dtoConverterService::convertToAdminDto);
    }

    // 모든 퀘스트들 불러오기
    public Page<QuestDto> getAllQuestsForAdmin(Pageable pageable) {
        Page<Quest> questPage = questRepository.findAll(pageable);

        return questPage.map(dtoConverterService::convertToSingleQuestDto);

    }

    public Page<QuestDto> getAllVerifyQuestsForAdmin(Pageable pageable) {
        Page<Quest> questPage = questRepository.findAllQuestsByStatus(QuestStatus.VERIFY, pageable);

        return questPage.map(dtoConverterService::convertToSingleQuestDto);

    }

    @Transactional(readOnly = true)
    public SingleUserDtoForAdmin getUserDetailForAdmin(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        List<Quest> userQuests = questRepository.findAllByUser_Id(targetUserId);

        List<QuestVerification> userVerifications = questVerificationRepository.findAllByUser_Id(targetUserId);

        return dtoConverterService.convertToSingleUserDtoForAdmin(user, userQuests, userVerifications);
    }

    // 단일 퀘스트 정보 조회
    public QuestVerifyDto getQuestById(Long id) {
        Quest quest = questRepository.findById(id).orElse(null);
        return dtoConverterService.convertToQuestVerifyDto(quest);
    }

    // 유저 정지
    @Transactional
    public void suspendUser(Long userId) {
        // 유저 상태 변경 (ACTIVE -> SUSPENDED)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저가 없습니다."));

        user.setUserStatus(UserStatus.SUSPENDED);

        userRefreshTokenRepository.deleteByUser_Id(userId);
    }

    // 유저 삭제
    @Transactional
    public void deleteUser(Long userId) {
        // 유저 상태 변경 (ACTIVE -> WITHDRAWN)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저가 없습니다."));

        user.setUserStatus(UserStatus.WITHDRAWN);

        userRefreshTokenRepository.deleteByUser_Id(userId);

        user.setName("삭제된 사용자");
        user.setNickName("Unknown User");
        user.setEmail("deleted_" + user.getId() + "@deleted.com");
    }

    // 퀘스트 삭제
    @Transactional
    public void deleteQuest(Long questId) {
        Quest quest = questRepository.findById(questId).orElseThrow(
                () -> new EntityNotFoundException("퀘스트가 존재하지 않습니다")
        );

        questRepository.delete(quest);
    }

    // 레코드 삭제
    @Transactional
    public void deleteQuestRecord(Long recordId) {
        QuestRecord questRecord = questRecordRepository.findById(recordId).orElseThrow(
                () -> new EntityNotFoundException("레코드가 존재 하지 않습니다")
        );
        questRecordRepository.delete(questRecord);
    }

    // 댓글 삭제
    @Transactional
    public void deleteVerification(Long commentId) {
        QuestVerification comment = questVerificationRepository.findById(commentId).orElseThrow(
                () -> new EntityNotFoundException("존재하지 않는 댓글입니다")
        );
        // 대댓글 카운터
        long commentCount = questVerificationRepository.countByParent_Id(comment.getId());
        if (commentCount > 0) {
            comment.setComment("삭제된 댓글입니다.");
            comment.setUser(null);
        }
        else {
            questVerificationRepository.delete(comment);
        }
    }

    // 임의 유저 생성
    @Transactional
    public Long createArbitraryUser(UserCreateRequestDtoForAdmin request) {

        if (userRepository.existsByNickName(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임 입니다.");
        }

        UserType userType = userTypeRepository.findByName(request.getUserType());
        if (userType == null) {
            throw new EntityNotFoundException("해당 유저 타입을 찾을 수 없습니다: " + request.getUserType());
        }

        String dummyEmail = "admin_created_" + UUID.randomUUID().toString().substring(0, 8) + "@goalwith.com";
        String dummyPassword = UUID.randomUUID().toString(); // 일반적인 로그인 불가하게

        User newUser = new User();
        newUser.setName("임의생성유저_" + request.getNickname());
        newUser.setNickName(request.getNickname());
        newUser.setEmail(dummyEmail);
        newUser.setPassword(dummyPassword);
        newUser.setUserType(userType);
        newUser.setLevel(request.getLevel());
        newUser.setActionPoint(request.getActionPoints());
        newUser.setExp(0);
        newUser.setRole(Role.ROLE_USER); // 기본 권한
        newUser.setUserStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(newUser);

        CharacterImage characterImage = characterImageRepository.findById(1);
        UserCharacter userCharacter = new UserCharacter();
        userCharacter.setUser(savedUser);
        userCharacter.setCharacterImage(characterImage);
        userCharacter.setEquipped(true);
        userCharacterRepository.save(userCharacter);

        Badge badge = badgeRepository.findById(1).orElse(null);

        UserBadge userBadge = new UserBadge();
        userBadge.setUser(savedUser);
        userBadge.setBadge(badge);
        userBadge.setEquipped(true);
        userBadgeRepository.save(userBadge);

        return savedUser.getId();

    }

    // 임의 퀘스트 생성
    @Transactional
    public Long createArbitraryQuest(QuestCreateRequestForAdmin request) {

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("해당 유저를 찾을 수 없습니다. ID: " + request.getUserId()));

        // isMain 여부 체크 로직
        // 해당 유저가 현재 진행 중인 메인 퀘스트가 있는지 확인
        boolean hasMainQuest = questRepository.existsByUser_IdAndIsMainTrueAndQuestStatus(targetUser.getId(), QuestStatus.PROGRESS);

        // 진행 중인 메인 퀘스트가 없다면 이 퀘스트를 메인으로 설정, 있다면 서브(false)로 설정
        boolean isMain = !hasMainQuest;

        // 퀘스트 엔티티 생성 (Builder 사용)
        Quest newQuest = Quest.builder()
                .user(targetUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .verificationRequired(true) // 무조건 true 설정
                .requiredVerification(request.getRequiredVerification())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isMain(isMain)
                .questStatus(QuestStatus.PROGRESS) // 기본 상태를 진행 중으로 설정
                .verificationCount(0)
                .build();

        Quest savedQuest = questRepository.save(newQuest);
        return savedQuest.getId();
    }

    // 임의 레코드의 리스틑 받아 저장하기
    @Transactional
    public void createArbitraryQuestRecords(RecordCreateDtoForAdminList requestList) throws IOException {

        if (requestList.getRecords() == null || requestList.getRecords().isEmpty()) {
            throw new IllegalArgumentException("등록할 레코드 데이터가 없습니다.");
        }

        for (RecordCreateDtoForAdmin dto : requestList.getRecords()) {
            Quest targetQuest = questRepository.findById(dto.getQuestId())
                    .orElseThrow(() -> new EntityNotFoundException("퀘스트를 찾을 수 없습니다. ID: " + dto.getQuestId()));

            QuestRecord newRecord = QuestRecord.builder()
                    .user(targetQuest.getUser())
                    .createdAt(dto.getCreatedAt())
                    .quest(targetQuest)
                    .text(dto.getText())
                    .build();

            // TimeCreation 으로 덮어씌워질수도 있으니
            newRecord.setCreatedAt(dto.getCreatedAt());

            QuestRecord savedRecord = questRecordRepository.save(newRecord);

            // 이미지가 존재한다면 S3 업로드 및 RecordImage 저장
            if (dto.getImages() != null && !dto.getImages().isEmpty()) {
                for (MultipartFile file : dto.getImages()) {
                    if (!file.isEmpty()) {
                        String imageUrl = s3Uploader.upload(file, "record-image");

                        // 이미지 엔티티 생성 및 저장
                        RecordImage recordImage = RecordImage.builder()
                                .questRecord(savedRecord)
                                .url(imageUrl)
                                .build();
                        recordImageRepository.save(recordImage);
                    }
                }
            }
        }
    }


}
