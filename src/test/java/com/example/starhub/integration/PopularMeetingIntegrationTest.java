package com.example.starhub.integration;

import com.example.starhub.dto.request.CreateMeetingRequestDto;
import com.example.starhub.dto.request.TechStackDto;
import com.example.starhub.dto.response.MeetingResponseDto;
import com.example.starhub.dto.response.MeetingSummaryResponseDto;
import com.example.starhub.entity.TechStackEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.entity.enums.Duration;
import com.example.starhub.entity.enums.RecruitmentType;
import com.example.starhub.entity.enums.TechCategory;
import com.example.starhub.repository.LikeRepository;
import com.example.starhub.repository.MeetingRepository;
import com.example.starhub.repository.TechStackRepository;
import com.example.starhub.repository.UserRepository;
import com.example.starhub.service.LikeService;
import com.example.starhub.service.MeetingService;
import com.example.starhub.service.PopularMeetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Nested
class PopularMeetingIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private TechStackRepository techStackRepository;

    @Autowired
    private PopularMeetingService popularMeetingService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private MeetingService meetingService;

    private UserEntity creator;
    private UserEntity applicant;
    List<Long> meetingIds = new ArrayList<>();
    List<TechStackEntity> techStackList;

    @BeforeEach
    void setUp() {
        creator = userRepository.save(UserEntity.createUser("creatorUser", "creatorPassword"));
        applicant = userRepository.save(UserEntity.createUser("applicantUser", "applicantPassword"));
    }

    @Test
    void testGetPopularProjects_returnsTop3PopularProjects() {
        createDataForPopular();

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularProjects(creator.getUsername());

        assertEquals(3, result.size());
        assertEquals("Test Meeting 1", result.get(0).getTitle());
        assertEquals("Test Meeting 2", result.get(1).getTitle());
        assertEquals("Test Meeting 3", result.get(2).getTitle());
        assertEquals(RecruitmentType.PROJECT, result.get(0).getRecruitmentType());
    }

    @Test
    void testGetPopularProjects_whenAnonymousUser_doesNotIncludeLikeInfo() {
        createDataForPopular();

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularProjects(null);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertNull(result.get(0).getLikeDto().getIsLiked());
    }

    @Test
    void testGetPopularProjects_returnsEmptyListWhenNoProjectsExist() {

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularProjects(creator.getUsername());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPopularStudies_returnsTop3PopularProjects() {
        createDataForPopular();

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularStudies(creator.getUsername());

        assertEquals(3, result.size());
        assertEquals("Test Meeting 1", result.get(0).getTitle());
        assertEquals("Test Meeting 2", result.get(1).getTitle());
        assertEquals("Test Meeting 3", result.get(2).getTitle());
        assertEquals(RecruitmentType.STUDY, result.get(0).getRecruitmentType());
    }

    @Test
    void testGetPopularStudies_whenAnonymousUser_doesNotIncludeLikeInfo() {
        createDataForPopular();

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularStudies(null);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertNull(result.get(0).getLikeDto().getIsLiked());
    }

    @Test
    void testGetPopularStudies_returnsEmptyListWhenNoProjectsExist() {

        List<MeetingSummaryResponseDto> result = popularMeetingService.getPopularStudies(creator.getUsername());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetExpiringPopularMeetings_returnsTop3PopularProjects() {
        createDataForPopular();

        List<MeetingSummaryResponseDto> result = popularMeetingService.getExpiringPopularMeetings(creator.getUsername());

        assertEquals(3, result.size());
        assertEquals("Test Meeting 1", result.get(0).getTitle());
        assertEquals("Test Meeting 1", result.get(1).getTitle());
        assertEquals("Test Meeting 2", result.get(2).getTitle());
        assertEquals(RecruitmentType.PROJECT, result.get(0).getRecruitmentType());
        assertEquals(RecruitmentType.STUDY, result.get(1).getRecruitmentType());
    }

    @Test
    void testGetExpiringPopularMeetings_whenAnonymousUser_doesNotIncludeLikeInfo() {
        createDataForPopular();

        List<MeetingSummaryResponseDto> result = popularMeetingService.getExpiringPopularMeetings(null);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertNull(result.get(0).getLikeDto().getIsLiked());
    }

    @Test
    void testGetExpiringPopularMeetings_returnsEmptyListWhenNoProjectsExist() {

        List<MeetingSummaryResponseDto> result = popularMeetingService.getExpiringPopularMeetings(creator.getUsername());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private void createDataForPopular() {
        clearTechStackData();
        saveTechStacks();
        techStackList = getAllTechStacks();
        createMeetings();
        addLikesToMeetings();
    }

    private void clearTechStackData() {
        techStackRepository.deleteAll();
    }

    private void saveTechStacks() {
        saveTechStack("React", TechCategory.FRONTEND);
        saveTechStack("Spring", TechCategory.BACKEND);
        saveTechStack("React Native", TechCategory.MOBILE);
        saveTechStack("OtherTool", TechCategory.OTHER);
    }

    private List<TechStackEntity> getAllTechStacks() {
        return techStackRepository.findAll();
    }

    private void createMeetings() {
        for (int i = 1; i <= 5; i++) {
            saveMeeting(RecruitmentType.PROJECT, i);
            saveMeeting(RecruitmentType.STUDY, i);
        }
    }

    private void addLikesToMeetings() {
        int likeCount = 2;
        for (Long meetingId : meetingIds) {
            addLikes(likeCount, meetingId);
            likeCount += 2;
        }
    }


    private MeetingResponseDto saveMeeting(RecruitmentType recruitmentType, int idx) {
        CreateMeetingRequestDto createMeetingRequestDto = buildCreateMeetingRequestDto(recruitmentType, idx);

        MeetingResponseDto meetingResponse = meetingService.createMeeting(creator.getUsername(), createMeetingRequestDto);
        return meetingResponse;
    }

    private CreateMeetingRequestDto buildCreateMeetingRequestDto(RecruitmentType recruitmentType, int idx) {
        CreateMeetingRequestDto requestDto = new CreateMeetingRequestDto(
                recruitmentType, 5, Duration.ONE_MONTH,
                LocalDate.now().plusMonths(2), "서울 강남구",
                37.5665, 126.9780, "Test Meeting " + idx,
                "스프링과 JPA 학습", "포트폴리오 제작", "기타 정보", List.of(techStackList.get(0).getId()), List.of("GraphQL")
        );
        return requestDto;
    }

    private TechStackEntity saveTechStack(String name, TechCategory category) {
        return techStackRepository.save(TechStackEntity.createTechStackEntity(buildTechStackDto(name, category)));
    }

    private TechStackDto buildTechStackDto(String name, TechCategory category) {
        return TechStackDto.builder()
                .name(name)
                .category(category)
                .build();
    }

    private void addLikes(int likeCount, Long meetingId) {

        for (int i = 0; i < likeCount; i++) {
            String username = "user" + i;
            userRepository.findByUsername(username)
                    .orElseGet(() -> userRepository.save(UserEntity.createUser(username, "password")));

            likeService.createLike(username, meetingId);
        }
    }
}
