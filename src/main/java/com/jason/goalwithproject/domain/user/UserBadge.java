package com.jason.goalwithproject.domain.user;

import com.jason.goalwithproject.domain.custom.Badge;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "user_badge")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", referencedColumnName = "id")
    private Badge badge;

    @Column(name = "is_equipped")
    private boolean equipped;
}
