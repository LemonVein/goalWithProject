package com.jason.goalwithproject.dto.user;

import com.jason.goalwithproject.dto.quest.SimpleCommentDto;
import com.jason.goalwithproject.dto.quest.SimpleQuestDto;
import com.jason.goalwithproject.dto.quest.SingleQuestDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SingleUserDtoForAdmin {
    private Long id;
    private String name;
    private String nickname;
    private String email;
    private String character;
    private int level;
    private int actionPoints;
    private LocalDateTime createdAt;
    private List<SimpleQuestDto> quests;
    private List<SimpleCommentDto> verifications;
}
