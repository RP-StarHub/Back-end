package com.example.starhub.service;

import com.example.starhub.entity.LikeEntity;
import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.exception.LikeAlreadyExistsException;
import com.example.starhub.exception.LikeNotFoundException;
import com.example.starhub.exception.MeetingNotFoundException;
import com.example.starhub.exception.UserNotFoundException;
import com.example.starhub.repository.LikeRepository;
import com.example.starhub.repository.MeetingRepository;
import com.example.starhub.repository.UserRepository;
import com.example.starhub.response.code.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LikeService likeService;

    private UserEntity user;
    private MeetingEntity meeting;
    private LikeEntity likeEntity;

    @BeforeEach
    void setUp() {
        user = UserEntity.createUser("testUser", "testPassword");
        meeting = MeetingEntity.builder()
                .title("Test Meeting")
                .creator(user)
                .isConfirmed(false)
                .build();

        likeEntity = LikeEntity.createLike(user, meeting);
    }

    @Test
    void createLike_Success() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(likeRepository.save(any(LikeEntity.class))).thenReturn(likeEntity);

        assertDoesNotThrow(() -> likeService.createLike("testUser", 1L));
        verify(likeRepository, times(1)).save(any(LikeEntity.class));
    }

    @Test
    void createLike_UserNotFound() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> likeService.createLike("testUser", 1L));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createLike_MeetingNotFound() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(meetingRepository.findById(1L)).thenReturn(Optional.empty());

        MeetingNotFoundException exception = assertThrows(MeetingNotFoundException.class, () -> likeService.createLike("testUser", 1L));
        assertEquals(ErrorCode.MEETING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createLike_AlreadyExists() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(likeRepository.save(any(LikeEntity.class))).thenThrow(DataIntegrityViolationException.class);

        LikeAlreadyExistsException exception = assertThrows(LikeAlreadyExistsException.class, () -> likeService.createLike("testUser", 1L));
        assertEquals(ErrorCode.LIKE_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void deleteLike_Success() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(likeRepository.findByUserAndMeeting(user, meeting)).thenReturn(Optional.of(likeEntity));

        assertDoesNotThrow(() -> likeService.deleteLike("testUser", 1L));
        verify(likeRepository, times(1)).delete(likeEntity);
    }

    @Test
    void deleteLike_UserNotFound() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> likeService.deleteLike("testUser", 1L));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteLike_MeetingNotFound() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(meetingRepository.findById(1L)).thenReturn(Optional.empty());

        MeetingNotFoundException exception = assertThrows(MeetingNotFoundException.class, () -> likeService.deleteLike("testUser", 1L));
        assertEquals(ErrorCode.MEETING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteLike_NotFound() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(likeRepository.findByUserAndMeeting(user, meeting)).thenReturn(Optional.empty());

        LikeNotFoundException exception = assertThrows(LikeNotFoundException.class, () -> likeService.deleteLike("testUser", 1L));
        assertEquals(ErrorCode.LIKE_NOT_FOUND, exception.getErrorCode());
    }

}