package com.example.starhub.service;

import com.example.starhub.dto.response.MeetingSummaryResponseDto;
import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.MeetingTechStackEntity;
import com.example.starhub.entity.TechStackEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.entity.enums.RecruitmentType;
import com.example.starhub.entity.enums.TechCategory;
import com.example.starhub.repository.LikeRepository;
import com.example.starhub.repository.MeetingRepository;
import com.example.starhub.repository.MeetingTechStackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @BeforeEach
    void setUp() {
        user = UserEntity.createUser("testUser", "testPassword");
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDY", "PROJECT"})
    void testGetPopularMeetings_success(String recruitmentType) {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> mockMeetings = createMockMeetings(meetingIds.size());
        List<MeetingTechStackEntity> mockTechStacks = createMockTechStacks(mockMeetings);

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.valueOf(recruitmentType), PageRequest.of(0, 3))).thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(mockMeetings);
        when(meetingTechStackRepository.findMeetingTechStacksByMeetingIds(meetingIds)).thenReturn(mockTechStacks);
        when(likeRepository.countByMeeting(any(MeetingEntity.class))).thenReturn(3L);
        when(likeRepository.existsByMeetingAndUserUsername(any(MeetingEntity.class), eq(user.getUsername()))).thenReturn(false);

        List<MeetingSummaryResponseDto> result;
        if (recruitmentType.equals("STUDY")) {
            result = popularMeetingService.getPopularStudies(user.getUsername());
        } else {
            result = popularMeetingService.getPopularProjects(user.getUsername());
        }

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Project A", result.get(0).getTitle());
        assertEquals("Java", result.get(0).getTechStacks().get(0));
        assertFalse(result.get(0).getLikeDto().getIsLiked());
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDY", "PROJECT"})
    void testGetPopularMeetings_noPopularMeetings(String recruitmentType) {
        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.valueOf(recruitmentType), PageRequest.of(0, 3)))
                .thenReturn(Collections.emptyList());

        List<MeetingSummaryResponseDto> result;
        if (recruitmentType.equals("STUDY")) {
            result = popularMeetingService.getPopularStudies("testUser");
        } else {
            result = popularMeetingService.getPopularProjects("testUser");
        }

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDY", "PROJECT"})
    void testGetPopularMeetings_anonUser(String recruitmentType) {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> mockMeetings = createMockMeetings(meetingIds.size());

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.valueOf(recruitmentType), PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(mockMeetings);

        List<MeetingSummaryResponseDto> result;
        if (recruitmentType.equals("STUDY")) {
            result = popularMeetingService.getPopularStudies(null);
        } else {
            result = popularMeetingService.getPopularProjects(null);
        }

        assertNotNull(result);
        assertEquals(3, result.size());
        assertNull(result.get(0).getLikeDto().getIsLiked());
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDY", "PROJECT"})
    void testGetPopularMeetings_lessThanThreeMeetings(String recruitmentType) {
        List<Long> meetingIds = Arrays.asList(1L, 2L);
        List<MeetingEntity> mockMeetings = createMockMeetings(meetingIds.size());

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.valueOf(recruitmentType), PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(mockMeetings);

        List<MeetingSummaryResponseDto> result;
        if (recruitmentType.equals("STUDY")) {
            result = popularMeetingService.getPopularStudies("testUser");
        } else {
            result = popularMeetingService.getPopularProjects("testUser");
        }

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDY", "PROJECT"})
    void testGetPopularMeetings_dbError(String recruitmentType) {
        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.valueOf(recruitmentType), PageRequest.of(0, 3)))
                .thenThrow(new RuntimeException("DB Error"));

        if (recruitmentType.equals("STUDY")) {
            assertThrows(RuntimeException.class, () -> popularMeetingService.getPopularStudies("testUser"));
        } else {
            assertThrows(RuntimeException.class, () -> popularMeetingService.getPopularProjects("testUser"));
        }
    }


    @ParameterizedTest
    @ValueSource(strings = {"STUDY", "PROJECT"})
    void testGetPopularMeetings_findAllByIdError(String recruitmentType) {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.valueOf(recruitmentType), PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds))
                .thenThrow(new RuntimeException("DB Error during findAllById"));

        if (recruitmentType.equals("STUDY")) {
            assertThrows(RuntimeException.class, () -> popularMeetingService.getPopularStudies("testUser"));
        } else {
            assertThrows(RuntimeException.class, () -> popularMeetingService.getPopularProjects("testUser"));
        }
    }


    @ParameterizedTest
    @ValueSource(strings = {"STUDY", "PROJECT"})
    void testGetPopularMeetings_dtoMapping(String recruitmentType) {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> mockMeetings = createMockMeetings(meetingIds.size());

        when(meetingRepository.findTop3PopularMeetingIds(RecruitmentType.valueOf(recruitmentType), PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(mockMeetings);

        List<MeetingSummaryResponseDto> result;
        if (recruitmentType.equals("STUDY")) {
            result = popularMeetingService.getPopularStudies("testUser");
        } else {
            result = popularMeetingService.getPopularProjects("testUser");
        }

        assertNotNull(result);
        assertTrue(result.stream().allMatch(dto ->
                mockMeetings.stream().anyMatch(meeting ->
                        meeting.getTitle().equals(dto.getTitle())
                )
        ));
    }

    @Test
    void testGetExpiringPopularMeetings_success() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> mockMeetings = createMockMeetings(meetingIds.size());
        List<MeetingTechStackEntity> mockTechStacks = createMockTechStacks(mockMeetings);

        when(meetingRepository.findTop3ExpiringPopularMeetingsIds(PageRequest.of(0, 3))).thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(mockMeetings);
        when(meetingTechStackRepository.findMeetingTechStacksByMeetingIds(meetingIds)).thenReturn(mockTechStacks);
        when(likeRepository.countByMeeting(any(MeetingEntity.class))).thenReturn(3L);
        when(likeRepository.existsByMeetingAndUserUsername(any(MeetingEntity.class), eq(user.getUsername()))).thenReturn(false);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getExpiringPopularMeetings(user.getUsername());

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Project A", result.get(0).getTitle());
        assertEquals("Java", result.get(0).getTechStacks().get(0));
        assertFalse(result.get(0).getLikeDto().getIsLiked());
    }

    @Test
    void testGetExpiringPopularMeetings_noPopularMeetings() {
        when(meetingRepository.findTop3ExpiringPopularMeetingsIds(PageRequest.of(0, 3)))
                .thenReturn(Collections.emptyList());

        List<MeetingSummaryResponseDto> result = popularMeetingService.getExpiringPopularMeetings("testUser");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetExpiringPopularMeetings_anonUser() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> mockMeetings = createMockMeetings(meetingIds.size());

        when(meetingRepository.findTop3ExpiringPopularMeetingsIds(PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(mockMeetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getExpiringPopularMeetings(null);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertNull(result.get(0).getLikeDto().getIsLiked());
    }

    @Test
    void testGetExpiringPopularMeetings_lessThanThreeMeetings() {
        List<Long> meetingIds = Arrays.asList(1L, 2L);
        List<MeetingEntity> mockMeetings = createMockMeetings(meetingIds.size());

        when(meetingRepository.findTop3ExpiringPopularMeetingsIds(PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(mockMeetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getExpiringPopularMeetings("testUser");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetExpiringPopularMeetings_dbError() {
        when(meetingRepository.findTop3ExpiringPopularMeetingsIds(PageRequest.of(0, 3)))
                .thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () -> popularMeetingService.getExpiringPopularMeetings("testUser"));
    }

    @Test
    void testGetExpiringPopularMeetings_findAllByIdError() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        when(meetingRepository.findTop3ExpiringPopularMeetingsIds(PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds))
                .thenThrow(new RuntimeException("DB Error during findAllById"));

        assertThrows(RuntimeException.class, () -> popularMeetingService.getExpiringPopularMeetings("testUser"));
    }

    @Test
    void testGetExpiringPopularMeetings_dtoMapping() {
        List<Long> meetingIds = Arrays.asList(1L, 2L, 3L);
        List<MeetingEntity> mockMeetings = createMockMeetings(meetingIds.size());

        when(meetingRepository.findTop3ExpiringPopularMeetingsIds(PageRequest.of(0, 3)))
                .thenReturn(meetingIds);
        when(meetingRepository.findAllById(meetingIds)).thenReturn(mockMeetings);

        List<MeetingSummaryResponseDto> result = popularMeetingService.getExpiringPopularMeetings("testUser");

        assertNotNull(result);
        assertTrue(result.stream().allMatch(dto ->
                mockMeetings.stream().anyMatch(meeting ->
                        meeting.getTitle().equals(dto.getTitle())
                )
        ));
    }

    private List<MeetingTechStackEntity> createMockTechStacks(List<MeetingEntity> mockMeetings) {
        List<MeetingTechStackEntity> mockTechStacks = Arrays.asList(
                new MeetingTechStackEntity(4L, mockMeetings.get(0), new TechStackEntity(1L, "Java", TechCategory.BACKEND)),
                new MeetingTechStackEntity(5L, mockMeetings.get(1), new TechStackEntity(2L, "React", TechCategory.FRONTEND)),
                new MeetingTechStackEntity(6L, mockMeetings.get(2), new TechStackEntity(3L, "Git", TechCategory.OTHER))
        );
        return mockTechStacks;
    }

    private List<MeetingEntity> createMockMeetings(int number) {
        List<MeetingEntity> mockMeetings = new ArrayList<>();

        for (int i = 1; i <= number; i++) {
            MeetingEntity meeting = MeetingEntity.builder()
                    .id((long) i)
                    .title("Project " + (char) ('A' + (i - 1)))
                    .recruitmentType(RecruitmentType.PROJECT)
                    .build();

            mockMeetings.add(meeting);
        }

        return mockMeetings;
    }
}