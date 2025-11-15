package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.QuestStatus;
import com.jason.goalwithproject.dto.user.UserDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// 인증 받을 퀘스트 목록을 불러올 때 사용하는 DTO 입니다.
@Getter
@Setter
@Builder
public class UserQuestVerifyResponseDto {
    private Long id;
    private String title;
    private String description;
    private boolean isMain;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private QuestStatus procedure;
    private boolean verificationRequired;
    private int verificationCount;
    private int requiredVerification;
    private List<QuestRecordDto> records;
    private UserDto user;
    private boolean verified;
    private boolean bookmarked;
}
