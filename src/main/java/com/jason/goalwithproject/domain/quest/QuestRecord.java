package com.jason.goalwithproject.domain.quest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class QuestRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_id", referencedColumnName = "id")
    private Quest quest;

    private LocalDateTime date;

    private String text;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
