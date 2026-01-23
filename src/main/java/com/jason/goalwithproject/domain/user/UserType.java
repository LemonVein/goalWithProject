package com.jason.goalwithproject.domain.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_type")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
}
