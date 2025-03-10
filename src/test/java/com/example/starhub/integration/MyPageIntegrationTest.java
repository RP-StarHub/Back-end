package com.example.starhub.integration;

import com.example.starhub.dto.request.ApplicationRequestDto;
import com.example.starhub.dto.request.CreateProfileRequestDto;
import com.example.starhub.dto.request.UpdateProfileRequestDto;
import com.example.starhub.dto.response.MeetingSummaryResponseDto;
import com.example.starhub.dto.response.ProfileResponseDto;
import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.exception.UserNotFoundException;
import com.example.starhub.repository.MeetingRepository;
import com.example.starhub.repository.UserRepository;
import com.example.starhub.service.ApplicationService;
import com.example.starhub.service.LikeService;
import com.example.starhub.service.MyPageService;
import com.example.starhub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MyPageIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MyPageService myPageService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ApplicationService applicationService;

    private UserEntity creator;
    private UserEntity applicant;
    List<Long> meetingIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        creator = userRepository.save(UserEntity.createUser("creatorUser", "creatorPassword"));
        applicant = userRepository.save(UserEntity.createUser("applicantUser", "applicantPassword"));

        saveUserProfile();

        for (int i = 1; i <= 5; i++) {
            MeetingEntity meeting = meetingRepository.save(MeetingEntity.builder()
                    .title("Test Meeting " + i)
                    .creator(creator)
                    .isConfirmed(false)
                    .build());

            meetingIds.add(meeting.getId());
        }
    }

    @Test
    void getUserProfile_Success() {

        ProfileResponseDto response = myPageService.getUserProfile(creator.getUsername());

        assertNotNull(response);
        assertEquals(creator.getName(), response.getName());
    }

    @Test
    void getUserProfile_shouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getUserProfile("invalidUser");
        });
    }

    @Test
    void updateUserProfile_Success() {
        UpdateProfileRequestDto updateProfileRequest = buildUpdateProfileRequestDto();

        ProfileResponseDto response = myPageService.updateUserProfile(creator.getUsername(), updateProfileRequest);

        assertNotNull(response);
        assertEquals(updateProfileRequest.getName(), response.getName());
    }

    @Test
    void updateUserProfile_shouldThrowUserNotFoundException() {
        UpdateProfileRequestDto updateProfileRequest = buildUpdateProfileRequestDto();

        assertThrows(UserNotFoundException.class, () -> {
            myPageService.updateUserProfile("invalidUser", updateProfileRequest);
        });
    }

    @Test
    void getUserRecentMeetings_withValidUsername_returnsTop3Meetings() {
        List<MeetingSummaryResponseDto> result = myPageService.getUserRecentMeetings(creator.getUsername());

        assertEquals(3, result.size());
    }

    @Test
    void getUserRecentMeetings_withLessThan3Creates_returnsCreatedMeetings() {
        meetingRepository.save(MeetingEntity.builder()
                .title("Test Meeting")
                .creator(applicant)
                .isConfirmed(false)
                .build());

        List<MeetingSummaryResponseDto> result = myPageService.getUserRecentMeetings(applicant.getUsername());

        assertEquals(1, result.size());
        assertEquals("Test Meeting", result.get(0).getTitle());
    }

    @Test
    void getUserRecentMeetings_withNoCreate_returnsEmptyList() {
        List<MeetingSummaryResponseDto> result = myPageService.getUserRecentMeetings(applicant.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserRecentMeetings_shouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getUserRecentMeetings("invalidUser");
        });
    }

    @Test
    void getLikedRecentMeetings_returnsTop3LikedMeetings() {
        createLikes(0, 5, applicant.getUsername());

        List<MeetingSummaryResponseDto> result = myPageService.getLikedRecentMeetings(applicant.getUsername());

        assertEquals(3, result.size());
        assertEquals("Test Meeting 5", result.get(0).getTitle());
        assertEquals("Test Meeting 4", result.get(1).getTitle());
        assertEquals("Test Meeting 3", result.get(2).getTitle());
    }

    @Test
    void getLikedRecentMeetings_withLessThan3Likes_returnsAllLikedMeetings() {
        createLikes(0, 1, applicant.getUsername());

        List<MeetingSummaryResponseDto> result = myPageService.getLikedRecentMeetings(applicant.getUsername());

        assertEquals(1, result.size());
        assertEquals("Test Meeting 1", result.get(0).getTitle());
    }

    @Test
    void getLikedRecentMeetings_withNoLikes_returnsEmptyList() {
        List<MeetingSummaryResponseDto> result = myPageService.getLikedRecentMeetings(creator.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void getAppliedRecentMeetings_returnsTop3AppliedMeetings() {
        createApplications(0, 5);

        List<MeetingSummaryResponseDto> result = myPageService.getAppliedRecentMeetings(applicant.getUsername());

        assertEquals(3, result.size());
        assertEquals("Test Meeting 5", result.get(0).getTitle());
        assertEquals("Test Meeting 4", result.get(1).getTitle());
        assertEquals("Test Meeting 3", result.get(2).getTitle());

    }

    @Test
    void getAppliedRecentMeetings_withLessThan3Likes_returnsAllLikedMeetings() {
        createApplications(0, 1);

        List<MeetingSummaryResponseDto> result = myPageService.getAppliedRecentMeetings(applicant.getUsername());

        assertEquals(1, result.size());
        assertEquals("Test Meeting 1", result.get(0).getTitle());
    }

    @Test
    void getAppliedRecentMeetings_withNoApplies_returnsEmptyList() {
        List<MeetingSummaryResponseDto> result = myPageService.getAppliedRecentMeetings(creator.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCreatedMeetings_withValidUsername_returnsPagedResults() {
        int page = 0;
        int size = 3;

        Page<MeetingSummaryResponseDto> result = myPageService.getCreatedMeetings(creator.getUsername(), page, size);

        assertEquals(3, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    void testGetCreatedMeetings_withSecondPage_returnsRemainingResults() {
        int page = 1;
        int size = 3;

        Page<MeetingSummaryResponseDto> result = myPageService.getCreatedMeetings(creator.getUsername(), page, size);

        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
    }

    @Test
    void testGetCreatedMeetings_withInvalidUsername_shouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getCreatedMeetings("invalidUser", 0, 5);
        });
    }

    @Test
    void testGetCreatedMeetings_withUserWhoDidNotCreateMeetings_returnsEmptyPage() {
        Page<MeetingSummaryResponseDto> result = myPageService.getCreatedMeetings(applicant.getUsername(), 0, 5);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLikedMeetings_withValidUsername_returnsPagedResults() {
        int page = 0;
        int size = 3;
        createLikes(0, 5, applicant.getUsername());

        Page<MeetingSummaryResponseDto> result = myPageService.getLikedMeetings(applicant.getUsername(), page, size);

        assertEquals(3, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    void testGetLikedMeetings_withSecondPage_returnsRemainingResults() {
        int page = 1;
        int size = 3;
        createLikes(0, 5, applicant.getUsername());

        Page<MeetingSummaryResponseDto> result = myPageService.getLikedMeetings(applicant.getUsername(), page, size);

        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
    }

    @Test
    void testGetLikedMeetings_withInvalidUsername_shouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getLikedMeetings("invalidUser", 0, 5);
        });
    }

    @Test
    void testGetLikedMeetings_withUserWhoDidNotCreateMeetings_returnsEmptyPage() {
        Page<MeetingSummaryResponseDto> result = myPageService.getLikedMeetings(creator.getUsername(), 0, 5);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAppliedMeetings_withValidUsername_returnsPagedResults() {
        int page = 0;
        int size = 3;
        createApplications(0, 5);

        Page<MeetingSummaryResponseDto> result = myPageService.getAppliedMeetings(applicant.getUsername(), page, size);

        assertEquals(3, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals("Test Meeting 5", result.getContent().get(0).getTitle());
        assertEquals("Test Meeting 4", result.getContent().get(1).getTitle());
    }

    @Test
    void testGetAppliedMeetings_withSecondPage_returnsRemainingResults() {
        int page = 1;
        int size = 3;
        createApplications(0, 5);

        Page<MeetingSummaryResponseDto> result = myPageService.getAppliedMeetings(applicant.getUsername(), page, size);

        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
    }

    @Test
    void testGetAppliedMeetings_withInvalidUsername_shouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class, () -> {
            myPageService.getAppliedMeetings("invalidUser", 0, 5);
        });
    }

    @Test
    void testGetAppliedMeetings_withUserWhoDidNotCreateMeetings_returnsEmptyPage() {
        Page<MeetingSummaryResponseDto> result = myPageService.getAppliedMeetings(creator.getUsername(), 0, 5);

        assertTrue(result.isEmpty());
    }

    private void saveUserProfile() {
        CreateProfileRequestDto createProfileRequest = new CreateProfileRequestDto("profileImage", "nickname", "name", 20, "bio", "email", "phoneNumber");
        userService.createUserProfile(creator.getUsername(), createProfileRequest);
    }

    private UpdateProfileRequestDto buildUpdateProfileRequestDto() {
        return new UpdateProfileRequestDto("updateProfileImage",
                "updateNickname", "updateName", 20, "updateBio", "updateEmail", "updatePhoneNumber");

    }

    private void createLikes(int start, int end, String username) {
        for (int i = start; i < end; i++) {
            likeService.createLike(username, meetingIds.get(i));
        }
    }

    private ApplicationRequestDto buildRequestDto() {
        ApplicationRequestDto requestDto = ApplicationRequestDto.builder()
                .content("This is a test application.")
                .build();
        return requestDto;
    }

    private void createApplications(int start, int end) {
        ApplicationRequestDto requestDto = buildRequestDto();

        for (int i = start; i < end; i++) {
            applicationService.createApplication(applicant.getUsername(), meetingIds.get(i), requestDto);
        }
    }
}