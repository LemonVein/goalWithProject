package com.jason.goalwithproject.dto.custom;

import com.jason.goalwithproject.domain.user.UserBadge;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BadgeDto {
    private int id;
    private String name;

    public BadgeDto(UserBadge userBadge) {
        this.id = userBadge.getBadge().getId();
        this.name = userBadge.getBadge().getName();
    }
}
