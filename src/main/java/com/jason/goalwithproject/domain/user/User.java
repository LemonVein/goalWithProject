package com.jason.goalwithproject.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class User {
    @Id
    @Column(unique = true, length = 32)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "nickname")
    private String nickName;

    private String email;

    private String password;

    private int level = 1;

    private int actionPoint = 0;

    private int exp = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_type_id", referencedColumnName = "id")
    private UserType userType;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public User(String name, String email, String password, String nickName, UserType userType) {
        this.name = name;
        this.email = email;
        this.nickName = nickName;
        this.userType = userType;
        this.password = password;

    }

}
