package com.jason.goalwithproject.dto.quest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordCreateDtoForAdminList {
    private List<RecordCreateDtoForAdmin> records = new ArrayList<>();
}
