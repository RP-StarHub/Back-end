package com.example.starhub.service;

import com.example.starhub.dto.request.ApplicationRequestDto;
import com.example.starhub.dto.request.UpdateProfileRequestDto;
import com.example.starhub.dto.response.MeetingSummaryResponseDto;
import com.example.starhub.dto.response.ProfileResponseDto;
import com.example.starhub.entity.ApplicationEntity;
import com.example.starhub.entity.LikeEntity;
import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.entity.enums.RecruitmentType;
import com.example.starhub.exception.UserNotFoundException;
import com.example.starhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MyPageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MeetingTechStackRepository meetingTechStackRepository;

    @InjectMocks
    private MyPageService myPageService;

    private UserEntity creator;
    private UserEntity applicant;
    private List<MeetingEntity> meetings;

    @BeforeEach
    void setUp() {
        creator = UserEntity.createUser("creatorUser", "creatorPassword");
        applicant = UserEntity.createUser("applicantUser", "applicantPassword");

        creator.createProfile("profile.jpg", "nick1", "creatorUser", 25, "Bio", "email@example.com", "010-1234-5678");
        applicant.createProfile("profile.jpg", "nick2", "applicantUser", 22, "Bio", "applicant@example.com", "010-5678-1234");

        meetings = createMockMeetings(5);
    }

    @Test
    void getUserProfile_Success() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        ProfileResponseDto response = myPageService.getUserProfile(creator.getUsername());

        assertNotNull(response);
        assertEquals(creator.getName(), response.getName());
    }

    @Test
    void getUserProfile_shouldThrowUserNotFoundException() {
        when(userRepository.findByUsername("invalidUser")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getUserProfile("invalidUser");
        });
    }

    @Test
    void updateUserProfile_Success() {
        UpdateProfileRequestDto updateProfileRequest = buildUpdateProfileRequestDto();
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        ProfileResponseDto response = myPageService.updateUserProfile(creator.getUsername(), updateProfileRequest);

        assertNotNull(response);
        assertEquals(updateProfileRequest.getName(), response.getName());
    }

    @Test
    void updateUserProfile_shouldThrowUserNotFoundException() {
        UpdateProfileRequestDto updateProfileRequest = buildUpdateProfileRequestDto();
        when(userRepository.findByUsername("invalidUser")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            myPageService.updateUserProfile("invalidUser", updateProfileRequest);
        });
    }

    @Test
    void getUserRecentMeetings_withValidUsername_returnsTop3Meetings() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        when(meetingRepository.findTop3ByCreatorOrderByCreatedAtDesc(creator))
                .thenReturn(meetings.subList(0, 3));

        List<MeetingSummaryResponseDto> result = myPageService.getUserRecentMeetings(creator.getUsername());

        assertEquals(3, result.size());
        assertEquals("Project A", result.get(0).getTitle());
        assertEquals("Project B", result.get(1).getTitle());
        assertEquals("Project C", result.get(2).getTitle());
    }

    @Test
    void getUserRecentMeetings_withLessThan3Creates_returnsCreatedMeetings() {
        MeetingEntity mockMeeting = MeetingEntity.builder()
                .title("Test Meeting")
                .creator(applicant)
                .isConfirmed(false)
                .build();

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(meetingRepository.findTop3ByCreatorOrderByCreatedAtDesc(applicant)).thenReturn(List.of(mockMeeting));

        List<MeetingSummaryResponseDto> result = myPageService.getUserRecentMeetings(applicant.getUsername());

        assertEquals(1, result.size());
        assertEquals("Test Meeting", result.get(0).getTitle());
    }

    @Test
    void getUserRecentMeetings_withNoCreatedMeetings_returnsEmptyList() {
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(meetingRepository.findTop3ByCreatorOrderByCreatedAtDesc(applicant)).thenReturn(List.of());

        List<MeetingSummaryResponseDto> result = myPageService.getUserRecentMeetings(applicant.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserRecentMeetings_shouldThrowUserNotFoundException() {
        when(userRepository.findByUsername("invalidUser")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getUserRecentMeetings("invalidUser");
        });
    }

    @Test
    void getLikedRecentMeetings_returnsTop3LikedMeetings() {
        MeetingEntity meeting5 = MeetingEntity.builder()
                .id(5L)
                .title("Test Meeting 5")
                .recruitmentType(RecruitmentType.PROJECT)
                .build();

        MeetingEntity meeting4 = MeetingEntity.builder()
                .id(4L)
                .title("Test Meeting 4")
                .recruitmentType(RecruitmentType.PROJECT)
                .build();

        MeetingEntity meeting3 = MeetingEntity.builder()
                .id(3L)
                .title("Test Meeting 3")
                .recruitmentType(RecruitmentType.PROJECT)
                .build();

        when(likeRepository.findTop3ByUserOrderByCreatedAtDesc(applicant))
                .thenReturn(Arrays.asList(
                        LikeEntity.createLike(applicant, meeting5),
                        LikeEntity.createLike(applicant, meeting4),
                        LikeEntity.createLike(applicant, meeting3)
                ));

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));

        List<MeetingSummaryResponseDto> result = myPageService.getLikedRecentMeetings(applicant.getUsername());

        assertEquals(3, result.size());
        assertEquals("Test Meeting 5", result.get(0).getTitle());
        assertEquals("Test Meeting 4", result.get(1).getTitle());
        assertEquals("Test Meeting 3", result.get(2).getTitle());
    }

    @Test
    void getLikedRecentMeetings_withLessThan3Likes_returnsAllLikedMeetings() {
        MeetingEntity meeting1 = MeetingEntity.builder()
                .id(1L)
                .title("Test Meeting 1")
                .recruitmentType(RecruitmentType.PROJECT)
                .build();

        when(likeRepository.findTop3ByUserOrderByCreatedAtDesc(applicant))
                .thenReturn(Arrays.asList(LikeEntity.createLike(applicant, meeting1)));

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));

        List<MeetingSummaryResponseDto> result = myPageService.getLikedRecentMeetings(applicant.getUsername());

        assertEquals(1, result.size());
        assertEquals("Test Meeting 1", result.get(0).getTitle());
    }

    @Test
    void getLikedRecentMeetings_withNoLikes_returnsEmptyList() {
        when(likeRepository.findTop3ByUserOrderByCreatedAtDesc(applicant))
                .thenReturn(new ArrayList<>());

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));

        List<MeetingSummaryResponseDto> result = myPageService.getLikedRecentMeetings(applicant.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void getAppliedRecentMeetings_returnsTop3AppliedMeetings() {
        MeetingEntity meeting5 = MeetingEntity.builder()
                .id(5L)
                .title("Test Meeting 5")
                .recruitmentType(RecruitmentType.PROJECT)
                .build();

        MeetingEntity meeting4 = MeetingEntity.builder()
                .id(4L)
                .title("Test Meeting 4")
                .recruitmentType(RecruitmentType.PROJECT)
                .build();

        MeetingEntity meeting3 = MeetingEntity.builder()
                .id(3L)
                .title("Test Meeting 3")
                .recruitmentType(RecruitmentType.PROJECT)
                .build();

        ApplicationRequestDto requestDto = buildApplicationRequestDto();
        when(applicationRepository.findTop3ByApplicantOrderByCreatedAtDesc(applicant))
                .thenReturn(Arrays.asList(
                        ApplicationEntity.createApplication(applicant, meeting5, requestDto),
                        ApplicationEntity.createApplication(applicant, meeting4, requestDto),
                        ApplicationEntity.createApplication(applicant, meeting3, requestDto)
                ));

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));

        List<MeetingSummaryResponseDto> result = myPageService.getAppliedRecentMeetings(applicant.getUsername());

        assertEquals(3, result.size());
        assertEquals("Test Meeting 5", result.get(0).getTitle());
        assertEquals("Test Meeting 4", result.get(1).getTitle());
        assertEquals("Test Meeting 3", result.get(2).getTitle());
    }

    @Test
    void getAppliedRecentMeetings_withLessThan3Applies_returnsAllAppliedMeetings() {
        MeetingEntity meeting1 = MeetingEntity.builder()
                .id(1L)
                .title("Test Meeting 1")
                .recruitmentType(RecruitmentType.PROJECT)
                .build();

        when(applicationRepository.findTop3ByApplicantOrderByCreatedAtDesc(applicant))
                .thenReturn(Arrays.asList(ApplicationEntity.createApplication(applicant, meeting1, buildApplicationRequestDto())));

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));

        List<MeetingSummaryResponseDto> result = myPageService.getAppliedRecentMeetings(applicant.getUsername());

        assertEquals(1, result.size());
        assertEquals("Test Meeting 1", result.get(0).getTitle());
    }

    @Test
    void getAppliedRecentMeetings_withNoApplies_returnsEmptyList() {
        when(applicationRepository.findTop3ByApplicantOrderByCreatedAtDesc(applicant))
                .thenReturn(new ArrayList<>());

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));

        List<MeetingSummaryResponseDto> result = myPageService.getAppliedRecentMeetings(applicant.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCreatedMeetings_withValidUsername_returnsPagedResults() {
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt")));

        Page<MeetingEntity> pagedMeetings = new PageImpl<>(meetings.subList(0, 3), pageRequest, meetings.size());

        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        when(meetingRepository.findByCreator(creator, pageRequest)).thenReturn(pagedMeetings);

        Page<MeetingSummaryResponseDto> result = myPageService.getCreatedMeetings(creator.getUsername(), 0, 3);

        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals("Project A", result.getContent().get(0).getTitle());
        assertEquals("Project B", result.getContent().get(1).getTitle());
    }

    @Test
    void testGetCreatedMeetings_withSecondPage_returnsRemainingResults() {
        int page = 1;
        int size = 3;

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        Page<MeetingEntity> pagedMeetings = new PageImpl<>(meetings.subList(3, 5), pageRequest, meetings.size());

        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        when(meetingRepository.findByCreator(creator, pageRequest)).thenReturn(pagedMeetings);

        Page<MeetingSummaryResponseDto> result = myPageService.getCreatedMeetings(creator.getUsername(), page, size);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals("Project D", result.getContent().get(0).getTitle());
        assertEquals("Project E", result.getContent().get(1).getTitle());
    }

    @Test
    void testGetCreatedMeetings_withInvalidUsername_shouldThrowUserNotFoundException() {
        when(userRepository.findByUsername("invalidUser")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getCreatedMeetings("invalidUser", 0, 5);
        });
    }

    @Test
    void testGetCreatedMeetings_withUserWhoDidNotCreateMeetings_returnsEmptyPage() {
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));

        when(meetingRepository.findByCreator(applicant, PageRequest.of(0, 5, Sort.by(Sort.Order.desc("createdAt")))))
                .thenReturn(Page.empty());

        Page<MeetingSummaryResponseDto> result = myPageService.getCreatedMeetings(applicant.getUsername(), 0, 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLikedMeetings_withValidUsername_returnsPagedResults() {

        LikeEntity like1 = LikeEntity.createLike(creator, meetings.get(0));
        LikeEntity like2 = LikeEntity.createLike(applicant, meetings.get(1));

        Page<LikeEntity> likedMeetingsPage = new PageImpl<>(List.of(like1, like2));

        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(likeRepository.findByUser(creator, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(likedMeetingsPage);

        when(meetingTechStackRepository.findByMeeting(meetings.get(0))).thenReturn(List.of());
        when(meetingTechStackRepository.findByMeeting(meetings.get(1))).thenReturn(List.of());

        when(likeRepository.countByMeeting(meetings.get(0))).thenReturn(5L);
        when(likeRepository.countByMeeting(meetings.get(1))).thenReturn(3L);

        when(likeRepository.existsByMeetingAndUserUsername(meetings.get(0), creator.getUsername())).thenReturn(true);
        when(likeRepository.existsByMeetingAndUserUsername(meetings.get(1), creator.getUsername())).thenReturn(true);

        Page<MeetingSummaryResponseDto> result = myPageService.getLikedMeetings(creator.getUsername(), 0, 3);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Project A", result.getContent().get(0).getTitle());
        assertEquals("Project B", result.getContent().get(1).getTitle());
        assertTrue(result.getContent().get(0).getLikeDto().getIsLiked());
        assertTrue(result.getContent().get(1).getLikeDto().getIsLiked());
    }

    @Test
    void testGetLikedMeetings_withSecondPage_returnsRemainingResults() {
        LikeEntity like1 = LikeEntity.createLike(creator, meetings.get(0));
        LikeEntity like2 = LikeEntity.createLike(applicant, meetings.get(1));

        Page<LikeEntity> likedMeetingsPage = new PageImpl<>(List.of(like1, like2));

        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(likeRepository.findByUser(creator, PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(likedMeetingsPage);

        when(meetingTechStackRepository.findByMeeting(meetings.get(0))).thenReturn(List.of());
        when(meetingTechStackRepository.findByMeeting(meetings.get(1))).thenReturn(List.of());

        when(likeRepository.countByMeeting(meetings.get(0))).thenReturn(5L);
        when(likeRepository.countByMeeting(meetings.get(1))).thenReturn(3L);

        when(likeRepository.existsByMeetingAndUserUsername(meetings.get(0), creator.getUsername())).thenReturn(true);
        when(likeRepository.existsByMeetingAndUserUsername(meetings.get(1), creator.getUsername())).thenReturn(true);

        Page<MeetingSummaryResponseDto> result = myPageService.getLikedMeetings(creator.getUsername(), 1, 3);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Project A", result.getContent().get(0).getTitle());
        assertEquals("Project B", result.getContent().get(1).getTitle());
        assertTrue(result.getContent().get(0).getLikeDto().getIsLiked());
        assertTrue(result.getContent().get(1).getLikeDto().getIsLiked());
    }

    @Test
    void testGetLikedMeetings_withInvalidUsername_shouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getLikedMeetings("invalidUser", 0, 5);
        });
    }

    @Test
    void testGetLikedMeetings_withUserWhoDidNotCreateMeetings_returnsEmptyPage() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(likeRepository.findByUser(creator, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(Page.empty());

        Page<MeetingSummaryResponseDto> result = myPageService.getLikedMeetings(creator.getUsername(), 0, 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAppliedMeetings_withValidUser_returnsPagedMeetings() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        ApplicationEntity testApplication = ApplicationEntity.builder()
                .id(1L)
                .applicant(applicant)
                .meeting(meetings.get(0))
                .build();

        Page<ApplicationEntity> applicationPage = new PageImpl<>(List.of(testApplication));
        when(applicationRepository.findByApplicant(eq(creator), any(Pageable.class)))
                .thenReturn(applicationPage);

        when(meetingTechStackRepository.findByMeeting(meetings.get(0))).thenReturn(List.of());
        when(likeRepository.countByMeeting(meetings.get(0))).thenReturn(10L);
        when(likeRepository.existsByMeetingAndUserUsername(meetings.get(0), creator.getUsername()))
                .thenReturn(true);

        Page<MeetingSummaryResponseDto> result = myPageService.getAppliedMeetings(creator.getUsername(), 0, 5);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());

        MeetingSummaryResponseDto meetingDto = result.getContent().get(0);
        assertEquals("Project A", meetingDto.getTitle());
        assertEquals(10L, meetingDto.getLikeDto().getLikeCount());
        assertTrue(meetingDto.getLikeDto().getIsLiked());
    }

    @Test
    void testGetAppliedMeetings_withSecondPage_returnsRemainingResults() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        ApplicationEntity testApplication = ApplicationEntity.builder()
                .id(1L)
                .applicant(applicant)
                .meeting(meetings.get(0))
                .build();

        Page<ApplicationEntity> applicationPage = new PageImpl<>(List.of(testApplication));
        when(applicationRepository.findByApplicant(eq(creator), any(Pageable.class)))
                .thenReturn(applicationPage);

        when(meetingTechStackRepository.findByMeeting(meetings.get(0))).thenReturn(List.of());
        when(likeRepository.countByMeeting(meetings.get(0))).thenReturn(10L);
        when(likeRepository.existsByMeetingAndUserUsername(meetings.get(0), creator.getUsername()))
                .thenReturn(true);

        Page<MeetingSummaryResponseDto> result = myPageService.getAppliedMeetings(creator.getUsername(), 1, 5);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());

        MeetingSummaryResponseDto meetingDto = result.getContent().get(0);
        assertEquals("Project A", meetingDto.getTitle());
        assertEquals(10L, meetingDto.getLikeDto().getLikeCount());
        assertTrue(meetingDto.getLikeDto().getIsLiked());
    }

    @Test
    void testGetAppliedMeetings_withInvalidUsername_shouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getAppliedMeetings("invalidUser", 0, 5);
        });
    }

    @Test
    void testGetAppliedMeetings_withUserWhoDidNotCreateMeetings_returnsEmptyPage() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(applicationRepository.findByApplicant(creator, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(Page.empty());

        Page<MeetingSummaryResponseDto> result = myPageService.getAppliedMeetings(creator.getUsername(), 0, 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
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

    private UpdateProfileRequestDto buildUpdateProfileRequestDto() {
        return new UpdateProfileRequestDto("updateProfileImage",
                "updateNickname", "updateName", 20, "updateBio", "updateEmail", "updatePhoneNumber");

    }

    private ApplicationRequestDto buildApplicationRequestDto() {
        ApplicationRequestDto requestDto = ApplicationRequestDto.builder()
                .content("This is a test application.")
                .build();
        return requestDto;
    }
}
