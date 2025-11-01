package com.jason.goalwithproject.domain.quest;

import com.jason.goalwithproject.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ManyToOne
    private User user;

    @JoinColumn(name = "quest_id", referencedColumnName = "id")
    @ManyToOne
    private Quest quest;

    private LocalDateTime date;
}
