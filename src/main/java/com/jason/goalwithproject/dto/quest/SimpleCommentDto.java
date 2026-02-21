package com.jason.goalwithproject.dto.quest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jason.goalwithproject.domain.quest.QuestStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleCommentDto {
    private Long id;
    private Long questId;
    private String comment;
    private LocalDateTime createdAt;
}
