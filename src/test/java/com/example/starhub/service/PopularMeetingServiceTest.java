package com.example.starhub.service;

import com.example.starhub.dto.response.MeetingSummaryResponseDto;
import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.entity.enums.RecruitmentType;
import com.example.starhub.repository.LikeRepository;
import com.example.starhub.repository.MeetingRepository;
import com.example.starhub.repository.MeetingTechStackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularMeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingTechStackRepository meetingTechStackRepository;

    @Mock
    private LikeRepository likeRepository;

    @InjectMocks
    private PopularMeetingService popularMeetingService;

    private UserEntity user;
    private MeetingEntity meeting;

    @BeforeEach
    void setUp() {
        user = UserEntity.createUser("testUser", "testPassword");
        meeting = MeetingEntity.builder()
                .title("Test Meeting")
                .creator(user)
                .isConfirmed(false)
                .build();
    }

    @Test
    void testGetPopularProjects_success() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> meetings = Arrays.asList(meeting, meeting, meeting);

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.PROJECT, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);

        when(meetingRepository.findAllById(meetingIds)).thenReturn(meetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularProjects("testUser");

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testGetPopularProjects_noPopularMeetings() {
        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.PROJECT, PageRequest.of(0, 3)))
                .thenReturn(Collections.emptyList());

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularProjects("testUser");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPopularProjects_anonUser() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> meetings = Arrays.asList(meeting, meeting, meeting);

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.PROJECT, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(meetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularProjects(null);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertNull(result.get(0).getLikeDto().getIsLiked());
    }

    @Test
    void testGetPopularProjects_lessThanThreeMeetings() {
        List<Long> meetingIds = Arrays.asList(1L, 2L);
        List<MeetingEntity> meetings = Arrays.asList(meeting, meeting);

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.PROJECT, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(meetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularProjects("testUser");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetPopularProjects_dbError() {
        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.PROJECT, PageRequest.of(0, 3)))
                .thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () -> popularMeetingService.getPopularProjects("testUser"));
    }

    @Test
    void testGetPopularProjects_findAllByIdError() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.PROJECT, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds))
                .thenThrow(new RuntimeException("DB Error during findAllById"));

        assertThrows(RuntimeException.class, () -> popularMeetingService.getPopularProjects("testUser"));
    }

    @Test
    void testGetPopularProjects_dtoMapping() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> meetings = Arrays.asList(meeting, meeting, meeting);

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.PROJECT, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(meetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularProjects("testUser");

        assertNotNull(result);
        assertTrue(result.stream().allMatch(dto -> dto.getTitle().equals(meeting.getTitle()))); 
    }

    @Test
    void testGetPopularStudies_success() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> meetings = Arrays.asList(meeting, meeting, meeting);

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.STUDY, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);

        when(meetingRepository.findAllById(meetingIds)).thenReturn(meetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularStudies("testUser");

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testGetPopularStudies_noPopularMeetings() {
        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.STUDY, PageRequest.of(0, 3)))
                .thenReturn(Collections.emptyList());

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularStudies("testUser");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPopularStudies_anonUser() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> meetings = Arrays.asList(meeting, meeting, meeting);

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.STUDY, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(meetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularStudies(null);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertNull(result.get(0).getLikeDto().getIsLiked());
    }

    @Test
    void testGetPopularStudies_lessThanThreeMeetings() {
        List<Long> meetingIds = Arrays.asList(1L, 2L);
        List<MeetingEntity> meetings = Arrays.asList(meeting, meeting);

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.STUDY, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(meetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularStudies("testUser");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetPopularStudies_dbError() {
        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.STUDY, PageRequest.of(0, 3)))
                .thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () -> popularMeetingService.getPopularStudies("testUser"));
    }

    @Test
    void testGetPopularStudies_findAllByIdError() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.STUDY, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds))
                .thenThrow(new RuntimeException("DB Error during findAllById"));

        assertThrows(RuntimeException.class, () -> popularMeetingService.getPopularStudies("testUser"));
    }

    @Test
    void testGetPopularStudies_dtoMapping() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> meetings = Arrays.asList(meeting, meeting, meeting);

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.STUDY, PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(meetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularStudies("testUser");

        assertNotNull(result);
        assertTrue(result.stream().allMatch(dto -> dto.getTitle().equals(meeting.getTitle())));
    }

}