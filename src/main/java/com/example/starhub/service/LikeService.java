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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    /**
     * 공통 검증 로직: 모임 가져오기
     */
    private MeetingEntity validateAndGetMeeting(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(ErrorCode.MEETING_NOT_FOUND));
    }

    /**
     * 공통 검증 로직: 사용자 가져오기
     */
    private UserEntity validateAndGetUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 좋아요 생성하기
     * - 모임 확정 여부와 상관없이 언제든지 좋아요 관련 기능을 이용할 수 있음
     *
     * @param username JWT에서 가져온 사용자명
     * @param meetingId 모임 아이디
     */
    public void createLike(String username, Long meetingId) {
        UserEntity userEntity = validateAndGetUser(username);
        MeetingEntity meetingEntity = validateAndGetMeeting(meetingId);

        LikeEntity likeEntity = LikeEntity.createLike(userEntity, meetingEntity);

        try {
            likeRepository.save(likeEntity);
        } catch (DataIntegrityViolationException e) {
            throw new LikeAlreadyExistsException(ErrorCode.LIKE_ALREADY_EXISTS);
        }
    }

    /**
     * 좋아요 삭제하기
     * - 모임 확정 여부와 상관없이 언제든지 좋아요 관련 기능을 이용할 수 있음
     *
     * @param username JWT에서 가져온 사용자명
     * @param meetingId 모임 아이디
     */
    public void deleteLike(String username, Long meetingId) {
        UserEntity userEntity = validateAndGetUser(username);
        MeetingEntity meetingEntity = validateAndGetMeeting(meetingId);

        LikeEntity likeEntity = likeRepository.findByUserAndMeeting(userEntity, meetingEntity)
                .orElseThrow(() -> new LikeNotFoundException(ErrorCode.LIKE_NOT_FOUND));

        likeRepository.delete(likeEntity);
    }
}
