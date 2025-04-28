package com.example.starhub.entity;

import com.example.starhub.dto.request.CreateMeetingRequestDto;
import com.example.starhub.dto.request.UpdateMeetingRequestDto;
import com.example.starhub.entity.enums.Duration;
import com.example.starhub.entity.enums.RecruitmentType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "post",
        indexes = {
                @Index(name = "idx_post_recruit_confirm", columnList = "recruitmentType, isConfirmed"),
                @Index(name = "idx_post_enddate_confirm", columnList = "endDate, isConfirmed")
        }
)
public class MeetingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 스터디 고유 식별자

    @Enumerated(EnumType.STRING)
    private RecruitmentType recruitmentType;  // 모집 구분 (스터디 / 프로젝트)

    private Integer maxParticipants;  // 모집 인원

    @Enumerated(EnumType.STRING)
    private Duration duration;  // 진행 기간

    private LocalDate endDate;  // 마감 날짜

    @Column(length = 100)
    private String location;  // 진행 장소

    @Column(precision = 15, scale = 13) // 최대 정수부 2자리 + 소수부 13자리
    private Double latitude;  // 위도

    @Column(precision = 16, scale = 13) // 최대 정수부 3자리 + 소수부 13자리
    private Double longitude;  // 경도

    @Column(length = 100)
    private String title;  // 스터디 제목

    @Column(columnDefinition = "TEXT")
    private String description;  // 스터디 소개

    @Column(columnDefinition = "TEXT")
    private String goal;  // 스터디 목표

    @Column(columnDefinition = "TEXT")
    private String otherInfo;  // 기타 정보

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성일

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;  // 수정일

    private Boolean isConfirmed;  // 모임 확정 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creator;  // 사용자(작성자)

    public static MeetingEntity createMeeting(UserEntity user, CreateMeetingRequestDto createMeetingRequestDto) {
        return MeetingEntity.builder()
                .recruitmentType(createMeetingRequestDto.getRecruitmentType())
                .maxParticipants(createMeetingRequestDto.getMaxParticipants())
                .duration(createMeetingRequestDto.getDuration())
                .endDate(createMeetingRequestDto.getEndDate())
                .location(createMeetingRequestDto.getLocation())
                .latitude(createMeetingRequestDto.getLatitude())
                .longitude(createMeetingRequestDto.getLongitude())
                .title(createMeetingRequestDto.getTitle())
                .description(createMeetingRequestDto.getDescription())
                .goal(createMeetingRequestDto.getGoal())
                .otherInfo(createMeetingRequestDto.getOtherInfo())
                .isConfirmed(false) // 기본값 설정
                .creator(user)
                .build();
    }

    public void updateMeeting(UpdateMeetingRequestDto updateMeetingRequestDto) {
        this.recruitmentType = updateValue(updateMeetingRequestDto.getRecruitmentType(), this.recruitmentType);
        this.maxParticipants = updateValue(updateMeetingRequestDto.getMaxParticipants(), this.maxParticipants);
        this.duration = updateValue(updateMeetingRequestDto.getDuration(), this.duration);
        this.endDate = updateValue(updateMeetingRequestDto.getEndDate(), this.endDate);
        this.location = updateValue(updateMeetingRequestDto.getLocation(), this.location);
        this.latitude = updateValue(updateMeetingRequestDto.getLatitude(), this.latitude);
        this.longitude = updateValue(updateMeetingRequestDto.getLongitude(), this.longitude);
        this.title = updateValue(updateMeetingRequestDto.getTitle(), this.title);
        this.description = updateValue(updateMeetingRequestDto.getDescription(), this.description);
        this.goal = updateValue(updateMeetingRequestDto.getGoal(), this.goal);
        this.otherInfo = updateValue(updateMeetingRequestDto.getOtherInfo(), this.otherInfo);
    }

    private <T> T updateValue(T newValue, T currentValue) {
        return newValue != null ? newValue : currentValue;
    }

    public void confirm() {

        this.isConfirmed = true;
    }

}
