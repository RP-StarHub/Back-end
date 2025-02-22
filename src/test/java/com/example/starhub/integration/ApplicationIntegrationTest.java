package com.example.starhub.integration;

import com.example.starhub.dto.request.ApplicationRequestDto;
import com.example.starhub.dto.response.ApplicationResponseDto;
import com.example.starhub.entity.ApplicationEntity;
import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.exception.*;
import com.example.starhub.repository.ApplicationRepository;
import com.example.starhub.repository.MeetingRepository;
import com.example.starhub.repository.UserRepository;
import com.example.starhub.response.code.ErrorCode;
import com.example.starhub.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ApplicationIntegrationTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private UserEntity creator; // 개설자
    private UserEntity applicant; // 지원자
    private MeetingEntity meeting;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자와 모임 생성
        creator = userRepository.save(UserEntity.createUser("creatorUser", "creatorPassword"));
        applicant = userRepository.save(UserEntity.createUser("applicantUser", "applicantPassword"));

        meeting = meetingRepository.save(MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(false)
                .build());
    }

    @Test
    void createApplication_success() {
        ApplicationRequestDto requestDto = buildRequestDto();

        ApplicationResponseDto responseDto = applicationService.createApplication(applicant.getUsername(), meeting.getId(), requestDto);

        assertNotNull(responseDto);
        assertEquals(requestDto.getContent(), responseDto.getContent());
        assertEquals(applicant.getNickname(), responseDto.getApplicant().getNickname());
    }

    @Test
    void createApplication_shouldThrowUserNotFoundException_whenUsernameIsInvalid() {
        ApplicationRequestDto requestDto = buildRequestDto();

        assertThrows(UserNotFoundException.class, () -> {
            applicationService.createApplication("invalidUser", meeting.getId(), requestDto);
        });
    }

    @Test
    void createApplication_shouldThrowMeetingNotFoundException_whenMeetingIdIsInvalid() {
        ApplicationRequestDto requestDto = buildRequestDto();

        assertThrows(MeetingNotFoundException.class, () -> {
            applicationService.createApplication(applicant.getUsername(), 999L, requestDto);
        });
    }

    @Test
    void createApplication_shouldThrowException_whenCreatorApplies() {
        // Given
        ApplicationRequestDto requestDto = buildRequestDto();

        MeetingCreatorCannotApplyException exception = assertThrows(MeetingCreatorCannotApplyException.class, () -> {
            applicationService.createApplication(meeting.getCreator().getUsername(), meeting.getId(), requestDto);
        });

        assertEquals(ErrorCode.MEETING_CREATOR_CANNOT_APPLY, exception.getErrorCode());
    }

    @Test
    void createApplication_shouldThrowException_whenDuplicateApplicationExists() {
        ApplicationEntity application = ApplicationEntity.createApplication(applicant, meeting, ApplicationRequestDto.builder()
                .content("Existing application")
                .build());
        applicationRepository.save(application);

        ApplicationRequestDto requestDto = buildRequestDto();

        DuplicateApplicationException exception = assertThrows(DuplicateApplicationException.class, () -> {
            applicationService.createApplication(applicant.getUsername(), meeting.getId(), requestDto);
        });

        assertEquals(ErrorCode.DUPLICATE_APPLICATION, exception.getErrorCode());
    }

    @Test
    void createApplication_shouldThrowStudyConfirmedException_whenMeetingIsConfirmed() {
         MeetingEntity confirmedMeeting = meetingRepository.save(MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(true)
                .build());

        ApplicationRequestDto requestDto = buildRequestDto();

        StudyConfirmedException exception = assertThrows(StudyConfirmedException.class, () -> {
            applicationService.createApplication(applicant.getUsername(), confirmedMeeting.getId(), requestDto);
        });

        assertEquals(ErrorCode.STUDY_CONFIRMED, exception.getErrorCode());
    }

    @Test
    void getApplicationList_shouldReturnList_whenUserIsCreator() {
        saveApplicant();

        String username = creator.getUsername();
        Long meetingId = meeting.getId();

        List<ApplicationResponseDto> response = applicationService.getApplicationList(username, meetingId);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("This is a test application.", response.get(0).getContent());
    }

    @Test
    void getApplicationList_shouldThrowMeetingNotFoundException_whenMeetingDoesNotExist() {
        String username = creator.getUsername();
        Long invalidMeetingId = 999L;

        MeetingNotFoundException exception = assertThrows(MeetingNotFoundException.class, () -> {
            applicationService.getApplicationList(username, invalidMeetingId);
        });

        assertEquals(ErrorCode.MEETING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getApplicationList_shouldThrowStudyConfirmedException_whenMeetingIsConfirmed() {
        String username = creator.getUsername();
        MeetingEntity confirmedMeeting = meetingRepository.save(MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(true)
                .build());

        StudyConfirmedException exception = assertThrows(StudyConfirmedException.class, () -> {
            applicationService.getApplicationList(username, confirmedMeeting.getId());
        });

        assertEquals(ErrorCode.STUDY_CONFIRMED, exception.getErrorCode());
    }

    @Test
    void getApplicationList_shouldThrowCreatorAuthorizationException_whenUserIsNotCreator() {
        String username = applicant.getUsername(); // 지원자가 요청
        Long meetingId = meeting.getId();

        CreatorAuthorizationException exception = assertThrows(CreatorAuthorizationException.class, () -> {
            applicationService.getApplicationList(username, meetingId);
        });

        assertEquals(ErrorCode.MEETING_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void getApplicationDetail_shouldReturnApplication_whenApplicantRequests() {
        saveApplicant();

        String username = applicant.getUsername();
        Long meetingId = meeting.getId();

        ApplicationResponseDto response = applicationService.getApplicationDetail(username, meetingId);

        // Then
        assertNotNull(response);
        assertEquals("This is a test application.", response.getContent());
        assertEquals(applicant.getNickname(), response.getApplicant().getNickname());
    }

    @Test
    void getApplicationDetail_shouldThrowMeetingNotFoundException_whenMeetingDoesNotExist() {
        String username = applicant.getUsername();
        Long invalidMeetingId = 999L;

        MeetingNotFoundException exception = assertThrows(MeetingNotFoundException.class, () -> {
            applicationService.getApplicationDetail(username, invalidMeetingId);
        });

        assertEquals(ErrorCode.MEETING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getApplicationDetail_shouldThrowStudyConfirmedException_whenMeetingIsConfirmed() {
        String username = creator.getUsername();
        MeetingEntity confirmedMeeting = meetingRepository.save(MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(true)
                .build());

        StudyConfirmedException exception = assertThrows(StudyConfirmedException.class, () -> {
            applicationService.getApplicationDetail(username, confirmedMeeting.getId());
        });

        assertEquals(ErrorCode.STUDY_CONFIRMED, exception.getErrorCode());
    }

    @Test
    void getApplicationDetail_shouldThrowApplicantAuthorizationException_whenCreatorRequests() {
        String username = creator.getUsername(); // 개설자가 요청
        Long meetingId = meeting.getId();

        ApplicantAuthorizationException exception = assertThrows(ApplicantAuthorizationException.class, () -> {
            applicationService.getApplicationDetail(username, meetingId);
        });

        assertEquals(ErrorCode.APPLICATION_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void getApplicationDetail_shouldThrowApplicationNotFoundException_whenApplicationDoesNotExist() {

        String username = "nonExistingUser";
        userRepository.save(UserEntity.createUser(username, "password"));

        Long meetingId = meeting.getId();

        // When & Then
        ApplicationNotFoundException exception = assertThrows(ApplicationNotFoundException.class, () -> {
            applicationService.getApplicationDetail(username, meetingId);
        });

        assertEquals(ErrorCode.APPLICATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateApplication_shouldUpdateContent_whenApplicantRequests() {

        ApplicationEntity application = saveApplicant();

        String username = applicant.getUsername();
        Long meetingId = meeting.getId();
        ApplicationRequestDto updatedRequestDto = updatedRequestDto();

        ApplicationResponseDto response = applicationService.updateApplication(username, meetingId, updatedRequestDto);

        assertNotNull(response);
        assertEquals("This is a updated application.", response.getContent());

        ApplicationEntity updatedApplication = applicationRepository.findById(application.getId()).orElseThrow();
        assertEquals("This is a updated application.", updatedApplication.getContent());
    }

    @Test
    void updateApplication_shouldThrowMeetingNotFoundException_whenMeetingDoesNotExist() {
        // Given
        String username = applicant.getUsername();
        Long invalidMeetingId = 999L;
        ApplicationRequestDto updatedRequestDto = updatedRequestDto();

        // When & Then
        MeetingNotFoundException exception = assertThrows(MeetingNotFoundException.class, () -> {
            applicationService.updateApplication(username, invalidMeetingId, updatedRequestDto);
        });

        assertEquals(ErrorCode.MEETING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateApplication_shouldThrowStudyConfirmedException_whenMeetingIsConfirmed() {
        String username = creator.getUsername();
        MeetingEntity confirmedMeeting = meetingRepository.save(MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(true)
                .build());

        ApplicationRequestDto updatedRequestDto = updatedRequestDto();

        // When & Then
        StudyConfirmedException exception = assertThrows(StudyConfirmedException.class, () -> {
            applicationService.updateApplication(username, confirmedMeeting.getId(), updatedRequestDto);
        });

        assertEquals(ErrorCode.STUDY_CONFIRMED, exception.getErrorCode());
    }

    @Test
    void updateApplication_shouldThrowApplicantAuthorizationException_whenCreatorRequests() {
        // Given
        String username = creator.getUsername(); // 개설자가 요청
        Long meetingId = meeting.getId();
        ApplicationRequestDto updatedRequestDto = updatedRequestDto();

        // When & Then
        ApplicantAuthorizationException exception = assertThrows(ApplicantAuthorizationException.class, () -> {
            applicationService.updateApplication(username, meetingId, updatedRequestDto);
        });

        assertEquals(ErrorCode.APPLICATION_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void updateApplication_shouldThrowApplicationNotFoundException_whenApplicationDoesNotExist() {
        // Given
        String username = "nonExistingUser"; // 지원하지 않은 사용자
        userRepository.save(UserEntity.createUser(username, "password"));

        Long meetingId = meeting.getId();
        ApplicationRequestDto updatedRequestDto = updatedRequestDto();

        // When & Then
        ApplicationNotFoundException exception = assertThrows(ApplicationNotFoundException.class, () -> {
            applicationService.updateApplication(username, meetingId, updatedRequestDto);
        });

        assertEquals(ErrorCode.APPLICATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteApplication_shouldDeleteApplication_whenApplicantRequests() {

        ApplicationEntity application = saveApplicant();

        String username = applicant.getUsername();
        Long meetingId = meeting.getId();

        applicationService.deleteApplication(username, meetingId);

        assertFalse(applicationRepository.existsById(application.getId()));
    }

    @Test
    void deleteApplication_shouldThrowMeetingNotFoundException_whenMeetingDoesNotExist() {
        // Given
        String username = applicant.getUsername();
        Long invalidMeetingId = 999L;

        // When & Then
        MeetingNotFoundException exception = assertThrows(MeetingNotFoundException.class, () -> {
            applicationService.deleteApplication(username, invalidMeetingId);
        });

        assertEquals(ErrorCode.MEETING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteApplication_shouldThrowStudyConfirmedException_whenMeetingIsConfirmed() {
        String username = applicant.getUsername();
        MeetingEntity confirmedMeeting = meetingRepository.save(MeetingEntity.builder()
                .title("Test Meeting")
                .creator(creator)
                .isConfirmed(true)
                .build());

        StudyConfirmedException exception = assertThrows(StudyConfirmedException.class, () -> {
            applicationService.deleteApplication(username, confirmedMeeting.getId());
        });

        assertEquals(ErrorCode.STUDY_CONFIRMED, exception.getErrorCode());
    }

    @Test
    void deleteApplication_shouldThrowApplicantAuthorizationException_whenCreatorRequests() {
        // Given
        String username = creator.getUsername(); // 개설자가 요청
        Long meetingId = meeting.getId();

        // When & Then
        ApplicantAuthorizationException exception = assertThrows(ApplicantAuthorizationException.class, () -> {
            applicationService.deleteApplication(username, meetingId);
        });

        assertEquals(ErrorCode.APPLICATION_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void deleteApplication_shouldThrowApplicationNotFoundException_whenApplicationDoesNotExist() {
        String username = "nonExistingUser"; // 지원하지 않은 사용자
        userRepository.save(UserEntity.createUser(username, "password"));
        Long meetingId = meeting.getId();

        ApplicationNotFoundException exception = assertThrows(ApplicationNotFoundException.class, () -> {
            applicationService.deleteApplication(username, meetingId);
        });

        assertEquals(ErrorCode.APPLICATION_NOT_FOUND, exception.getErrorCode());
    }

    private ApplicationEntity saveApplicant() {
        return applicationRepository.save(ApplicationEntity.createApplication(applicant, meeting, buildRequestDto()));
    }

    private ApplicationRequestDto buildRequestDto() {
        ApplicationRequestDto requestDto = ApplicationRequestDto.builder()
                .content("This is a test application.")
                .build();
        return requestDto;
    }

    private ApplicationRequestDto updatedRequestDto() {
        ApplicationRequestDto requestDto = ApplicationRequestDto.builder()
                .content("This is a updated application.")
                .build();
        return requestDto;
    }
}