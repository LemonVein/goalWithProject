package com.jason.goalwithproject.dto.quest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jason.goalwithproject.domain.quest.QuestStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class QuestResponseDto {
    private Long id;
    private String title;
    private String description;
    @JsonProperty("isMain")
    private boolean isMain;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private QuestStatus procedure;
    private boolean verificationRequired;
    private int verificationCount;
    private int requiredVerification;
    private List<QuestRecordDto> records;
    private List<QuestVerificationDto> verifications;
}
