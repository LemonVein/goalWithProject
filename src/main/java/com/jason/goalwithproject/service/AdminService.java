package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.quest.*;
import com.jason.goalwithproject.domain.user.User;
import com.jason.goalwithproject.domain.user.UserRefreshTokenRepository;
import com.jason.goalwithproject.domain.user.UserRepository;
import com.jason.goalwithproject.domain.user.UserStatus;
import com.jason.goalwithproject.dto.quest.QuestDto;
import com.jason.goalwithproject.dto.quest.QuestVerifyDto;
import com.jason.goalwithproject.dto.user.UserInfoForAdmin;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final QuestRepository questRepository;
    private final QuestVerificationRepository questVerificationRepository;
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


}
