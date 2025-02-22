package com.example.starhub.integration;

import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.exception.LikeAlreadyExistsException;
import com.example.starhub.exception.LikeNotFoundException;
import com.example.starhub.repository.LikeRepository;
import com.example.starhub.repository.MeetingRepository;
import com.example.starhub.repository.UserRepository;
import com.example.starhub.response.code.ErrorCode;
import com.example.starhub.service.LikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LikeIntegrationTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    private UserEntity user;
    private MeetingEntity meeting;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자와 모임 생성
        user = userRepository.save(UserEntity.createUser("testUser", "password"));
        meeting = meetingRepository.save(MeetingEntity.builder()
                .title("Test Meeting")
                .creator(user)
                .isConfirmed(false)
                .build());
    }

    @Test
    void createLike_shouldCreateLikeSuccessfully_whenLikeDoesNotExist() {
        likeService.createLike(user.getUsername(), meeting.getId());

        assertTrue(likeRepository.existsByUserAndMeeting(user, meeting));
    }

    @Test
    void createLike_shouldThrowException_whenLikeAlreadyExists() {

        likeService.createLike(user.getUsername(), meeting.getId());

        LikeAlreadyExistsException exception = assertThrows(LikeAlreadyExistsException.class, () -> {
            likeService.createLike(user.getUsername(), meeting.getId());
        });

        assertEquals(ErrorCode.LIKE_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void deleteLike_shouldDeleteLikeSuccessfully_whenLikeExists() {
        likeService.createLike(user.getUsername(), meeting.getId()); // 먼저 좋아요 추가

        likeService.deleteLike(user.getUsername(), meeting.getId());

        assertFalse(likeRepository.existsByUserAndMeeting(user, meeting));
    }

    @Test
    void deleteLike_shouldThrowException_whenLikeNotFound() {
        // When & Then
        LikeNotFoundException exception = assertThrows(LikeNotFoundException.class, () -> {
            likeService.deleteLike(user.getUsername(), meeting.getId());
        });

        assertEquals(ErrorCode.LIKE_NOT_FOUND, exception.getErrorCode());
    }
}