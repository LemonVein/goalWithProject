package com.jason.goalwithproject.dto.quest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jason.goalwithproject.domain.quest.QuestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class QuestAddRequest {
    private String title;
    private String description;
    @JsonProperty("isMain")
    private boolean isMain;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private QuestStatus procedure;
    private boolean verificationRequired;
    private int requiredVerification;
}
