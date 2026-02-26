package com.jason.goalwithproject.dto.quest;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class QuestCreateRequestForAdmin {
    private Long userId;
    private String title;
    private String description;
    private int requiredVerification;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
