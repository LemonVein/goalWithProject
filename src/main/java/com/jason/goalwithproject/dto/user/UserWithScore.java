package com.jason.goalwithproject.dto.user;

import com.jason.goalwithproject.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserWithScore {
    private User user;
    private double score;
}
