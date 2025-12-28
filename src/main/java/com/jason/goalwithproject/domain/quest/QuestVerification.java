package com.jason.goalwithproject.domain.quest;

import com.jason.goalwithproject.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private QuestVerification parent;

    @OneToMany(mappedBy = "parent", orphanRemoval = true)
    private List<QuestVerification> children = new ArrayList<>();

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_record_id", referencedColumnName = "id")
    private QuestRecord questRecord;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
