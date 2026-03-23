package com.jason.goalwithproject.dto.quest;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CommentAddRequestDtoForAdmin {
    private Long questId;
    private Long userId;
    private String text;
    private LocalDateTime createdAt;
    private Long parentId;

}
