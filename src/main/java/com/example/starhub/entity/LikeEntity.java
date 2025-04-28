package com.example.starhub.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "meeting_id"}),
        indexes = @Index(name = "idx_like_meeting_user", columnList = "meeting_id, user_id")
)
public class LikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 좋아요 고유 식별자

    @CreatedDate
    private LocalDateTime createdAt;  // 생성 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;  // 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingEntity meeting;   // 스터디

    public static LikeEntity createLike(UserEntity user, MeetingEntity meeting) {
        return LikeEntity.builder()
                .user(user)
                .meeting(meeting)
                .build();
    }

}

