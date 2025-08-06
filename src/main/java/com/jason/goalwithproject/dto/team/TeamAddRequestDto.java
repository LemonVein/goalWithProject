package com.jason.goalwithproject.dto.team;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamAddRequestDto {
    private String name;
    private String description;
    private boolean isPublic;
}
