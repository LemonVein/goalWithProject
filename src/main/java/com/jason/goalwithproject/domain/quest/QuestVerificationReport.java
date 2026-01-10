package com.jason.goalwithproject.domain.quest;

import com.jason.goalwithproject.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "verification_report")
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class QuestVerificationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id", referencedColumnName = "id")
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "verification_id", referencedColumnName = "id")
    private QuestVerification verification;

    private String reason;
}
