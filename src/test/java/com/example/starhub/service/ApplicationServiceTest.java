package com.example.starhub.service;

import com.example.starhub.dto.request.ApplicationRequestDto;
import com.example.starhub.dto.response.ApplicationResponseDto;
import com.example.starhub.entity.ApplicationEntity;
import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.exception.*;
import com.example.starhub.repository.ApplicationRepository;
import com.example.starhub.repository.MeetingRepository;
import com.example.starhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @InjectMocks
    private ApplicationService applicationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    private UserEntity creator;
    private UserEntity applicant;
    private MeetingEntity meeting;

    @BeforeEach
    void setUp() {
        creator = UserEntity.createUser("creatorUser", "creatorPassword");
        applicant = UserEntity.createUser("applicantUser", "applicantPassword");

        meeting = MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(false)
                .build();

    }

    @Test
    public void testCreateApplication_Success() {
        Long meetingId = 1L;
        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(applicationRepository.existsByMeetingAndApplicant(meeting, applicant)).thenReturn(false);
        when(applicationRepository.save(any(ApplicationEntity.class))).thenReturn(ApplicationEntity.createApplication(
                applicant,
                meeting,
                applicationRequestDto
        ));

        ApplicationResponseDto response = applicationService.createApplication(applicant.getUsername(), meetingId, applicationRequestDto);

        assertNotNull(response);
        verify(applicationRepository, times(1)).save(any(ApplicationEntity.class));
    }


    @Test
    void testCreateApplication_MeetingCreatorCannotApply() {
        Long meetingId = meeting.getId();
        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");

        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));

        assertThrows(MeetingCreatorCannotApplyException.class, () -> {
            applicationService.createApplication(creator.getUsername(), meetingId, applicationRequestDto);
        });
    }

    @Test
    void testCreateApplication_DuplicateApplication() {
        String username = "applicantUser";
        Long meetingId = meeting.getId();
        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(applicant));
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(applicationRepository.existsByMeetingAndApplicant(meeting, applicant)).thenReturn(true);

        assertThrows(DuplicateApplicationException.class, () -> {
            applicationService.createApplication(username, meetingId, applicationRequestDto);
        });
    }

    @Test
    public void testCreateApplication_MeetingConfirmed() {
        Long meetingId = meeting.getId();
        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");

        MeetingEntity confirmedMeeting = MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(true)
                .build(); // 이미 확정된 상태

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(confirmedMeeting));

        assertThrows(StudyConfirmedException.class, () ->
                applicationService.createApplication(applicant.getUsername(), meetingId, applicationRequestDto));
    }

    @Test
    public void testCreateApplication_UserNotFound() {
        String username = "nonexistentUser";
        Long meetingId = meeting.getId();
        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                applicationService.createApplication(username, meetingId, applicationRequestDto));
    }

    @Test
    public void testGetApplicationList_Success() {
        Long meetingId = meeting.getId();

        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(
                applicant,
                meeting,
                applicationRequestDto);

        List<ApplicationEntity> applicationEntities = List.of(applicationEntity);

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(applicationRepository.findByMeeting(meeting)).thenReturn(applicationEntities);

        List<ApplicationResponseDto> response = applicationService.getApplicationList(creator.getUsername(), meetingId);

        assertNotNull(response);
        assertEquals(1, response.size());
        verify(applicationRepository, times(1)).findByMeeting(meeting);
    }


    @Test
    public void testGetApplicationList_NotMeetingCreator() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));

        assertThrows(CreatorAuthorizationException.class, () ->
                applicationService.getApplicationList(applicant.getUsername(), meetingId));
    }

    @Test
    public void testGetApplicationList_NoApplications() {
        Long meetingId = meeting.getId();
        List<ApplicationEntity> applicationEntities = new ArrayList<>();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(applicationRepository.findByMeeting(meeting)).thenReturn(applicationEntities);

        List<ApplicationResponseDto> response = applicationService.getApplicationList(creator.getUsername(), meetingId);

        assertNotNull(response);
        assertTrue(response.isEmpty());
        verify(applicationRepository, times(1)).findByMeeting(meeting);
    }

    @Test
    public void testGetApplicationList_MeetingNotFound() {
        String username = "creator";
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () ->
                applicationService.getApplicationList(username, meetingId));
    }

    @Test
    public void testGetApplicationDetail_Success() {
        Long meetingId = meeting.getId();
        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(
                applicant,
                meeting,
                applicationRequestDto);

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(applicationRepository.findByApplicantAndMeeting(applicant, meeting)).thenReturn(Optional.of(applicationEntity));

        ApplicationResponseDto response = applicationService.getApplicationDetail(applicant.getUsername(), meetingId);

        assertNotNull(response);
        assertEquals(applicant.getNickname(), response.getApplicant().getNickname());
        assertEquals(meetingId, response.getId());
        verify(applicationRepository, times(1)).findByApplicantAndMeeting(applicant, meeting);
    }

    @Test
    public void testGetApplicationDetail_NotApplicant() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        assertThrows(ApplicantAuthorizationException.class, () ->
                applicationService.getApplicationDetail(creator.getUsername(), meetingId));
    }

    @Test
    public void testGetApplicationDetail_ApplicationNotFound() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(applicationRepository.findByApplicantAndMeeting(applicant, meeting)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () ->
                applicationService.getApplicationDetail(applicant.getUsername(), meetingId));
    }

    @Test
    public void testGetApplicationDetail_MeetingNotFound() {
        Long meetingId = meeting.getId();

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () ->
                applicationService.getApplicationDetail(applicant.getUsername(), meetingId));
    }

    @Test
    public void testUpdateApplication_Success() {
        Long meetingId = meeting.getId();
        ApplicationRequestDto applicationRequestDto = new ApplicationRequestDto("Application content");
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(
                applicant,
                meeting,
                applicationRequestDto);

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(applicationRepository.findByApplicantAndMeeting(applicant, meeting)).thenReturn(Optional.of(applicationEntity));

        ApplicationResponseDto response = applicationService.updateApplication(applicant.getUsername(), meetingId, applicationRequestDto);

        assertNotNull(response);
        assertEquals("Application content", response.getContent());
    }

    @Test
    public void testUpdateApplication_NotApplicant() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        assertThrows(ApplicantAuthorizationException.class, () ->
                applicationService.updateApplication(creator.getUsername(), meetingId, new ApplicationRequestDto("Updated content")));
    }

    @Test
    public void testUpdateApplication_ApplicationNotFound() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(applicationRepository.findByApplicantAndMeeting(applicant, meeting)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () ->
                applicationService.updateApplication(applicant.getUsername(), meetingId, new ApplicationRequestDto("Updated content")));
    }

    @Test
    public void testUpdateApplication_MeetingNotFound() {
        Long meetingId = meeting.getId();

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () ->
                applicationService.updateApplication(applicant.getUsername(), meetingId, new ApplicationRequestDto("Updated content")));
    }

    @Test
    public void testDeleteApplication_Success() {
        Long meetingId = meeting.getId();
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(applicant, meeting, new ApplicationRequestDto("Some content"));

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(applicationRepository.findByApplicantAndMeeting(applicant, meeting)).thenReturn(Optional.of(applicationEntity));

        applicationService.deleteApplication(applicant.getUsername(), meetingId);

        verify(applicationRepository, times(1)).delete(applicationEntity);
    }

    @Test
    public void testDeleteApplication_ApplicationNotFound() {
        Long meetingId = meeting.getId();

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(applicationRepository.findByApplicantAndMeeting(applicant, meeting)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> {
            applicationService.deleteApplication(applicant.getUsername(), meetingId);
        });
    }

    @Test
    public void testDeleteApplication_MeetingCreatorCannotApply() {
        Long meetingId = meeting.getId();
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(creator, meeting, new ApplicationRequestDto("Some content"));

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(creator.getUsername())).thenReturn(Optional.of(creator));

        assertThrows(ApplicantAuthorizationException.class, () -> {
            applicationService.deleteApplication(creator.getUsername(), meetingId);
        });
    }

    @Test
    public void testDeleteApplication_UserNotFound() {
        Long meetingId = meeting.getId();
        String nonExistentUsername = "nonexistent";

        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            applicationService.deleteApplication(nonExistentUsername, meetingId);
        });
    }

    @Test
    public void testDeleteApplication_MeetingNotFound() {
        Long nonExistentMeetingId = 999L;

        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(meetingRepository.findWithCreatorById(nonExistentMeetingId)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () -> {
            applicationService.deleteApplication(applicant.getUsername(), nonExistentMeetingId);
        });
    }

    @Test
    public void testDeleteApplication_DatabaseError() {
        Long meetingId = meeting.getId();
        ApplicationEntity applicationEntity = ApplicationEntity.createApplication(applicant, meeting, new ApplicationRequestDto("Some content"));

        when(meetingRepository.findWithCreatorById(meetingId)).thenReturn(Optional.of(meeting));
        when(userRepository.findByUsername(applicant.getUsername())).thenReturn(Optional.of(applicant));
        when(applicationRepository.findByApplicantAndMeeting(applicant, meeting)).thenReturn(Optional.of(applicationEntity));

        doThrow(new RuntimeException("Database error")).when(applicationRepository).delete(applicationEntity);

        assertThrows(RuntimeException.class, () -> {
            applicationService.deleteApplication(applicant.getUsername(), meetingId);
        });
    }

}