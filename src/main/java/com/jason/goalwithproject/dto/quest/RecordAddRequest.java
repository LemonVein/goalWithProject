package com.jason.goalwithproject.dto.quest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class RecordAddRequest {
    private String text;
    private List<String> images;
}
