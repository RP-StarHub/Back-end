package com.example.starhub.entity;

import com.example.starhub.dto.request.ApplicationRequestDto;
import com.example.starhub.entity.enums.ApplicationStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 지원서 고유 식별자

    @Column(columnDefinition = "TEXT")
    private String content;  // 지원서 내용

    @CreatedDate
    private LocalDateTime createdAt;  // 작성 시간

    @LastModifiedDate
    private LocalDateTime updatedAt;  // 수정 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private UserEntity applicant;  // 사용자(작성자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingEntity meeting;  // 해당 포스트

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    public static ApplicationEntity createApplication(UserEntity user, MeetingEntity meeting, ApplicationRequestDto applicationRequestDto) {
        return ApplicationEntity.builder()
                .applicant(user)
                .content(applicationRequestDto.getContent())
                .meeting(meeting)
                .status(ApplicationStatus.PENDING) // 기본 상태 (대기 상태)
                .build();
    }

    public void updateContent(String content) {
        this.content = content;
    }

    // 상태 변경 메서드
    public void approve() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 지원서만 승인할 수 있습니다.");
        }

        this.status = ApplicationStatus.APPROVED;
    }

    public void reject() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 지원서만 거절할 수 있습니다.");
        }

        this.status = ApplicationStatus.REJECTED;
    }

}
