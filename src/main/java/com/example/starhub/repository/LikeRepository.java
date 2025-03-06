package com.example.starhub.repository;

import com.example.starhub.entity.LikeEntity;
import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {

    Long countByMeeting(MeetingEntity meetingEntity);
    boolean existsByMeetingAndUserUsername(MeetingEntity meetingEntity, String username);
    void deleteByMeeting(MeetingEntity meetingEntity);
    boolean existsByUserAndMeeting(UserEntity user, MeetingEntity meeting);
    List<LikeEntity> findTop3ByUserOrderByCreatedAtDesc(UserEntity user);
    Page<LikeEntity> findByUser(UserEntity user, Pageable pageable);
    int deleteByUserAndMeeting(UserEntity user, MeetingEntity meeting);
}
