package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.QuestStatus;
import com.jason.goalwithproject.dto.user.UserDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class QuestVerifyDto {
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
    private List<RecordCommentDto> verifications;
    private UserDto user;
}
