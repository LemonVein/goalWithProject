package com.jason.goalwithproject.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jason.goalwithproject.dto.quest.SingleQuestDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserInformationDto {
    private Long id;
    private String nickname;
    private String email;
    private int level;
    private int actionPoints;
    private int exp;
    private String userType;
    // 캐릭터 url
    private String character;
    private String badge;
    @JsonProperty("main_quest")
    private SingleQuestDto mainQuest;

}
