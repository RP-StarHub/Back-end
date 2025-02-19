package com.example.starhub.repository;

import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.entity.enums.RecruitmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<MeetingEntity, Long>, MeetingRepositoryCustom {

    @Query("SELECT m FROM MeetingEntity m JOIN FETCH m.creator WHERE m.id = :meetingId")
    Optional<MeetingEntity> findWithCreatorById(@Param("meetingId") Long meetingId);
    List<MeetingEntity> findTop3ByCreatorOrderByCreatedAtDesc(UserEntity creator);
    Page<MeetingEntity> findByCreator(UserEntity creator, Pageable pageable);

    @Query("""
        SELECT m.id 
        FROM MeetingEntity m
        LEFT JOIN LikeEntity l ON l.meeting = m
        WHERE m.endDate > CURRENT_TIMESTAMP
            AND m.isConfirmed = false
        GROUP BY m.id
        ORDER BY m.endDate ASC, COUNT(l) DESC
    """)
    List<Long> findTop3ExpiringPopularMeetingsIds(Pageable pageable);

    @Query("""
        SELECT m.id 
        FROM MeetingEntity m
        LEFT JOIN LikeEntity l ON l.meeting = m
        WHERE m.recruitmentType = :recruitmentType 
            AND m.isConfirmed = false
        GROUP BY m.id
        ORDER BY COUNT(l) DESC
    """)
    List<Long> findTop3PopularMeetingIds(@Param("recruitmentType") RecruitmentType recruitmentType, Pageable pageable);

}
