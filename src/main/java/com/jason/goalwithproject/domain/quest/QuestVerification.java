package com.jason.goalwithproject.domain.quest;

import com.jason.goalwithproject.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class QuestVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", referencedColumnName = "id")
    private Quest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_record_id", referencedColumnName = "id")
    private QuestRecord questRecord;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
