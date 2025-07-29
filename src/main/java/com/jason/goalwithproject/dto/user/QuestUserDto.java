package com.jason.goalwithproject.dto.user;

import lombok.Getter;
import lombok.Setter;

// 퀘스트들을 불러 올 때, 함께 사용할 유저 DTO 입니다
@Getter
@Setter
public class QuestUserDto {
    private Long userId;
    private String nickname;
    private int level;
    private int actionPoints;
    private String userType;
    private String avatar;
    private String badge;

}
