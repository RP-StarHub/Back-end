package com.example.starhub.service;

import com.example.starhub.config.cache.CustomCacheable;
import com.example.starhub.dto.response.LikeDto;
import com.example.starhub.dto.response.MeetingSummaryResponseDto;
import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.MeetingTechStackEntity;
import com.example.starhub.entity.enums.RecruitmentType;
import com.example.starhub.repository.LikeRepository;
import com.example.starhub.repository.MeetingRepository;
import com.example.starhub.repository.MeetingTechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopularMeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingTechStackRepository meetingTechStackRepository;
    private final LikeRepository likeRepository;

    public static final int FIRST_PAGE = 0;
    private static final int TOP_N_POPULAR_MEETINGS = 3;

    /**
     * 인기글 페이지 - 프로젝트 인기글 3개를 반환합니다.
     */
    @CustomCacheable(key = "'popular:project'", ttl = 300)
    public List<MeetingSummaryResponseDto> getPopularProjects(String username) {
        return getPopularMeetings(RecruitmentType.PROJECT, username, false);
    }

    /**
     * 인기글 페이지 - 스터디 인기글 3개를 반환합니다.
     */
    @CustomCacheable(key = "'popular:study'", ttl = 300)
    public List<MeetingSummaryResponseDto> getPopularStudies(String username) {
        return getPopularMeetings(RecruitmentType.STUDY, username, false);
    }

    /**
     * 인기글 페이지 - 마감임박 인기글 3개를 반환합니다.
     */
    @CustomCacheable(key = "'popular:deadline'", ttl = 300)
    public List<MeetingSummaryResponseDto> getExpiringPopularMeetings(String username) {
        return getPopularMeetings(null, username, true);
    }

    /**
     * 인기글 페이지 - 프로젝트, 스터디, 마감임박 인기글을 반환합니다.
     *
     * @param recruitmentType 모집 유형 (`PROJECT`, `STUDY`, 등)
     * @param username 사용자명
     * @param isExpiring 마감임박 여부
     * @return 모임 요약된 정보가 담긴 DTO
     */
    private List<MeetingSummaryResponseDto> getPopularMeetings(RecruitmentType recruitmentType, String username, boolean isExpiring) {
        List<Long> meetingIds = getMeetingIds(recruitmentType, isExpiring);
        if (meetingIds.isEmpty()) return Collections.emptyList();

        List<MeetingEntity> meetings = meetingRepository.findAllById(meetingIds);
        Map<Long, Set<String>> meetingTechStacksMap = getTechStacksMap(meetingIds);

        return meetings.stream()
                .map(meeting -> createMeetingSummaryResponseDto(meeting, username, meetingTechStacksMap))
                .collect(Collectors.toList());
    }

    /**
     * 모집 유형과 마감임박 여부에 따라 모임 ID 리스트를 가져옵니다.
     */
    private List<Long> getMeetingIds(RecruitmentType recruitmentType, boolean isExpiring) {
        if (isExpiring) {
            return meetingRepository.findTop3ExpiringPopularMeetingsIds(PageRequest.of(FIRST_PAGE, TOP_N_POPULAR_MEETINGS));
        } else {
            return meetingRepository.findTop3PopularMeetingIds(recruitmentType, PageRequest.of(FIRST_PAGE, TOP_N_POPULAR_MEETINGS));
        }
    }

    /**
     * 모임 ID 리스트에 해당하는 기술 스택 정보를 가져옵니다.
     */
    private Map<Long, Set<String>> getTechStacksMap(List<Long> meetingIds) {
        List<MeetingTechStackEntity> meetingTechStacks = meetingTechStackRepository.findMeetingTechStacksByMeetingIds(meetingIds);
        return meetingTechStacks.stream()
                .collect(Collectors.groupingBy(
                        mts -> mts.getMeeting().getId(),
                        Collectors.mapping(mts -> mts.getTechStack().getName(), Collectors.toSet())
                ));
    }

    /**
     * 해당 모임 ID에 대해 DTO를 생성합니다.
     */
    private MeetingSummaryResponseDto createMeetingSummaryResponseDto(MeetingEntity meeting, String username, Map<Long, Set<String>> meetingTechStacksMap) {
        Set<String> techStacksSet = meetingTechStacksMap.getOrDefault(meeting.getId(), Collections.emptySet());
        List<String> techStacks = new ArrayList<>(techStacksSet);

        // 좋아요 관련 정보를 가져오기
        LikeDto likeDto = getLikeDtoForMeeting(meeting, username);

        return MeetingSummaryResponseDto.fromEntity(meeting, techStacks, likeDto);
    }

    /**
     * 모임에 대한 좋아요 정보 및 내가 좋아요를 눌렀는지 여부를 반환하는 메서드
     * - 익명 사용자의 경우 isLiked 정보를 넘기지 않음 -> null
     *
     * @param meetingEntity 모임 엔티티
     * @param username   사용자명
     * @return 좋아요 DTO
     */
    private LikeDto getLikeDtoForMeeting(MeetingEntity meetingEntity, String username) {
        Long likeCount = likeRepository.countByMeeting(meetingEntity);

        // 익명 사용자인 경우 좋아요 여부를 포함하지 않음
        Boolean isLiked = (username != null)
                ? likeRepository.existsByMeetingAndUserUsername(meetingEntity, username)
                : null;

        return LikeDto.builder()
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }
}
