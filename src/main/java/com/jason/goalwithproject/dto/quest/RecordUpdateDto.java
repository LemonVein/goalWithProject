package com.jason.goalwithproject.dto.quest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordUpdateDto {
    private String text;
    private List<String> existingImages;
}
