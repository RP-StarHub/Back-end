package com.example.starhub.service;

import com.example.starhub.dto.request.ApplicationRequestDto;
import com.example.starhub.dto.request.ConfirmMeetingRequestDto;
import com.example.starhub.dto.request.CreateMeetingRequestDto;
import com.example.starhub.dto.request.UpdateMeetingRequestDto;
import com.example.starhub.dto.response.ConfirmMeetingResponseDto;
import com.example.starhub.dto.response.MeetingDetailResponseDto;
import com.example.starhub.dto.response.MeetingResponseDto;
import com.example.starhub.entity.*;
import com.example.starhub.entity.enums.ApplicationStatus;
import com.example.starhub.entity.enums.Duration;
import com.example.starhub.entity.enums.RecruitmentType;
import com.example.starhub.entity.enums.TechCategory;
import com.example.starhub.exception.*;
import com.example.starhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @InjectMocks
    private MeetingService meetingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private TechStackRepository techStackRepository;

    @Mock
    private MeetingTechStackRepository meetingTechStackRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private CreateMeetingRequestDto createMeetingRequestDto;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private TechStackService techStackService;

    private UserEntity creator; // 개설자
    private UserEntity applicant; // 지원자
    private UserEntity failedApplicant; // 모임 거절된 지원자
    private MeetingEntity meeting;
    private MeetingEntity confirmedMeeting;

    @BeforeEach
    void setUp() {
        creator = UserEntity.createUser("creatorUser", "creatorPassword");
        applicant = UserEntity.createUser("applicantUser", "applicantPassword");
        failedApplicant = UserEntity.createUser("failedUser", "failedPassword");

        meeting = MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(false)
                .build();

        confirmedMeeting = MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(true)
                .build();

    }

    @Test
    void createMeeting_Success() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(meetingRepository.save(any(MeetingEntity.class))).thenReturn(meeting);

        MeetingResponseDto result = meetingService.createMeeting(creator.getUsername(), createMeetingRequestDto);

        assertNotNull(result);
        verify(userRepository, times(1)).findByUsername(creator.getUsername());
        verify(meetingRepository, times(1)).save(any(MeetingEntity.class));
    }

    @Test
    void createMeeting_UserNotFound() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                meetingService.createMeeting(creator.getUsername(), createMeetingRequestDto));

        verify(userRepository, times(1)).findByUsername(creator.getUsername());
        verify(meetingRepository, never()).save(any(MeetingEntity.class));
    }

    @Test
    void createMeeting_VerifySave() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(meetingRepository.save(any(MeetingEntity.class))).thenReturn(meeting);

        meetingService.createMeeting(creator.getUsername(), createMeetingRequestDto);

        verify(meetingRepository, times(1)).save(any(MeetingEntity.class));
    }

    @Test
    void createMeeting_RepositorySaveException() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(meetingRepository.save(any(MeetingEntity.class))).thenThrow(new RuntimeException("Database Error"));

        assertThrows(RuntimeException.class, () ->
                meetingService.createMeeting(creator.getUsername(), createMeetingRequestDto));
    }

    @Test
    void createMeeting_TechStackProcessingError() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(meetingRepository.save(any(MeetingEntity.class))).thenReturn(meeting);
        doThrow(new RuntimeException("Tech stack processing error")).when(meetingTechStackRepository).saveAll(any());

        assertThrows(RuntimeException.class, () ->
                meetingService.createMeeting(creator.getUsername(), createMeetingRequestDto));
    }

    @Test
    void createMeeting_WithEmptyTechStack() {
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(meetingRepository.save(any(MeetingEntity.class))).thenReturn(meeting);

        MeetingResponseDto result = meetingService.createMeeting(creator.getUsername(), createMeetingRequestDto);
        assertNotNull(result);
    }


    @Test
    void getMeetingDetail_meetingNotFound() {
        Long meetingId = meeting.getId();
        String username = "testUser";
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () -> meetingService.getMeetingDetail(username, meetingId));
    }

    @Test
    void getMeetingDetail_userIsCreator() {
        Long meetingId = meeting.getId();
        String username = creator.getUsername();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(username, meetingId);

        assertEquals("Creator", response.getUserType());
    }

    @Test
    void getMeetingDetail_userIsApplicant() {
        Long meetingId = meeting.getId();

        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(
                applicant,
                meeting,
                applicationRequestDto);

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(applicationRepository.findByApplicantAndMeeting(applicant, meeting)).thenReturn(Optional.of(applicationEntity));

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(applicant.getUsername(), meetingId);

        assertTrue(response.getIsApplication());
        assertEquals(ApplicationStatus.PENDING, response.getApplicationStatus());
    }

    @Test
    void getMeetingDetail_userIsNotApplicant() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(failedApplicant.getUsername())).thenReturn(Optional.of(failedApplicant));
        when(applicationRepository.findByApplicantAndMeeting(failedApplicant, meeting)).thenReturn(Optional.empty());

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(failedApplicant.getUsername(), meetingId);

        assertFalse(response.getIsApplication());
    }

    @Test
    void getMeetingDetail_userIsAnonymous() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(null, meetingId);

        assertEquals("Anonymous", response.getUserType());
    }

    @Test
    void getMeetingDetail_withTechStacks() {
        Long meetingId = meeting.getId();
        List<String> techStacks = List.of("Java", "Spring");

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingTechStackRepository.findByMeeting(meeting)).thenReturn(
                techStacks.stream().map(stack -> {
                    TechStackEntity techStackEntity = TechStackEntity.builder()
                            .name(stack)
                            .category(TechCategory.BACKEND)
                            .build();

                    return MeetingTechStackEntity.builder()
                            .meeting(meeting)
                            .techStack(techStackEntity)
                            .build();
                }).collect(Collectors.toList())
        );

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(creator.getUsername(), meetingId);

        assertTrue(response.getPostInfo().getTechStacks().contains("Java"));
        assertTrue(response.getPostInfo().getTechStacks().contains("Spring"));
    }

    @Test
    void getMeetingDetail_withApplicationAndTechStacksAndLikes() {
        Long meetingId = meeting.getId();
        List<String> techStacks = List.of("Java", "Spring");
        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(
                applicant,
                meeting,
                applicationRequestDto);

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(applicationRepository.findByApplicantAndMeeting(any(), eq(meeting))).thenReturn(Optional.of(applicationEntity));
        when(meetingTechStackRepository.findByMeeting(meeting)).thenReturn(
                techStacks.stream().map(stack -> {
                    TechStackEntity techStackEntity = TechStackEntity.builder()
                            .name(stack)
                            .category(TechCategory.BACKEND)
                            .build();
                    return MeetingTechStackEntity.builder()
                            .meeting(meeting)
                            .techStack(techStackEntity)
                            .build();
                }).collect(Collectors.toList())
        );

        when(likeRepository.countByMeeting(meeting)).thenReturn(10L);
        when(likeRepository.existsByMeetingAndUserUsername(meeting, applicant.getUsername())).thenReturn(true);

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(applicant.getUsername(), meetingId);

        assertTrue(response.getPostInfo().getTechStacks().contains("Java"));
        assertTrue(response.getPostInfo().getTechStacks().contains("Spring"));
        assertNotNull(response.getLikeDto());
        assertEquals(10L, response.getLikeDto().getLikeCount());
        assertTrue(response.getLikeDto().getIsLiked());
        assertTrue(response.getIsApplication());
        assertEquals(ApplicationStatus.PENDING, response.getApplicationStatus());
    }

    @Test
    void getMeetingDetail_withLikeInfo() {
        Long meetingId = meeting.getId();

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(likeRepository.countByMeeting(meeting)).thenReturn(10L);
        when(likeRepository.existsByMeetingAndUserUsername(meeting, applicant.getUsername())).thenReturn(true);

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(applicant.getUsername(), meetingId);

        assertNotNull(response.getLikeDto());
        assertEquals(10L, response.getLikeDto().getLikeCount());
        assertTrue(response.getLikeDto().getIsLiked());
    }

    @Test
    void getConfirmedMeetingDetail_Success_Creator() {
        Long meetingId = confirmedMeeting.getId();
        String username = creator.getUsername();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(confirmedMeeting));

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(username, meetingId);

        assertEquals("Creator", response.getUserType());
    }

    @Test
    void getConfirmedMeetingDetail_userIsApplicant() {
        Long meetingId = confirmedMeeting.getId();

        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(
                applicant,
                meeting,
                applicationRequestDto);

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(confirmedMeeting));
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(applicationRepository.findByApplicantAndMeeting(applicant, confirmedMeeting)).thenReturn(Optional.of(applicationEntity));

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(applicant.getUsername(), meetingId);

        assertTrue(response.getIsApplication());
        assertEquals(ApplicationStatus.PENDING, response.getApplicationStatus());
    }

    @Test
    void getConfirmedMeetingDetail_userIsNotApplicant() {
        Long meetingId = confirmedMeeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(confirmedMeeting));
        when(userRepository.findByUsername(failedApplicant.getUsername())).thenReturn(Optional.of(failedApplicant));
        when(applicationRepository.findByApplicantAndMeeting(failedApplicant, confirmedMeeting)).thenReturn(Optional.empty());

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(failedApplicant.getUsername(), meetingId);

        assertFalse(response.getIsApplication());
    }

    @Test
    void getConfirmedMeetingDetail_userIsAnonymous() {
        Long meetingId = confirmedMeeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(confirmedMeeting));

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(null, meetingId);

        assertEquals("Anonymous", response.getUserType());
    }


    @Test
    void updateMeeting_whenUserIsNotCreator_shouldThrowException() {
        Long meetingId = meeting.getId();
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));

        assertThrows(CreatorAuthorizationException.class, () -> {
            meetingService.updateMeeting(applicant.getUsername(), meetingId, buildUpdateMeetingRequestDto());
        });
    }

    @Test
    void updateMeeting_withExistingTechStacks_shouldUpdateTechStacks() {
        Long meetingId = meeting.getId();

        TechStackEntity techStack1 = new TechStackEntity(1L, "Java", TechCategory.BACKEND);
        TechStackEntity techStack2 = new TechStackEntity(2L, "Spring", TechCategory.BACKEND);

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(techStackRepository.findAllById(anyList())).thenReturn(List.of(techStack1, techStack2));

        MeetingResponseDto response = meetingService.updateMeeting(creator.getUsername(), meetingId, buildUpdateMeetingRequestDto());

        assertNotNull(response);
        assertTrue(response.getTechStacks().contains("Java"));
        assertTrue(response.getTechStacks().contains("Spring"));
        verify(meetingTechStackRepository, times(1)).saveAll(anyList());
    }

    @Test
    void updateMeeting_withNewTechStacks_shouldAddNewTechStacks() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));

        MeetingResponseDto response = meetingService.updateMeeting(creator.getUsername(), meetingId, buildUpdateMeetingRequestDto());

        assertNotNull(response);
        assertTrue(response.getTechStacks().contains("GIT"));
        verify(meetingTechStackRepository, times(1)).save(any());
    }

    @Test
    void updateMeeting_withoutTechStacks_shouldNotUpdateTechStacks() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(techStackRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

        UpdateMeetingRequestDto requestDto = new UpdateMeetingRequestDto(
                RecruitmentType.PROJECT, 10, Duration.THREE_MONTHS,
                LocalDate.now().plusMonths(4), "서울 마포구",
                37.5555, 126.9999, "Updated Title",
                "업데이트된 설명", "새로운 목표", "업데이트된 기타 정보",
                Collections.emptyList(), Collections.emptyList()
        );
        MeetingResponseDto response = meetingService.updateMeeting(creator.getUsername(), meetingId, requestDto);

        assertNotNull(response);
        assertTrue(response.getTechStacks().isEmpty());
        verify(meetingTechStackRepository, times(0)).save(any());
    }

    @Test
    void updateMeeting_shouldReturnUpdatedTechStacks() {
        Long meetingId = meeting.getId();

        TechStackEntity techStack1 = new TechStackEntity(1L, "Java", TechCategory.BACKEND);
        TechStackEntity techStack2 = new TechStackEntity(2L, "Spring", TechCategory.BACKEND);

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(techStackRepository.findAllById(anyList())).thenReturn(List.of(techStack1, techStack2));

        MeetingResponseDto response = meetingService.updateMeeting(creator.getUsername(), meetingId, buildUpdateMeetingRequestDto());

        assertNotNull(response);
        assertTrue(response.getTechStacks().contains("Java"));
        assertTrue(response.getTechStacks().contains("Spring"));
        assertEquals("Updated Title", response.getTitle());
    }

    @Test
    void deleteMeeting_shouldDeleteMeetingAndRelatedData_whenCreatorDeletes() {
        when(meetingRepository.findWithCreatorById(meeting.getId())).thenReturn(Optional.of(meeting));

        meetingService.deleteMeeting(creator.getUsername(), meeting.getId());

        verify(meetingTechStackRepository).deleteByMeeting(meeting);
        verify(likeRepository).deleteByMeeting(meeting);
        verify(meetingRepository).delete(meeting);
    }

    @Test
    void deleteMeeting_shouldThrowException_whenStudyConfirmed() {
        when(meetingRepository.findWithCreatorById(confirmedMeeting.getId())).thenReturn(Optional.of(confirmedMeeting));

        assertThrows(StudyConfirmedException.class, () -> {
            meetingService.deleteMeeting(creator.getUsername(), confirmedMeeting.getId());
        });
    }

    @Test
    void deleteMeeting_shouldThrowException_whenNotMeetingCreator() {
        when(meetingRepository.findWithCreatorById(meeting.getId())).thenReturn(Optional.of(meeting));

        assertThrows(CreatorAuthorizationException.class, () -> {
            meetingService.deleteMeeting(applicant.getUsername(), meeting.getId());
        });
    }

    @Test
    void deleteMeeting_shouldThrowException_whenMeetingNotFound() {
        when(meetingRepository.findWithCreatorById(999L)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () -> {
            meetingService.deleteMeeting(creator.getUsername(), 999L);
        });
    }

    @Test
    void confirmMeetingMember_shouldThrowException_whenUserIsNotCreator() {
        when(meetingRepository.findWithCreatorById(meeting.getId())).thenReturn(Optional.of(meeting));
        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(List.of(1L));

        assertThrows(CreatorAuthorizationException.class, () -> {
            meetingService.confirmMeetingMember(applicant.getUsername(), meeting.getId(), requestDto);
        });
    }

    @Test
    void confirmMeetingMember_shouldThrowException_whenInvalidApplicationIds() {
        List<Long> invalidApplicationIds = List.of(999L, 1000L);
        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(invalidApplicationIds);

        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(
                applicant,
                meeting,
                applicationRequestDto);
        when(meetingRepository.findWithCreatorById(meeting.getId())).thenReturn(Optional.of(meeting));
        when(applicationRepository.findByMeeting(meeting)).thenReturn(List.of(applicationEntity));

        assertThrows(InvalidApplicationIdException.class, () -> {
            meetingService.confirmMeetingMember(creator.getUsername(), meeting.getId(), requestDto);
        });
    }


    @Test
    void confirmMeetingMember_shouldThrowException_whenMeetingNotFound() {
        List<Long> invalidApplicationIds = List.of(999L, 1000L);
        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(invalidApplicationIds);

        assertThrows(MeetingNotFoundException.class, () -> {
            meetingService.confirmMeetingMember(creator.getUsername(), 9999L, requestDto);
        });
    }

    @Test
    void confirmMeetingMember_shouldUpdateApplicationStatus() {
        Long validApplicationId = 1L;
        List<Long> validApplicationIds = List.of(validApplicationId);
        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(validApplicationIds);

        ApplicationEntity applicationEntity = ApplicationEntity.builder()
                .id(validApplicationId)
                .status(ApplicationStatus.PENDING)
                .applicant(applicant)
                .build();

        when(meetingRepository.findWithCreatorById(meeting.getId())).thenReturn(Optional.of(meeting));
        when(applicationRepository.findByMeeting(meeting)).thenReturn(List.of(applicationEntity));

        List<ConfirmMeetingResponseDto> result = meetingService.confirmMeetingMember(creator.getUsername(), meeting.getId(), requestDto);

        assertEquals(2, result.size());
        assertEquals(ApplicationStatus.APPROVED, applicationEntity.getStatus());
    }

    @Test
    void getConfirmedMembers_meetingNotFound_shouldThrowException() {
        Long meetingId = 9999L;
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () -> meetingService.getConfirmedMembers(creator.getUsername(), meetingId));
    }

    @Test
    void getConfirmedMembers_userNotFound_shouldThrowException() {
        Long meetingId = confirmedMeeting.getId();
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(confirmedMeeting));
        when(userRepository.findByUsername("invalidUser")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> meetingService.getConfirmedMembers("invalidUser", meetingId));
    }

    @Test
    void getConfirmedMembers_applicationNotFound_shouldThrowException() {
        Long meetingId = confirmedMeeting.getId();
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(confirmedMeeting));
        when(userRepository.findByUsername(failedApplicant.getUsername())).thenReturn(Optional.of(failedApplicant));

        assertThrows(ApplicationNotFoundException.class, () -> meetingService.getConfirmedMembers(failedApplicant.getUsername(), meetingId));
    }

    @Test
    public void testGetConfirmedMembers_NoConfirmedApplicants() {
        Long meetingId = confirmedMeeting.getId();

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(confirmedMeeting));
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(applicationRepository.findByMeetingAndStatus(confirmedMeeting, ApplicationStatus.APPROVED))
                .thenReturn(Collections.emptyList());

        List<ConfirmMeetingResponseDto> actualResponse = meetingService.getConfirmedMembers(creator.getUsername(), meetingId);

        assertEquals(1, actualResponse.size());
    }

    @Test
    public void testGetConfirmedMembers_SuccessForCreator() {
        Long meetingId = confirmedMeeting.getId();
        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(
                applicant,
                meeting,
                applicationRequestDto);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(confirmedMeeting));
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(applicationRepository.findByMeetingAndStatus(confirmedMeeting, ApplicationStatus.APPROVED))
                .thenReturn(Arrays.asList(applicationEntity));

        List<ConfirmMeetingResponseDto> expectedResponse = new ArrayList<>();
        expectedResponse.add(ConfirmMeetingResponseDto.fromEntity(creator));

        List<ConfirmMeetingResponseDto> actualResponse = meetingService.getConfirmedMembers(creator.getUsername(), meetingId);

        assertEquals(expectedResponse.size() + 1, actualResponse.size());
    }

    private UpdateMeetingRequestDto buildUpdateMeetingRequestDto() {
        UpdateMeetingRequestDto requestDto = new UpdateMeetingRequestDto(
                RecruitmentType.PROJECT, 10, Duration.THREE_MONTHS,
                LocalDate.now().plusMonths(4), "서울 마포구",
                37.5555, 126.9999, "Updated Title",
                "업데이트된 설명", "새로운 목표", "업데이트된 기타 정보",
                List.of(1L), List.of("GIT")
        );
        return requestDto;
    }
}