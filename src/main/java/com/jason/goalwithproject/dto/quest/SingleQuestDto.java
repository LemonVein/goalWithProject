package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.Quest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SingleQuestDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean verificationRequired;
    private int verificationCount;
    private int requiredVerification;

    public static SingleQuestDto from(Quest quest) {
        if (quest == null) {
            return null;
        }
        return SingleQuestDto.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .startDate(quest.getStartDate())
                .endDate(quest.getEndDate())
                .verificationRequired(quest.isVerificationRequired())
                .verificationCount(quest.getVerificationCount())
                .requiredVerification(quest.getRequiredVerification())
                .build();
    }
}
