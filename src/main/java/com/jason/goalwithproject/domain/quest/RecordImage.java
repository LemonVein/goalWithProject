package com.jason.goalwithproject.domain.quest;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "record_image")
public class RecordImage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_record_id", referencedColumnName = "id")
    private QuestRecord questRecord;

    private String url;
}
