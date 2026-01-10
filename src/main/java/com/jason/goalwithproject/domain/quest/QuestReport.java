package com.jason.goalwithproject.domain.quest;

import com.jason.goalwithproject.domain.user.User;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "quest_report")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id", referencedColumnName = "id")
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "quest_id", referencedColumnName = "id")
    private Quest quest;

    private String reason;
}
