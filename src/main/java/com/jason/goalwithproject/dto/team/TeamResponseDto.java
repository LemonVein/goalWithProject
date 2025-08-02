package com.jason.goalwithproject.dto.team;

import com.jason.goalwithproject.domain.quest.Quest;
import com.jason.goalwithproject.domain.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class TeamResponseDto {
    private int id;
    private String name;
    private String description;
    private List<User> members;
    private User leader;
    private boolean isPublic;
    private LocalDateTime createdAt;
    private Quest teamQuest;
}
