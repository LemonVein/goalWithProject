package com.jason.goalwithproject.dto.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppleInfo {
    private String sub;   // 고유 ID
    private String email; // 이메일
}
