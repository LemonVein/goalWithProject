package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.QuestVerification;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestVerificationDto {
    private Long userId;
    private String comment;

    public static QuestVerificationDto fromEntity(QuestVerification questVerification) {
        QuestVerificationDto dto = new QuestVerificationDto();
        dto.setUserId(questVerification.getUser().getId());
        dto.setComment(questVerification.getComment());
        return dto;
    }
}
