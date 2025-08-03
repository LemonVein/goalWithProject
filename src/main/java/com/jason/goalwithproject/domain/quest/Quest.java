package com.jason.goalwithproject.domain.quest;

import com.jason.goalwithproject.domain.team.Team;
import com.jason.goalwithproject.domain.user.User;
import com.jason.goalwithproject.dto.quest.QuestAddRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Quest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private Team team;

    private String title;

    private String description;

    @Column(name = "is_main")
    private boolean isMain = false;

    @CreationTimestamp
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "quest_status", nullable = false)
    private QuestStatus questStatus = QuestStatus.PROGRESS;

    @Column(name = "verification_required")
    private boolean verificationRequired = false;

    @Column(name = "verification_count")
    private int verificationCount = 0;

    @Column(name = "required_verification")
    private int requiredVerification = 2;

    public void updateFrom(QuestAddRequest request) {
        this.title = request.getTitle();
        this.description = request.getDescription();
        this.isMain = request.isMain();
        this.endDate = request.getEndDate();
        this.questStatus = request.getProcedure();
        this.verificationRequired = request.isVerificationRequired();
        this.requiredVerification = request.getRequiredVerification();
    }
}
