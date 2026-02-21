package com.jason.goalwithproject.dto.quest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jason.goalwithproject.domain.quest.QuestStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SimpleQuestDto {
    private Long id;

    @JsonProperty("procedure")
    private QuestStatus questStatus;

    private String title;
    private LocalDateTime createdAt;
}
