package com.example.starhub.integration;

import com.example.starhub.dto.request.*;
import com.example.starhub.dto.response.ApplicationResponseDto;
import com.example.starhub.dto.response.ConfirmMeetingResponseDto;
import com.example.starhub.dto.response.MeetingDetailResponseDto;
import com.example.starhub.dto.response.MeetingResponseDto;
import com.example.starhub.entity.TechStackEntity;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.entity.enums.ApplicationStatus;
import com.example.starhub.entity.enums.Duration;
import com.example.starhub.entity.enums.RecruitmentType;
import com.example.starhub.entity.enums.TechCategory;
import com.example.starhub.exception.*;
import com.example.starhub.repository.*;
import com.example.starhub.service.ApplicationService;
import com.example.starhub.service.LikeService;
import com.example.starhub.service.MeetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MeetingIntegrationTest {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private TechStackRepository techStackRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MeetingTechStackRepository meetingTechStackRepository;

    private UserEntity creator; // 개설자
    private UserEntity applicant; // 지원자
    private UserEntity failedApplicant; // 모임 거절된 지원자
    List<TechStackEntity> techStackList;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        creator = userRepository.save(UserEntity.createUser("creatorUser", "creatorPassword"));
        applicant = userRepository.save(UserEntity.createUser("applicantUser", "applicantPassword"));
        failedApplicant = userRepository.save(UserEntity.createUser("failedApplicantUser", "failedApplicantPassword"));

        // 테스트용 데이터 초기화
        techStackRepository.deleteAll();

        // 기본 데이터 추가
        saveTechStack("React", TechCategory.FRONTEND);
        saveTechStack("Spring", TechCategory.BACKEND);
        saveTechStack("React Native", TechCategory.MOBILE);
        saveTechStack("OtherTool", TechCategory.OTHER);

        techStackList = techStackRepository.findAll();
    }

    @Test
    void createMeeting_Success() {
        CreateMeetingRequestDto requestDto = buildCreateMeetingRequestDto();

        MeetingResponseDto response = meetingService.createMeeting(creator.getUsername(), requestDto);

        assertNotNull(response);
        assertEquals(requestDto.getTitle(), response.getTitle());
        assertEquals(requestDto.getTechStackIds().size() + requestDto.getOtherTechStacks().size(), response.getTechStacks().size());
    }

    @Test
    void createMeeting_shouldThrowUserNotFoundException() {
        CreateMeetingRequestDto requestDto = buildCreateMeetingRequestDto();

        assertThrows(UserNotFoundException.class, () -> {
            meetingService.createMeeting("invalidUser", requestDto);
        });
    }

    @Test
    void getMeetingDetail_Success_Creator() {
        CreateMeetingRequestDto request = buildCreateMeetingRequestDto();
        MeetingResponseDto meetingResponse = meetingService.createMeeting(creator.getUsername(), request);

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(creator.getUsername(), meetingResponse.getId());

        assertNotNull(response);
        assertEquals("Creator", response.getUserType());
        assertFalse(response.getPostInfo().getIsConfirmed());
        assertEquals(request.getTechStackIds().size() + request.getOtherTechStacks().size(), response.getPostInfo().getTechStacks().size());
    }

    @Test
    void getMeetingDetail_WithApplication_Success() {
        MeetingResponseDto meetingResponse = saveMeeting();
        saveApplication(meetingResponse, applicant.getUsername());

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(applicant.getUsername(), meetingResponse.getId());

        assertNotNull(response);
        assertEquals("Applicant", response.getUserType());
        assertTrue(response.getIsApplication());
        assertEquals(ApplicationStatus.PENDING, response.getApplicationStatus());
        assertFalse(response.getPostInfo().getIsConfirmed());
    }

    @Test
    void getMeetingDetail_WithoutApplication_Success() {
        MeetingResponseDto meetingResponse = saveMeeting();

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(applicant.getUsername(), meetingResponse.getId());

        assertNotNull(response);
        assertEquals("Applicant", response.getUserType());
        assertFalse(response.getIsApplication());
        assertFalse(response.getPostInfo().getIsConfirmed());
    }

    @Test
    void getMeetingDetail_Success_Anonymous() {
        MeetingResponseDto meetingResponse = saveMeeting();

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(null, meetingResponse.getId());

        assertNotNull(response);
        assertEquals("Anonymous", response.getUserType());
        assertFalse(response.getPostInfo().getIsConfirmed());
    }

    @Test
    void getMeetingDetail_ShouldThrowMeetingNotFoundException() {
        Long invalidMeetingId = 9999L;

        assertThrows(MeetingNotFoundException.class, () -> {
            meetingService.getMeetingDetail(creator.getUsername(), invalidMeetingId);
        });
    }

    @Test
    void getMeetingDetail_NoTechStacks() {
        CreateMeetingRequestDto requestDto = new CreateMeetingRequestDto(
                RecruitmentType.STUDY, 5, Duration.ONE_MONTH,
                LocalDate.now().plusMonths(2), "서울 강남구",
                37.5665, 126.9780, "백엔드 스터디",
                "스프링과 JPA 학습", "포트폴리오 제작", "기타 정보",
                List.of(), List.of() // 기술 스택 없음
        );
        MeetingResponseDto meetingResponse = meetingService.createMeeting(creator.getUsername(), requestDto);

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(creator.getUsername(), meetingResponse.getId());

        assertNotNull(response);
        assertEquals(0, response.getPostInfo().getTechStacks().size());
    }

    @Test
    void getMeetingDetail_LikedMeeting() {
        MeetingResponseDto meetingResponse = saveMeeting();

        likeService.createLike(applicant.getUsername(), meetingResponse.getId());

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(applicant.getUsername(), meetingResponse.getId());

        assertNotNull(response);
        assertTrue(response.getLikeDto().getIsLiked());
    }

    @Test
    void getMeetingDetail_NotLikedMeeting() {
        MeetingResponseDto meetingResponse = saveMeeting();

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(applicant.getUsername(), meetingResponse.getId());

        assertNotNull(response);
        assertFalse(response.getLikeDto().getIsLiked());
    }

    @Test
    void getConfirmedMeetingDetail_Success_Creator() {
        MeetingResponseDto meetingResponse = confirmingMeeting();

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(creator.getUsername(), meetingResponse.getId());

        assertNotNull(response);
        assertEquals("Creator", response.getUserType());
        assertTrue(response.getPostInfo().getIsConfirmed());
    }

    @Test
    void getConfirmedMeetingDetail_WithApplication_Success() {
        MeetingResponseDto meetingResponse = confirmingMeeting();

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(applicant.getUsername(), meetingResponse.getId());

        assertNotNull(response);
        assertEquals("Applicant", response.getUserType());
        assertTrue(response.getIsApplication());
        assertEquals(ApplicationStatus.APPROVED, response.getApplicationStatus());
        assertTrue(response.getPostInfo().getIsConfirmed());
    }

    @Test
    void getConfirmedMeetingDetail_WithOutApplication_Success() {
        MeetingResponseDto meetingResponse = saveMeeting();
        ApplicationResponseDto applicationResponse = saveApplication(meetingResponse, applicant.getUsername());
        saveApplication(meetingResponse, failedApplicant.getUsername());

        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(
                List.of(applicationResponse.getId())
        );
        meetingService.confirmMeetingMember(creator.getUsername(), meetingResponse.getId(), requestDto);

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(failedApplicant.getUsername(), meetingResponse.getId());

        assertNotNull(response);
        assertEquals("Applicant", response.getUserType());
        assertTrue(response.getIsApplication());
        assertEquals(ApplicationStatus.REJECTED, response.getApplicationStatus());
        assertTrue(response.getPostInfo().getIsConfirmed());
    }

    @Test
    void getConfirmedMeetingDetail_Success_Anonymous() {
        MeetingResponseDto meetingResponse = confirmingMeeting();

        MeetingDetailResponseDto response = meetingService.getMeetingDetail(null, meetingResponse.getId());

        assertNotNull(response);
        assertEquals("Anonymous", response.getUserType());
        assertTrue(response.getPostInfo().getIsConfirmed());
    }

    @Test
    void updateMeeting_Success() {

        MeetingResponseDto meetingResponse = saveMeeting();

        UpdateMeetingRequestDto updateRequest = buildUpdateMeetingRequestDto();
        MeetingResponseDto response = meetingService.updateMeeting(creator.getUsername(), meetingResponse.getId(), updateRequest);

        assertNotNull(response);
        assertEquals(updateRequest.getTitle(), response.getTitle());
        assertEquals(updateRequest.getDescription(), response.getDescription());
    }

    @Test
    void updateMeeting_shouldThrowMeetingNotFoundException() {

        UpdateMeetingRequestDto updateRequest = buildUpdateMeetingRequestDto();

        assertThrows(MeetingNotFoundException.class, () -> {
            meetingService.updateMeeting(creator.getUsername(), 9999L, updateRequest);
        });
    }

    @Test
    void updateMeeting_shouldThrowUnauthorizedException() {

        MeetingResponseDto meetingResponse = saveMeeting();

        UpdateMeetingRequestDto requestDto = buildUpdateMeetingRequestDto();

        assertThrows(CreatorAuthorizationException.class, () -> {
            meetingService.updateMeeting(applicant.getUsername(), meetingResponse.getId(), requestDto);
        });
    }

    @Test
    void updateMeeting_shouldThrowStudyConfirmedException() {
        MeetingResponseDto meetingResponse = confirmingMeeting();
        UpdateMeetingRequestDto requestDto = buildUpdateMeetingRequestDto();

        assertThrows(StudyConfirmedException.class, () -> {
            meetingService.updateMeeting(creator .getUsername(), meetingResponse.getId(), requestDto);
        });
    }

    @Test
    void deleteMeeting_Success() {

        MeetingResponseDto meetingResponse = saveMeeting();

        meetingService.deleteMeeting(creator.getUsername(), meetingResponse.getId());

        assertFalse(meetingRepository.findById(meetingResponse.getId()).isPresent());
    }

    @Test
    void deleteMeeting_shouldThrowMeetingNotFoundException() {

        assertThrows(MeetingNotFoundException.class, () -> {
            meetingService.deleteMeeting(creator.getUsername(), 9999L);
        });
    }

    @Test
    void deleteMeeting_shouldThrowUnauthorizedException() {
        MeetingResponseDto meetingResponse = saveMeeting();

        assertThrows(CreatorAuthorizationException.class, () -> {
            meetingService.deleteMeeting(applicant.getUsername(), meetingResponse.getId());
        });
    }

    @Test
    void deleteMeeting_shouldThrowStudyConfirmedException() {
        MeetingResponseDto meetingResponse = confirmingMeeting();

        assertThrows(StudyConfirmedException.class, () -> {
            meetingService.deleteMeeting(creator.getUsername(), meetingResponse.getId());
        });
    }

    @Test
    void confirmMeetingMember_Success() {

        MeetingResponseDto meetingResponse = saveMeeting();
        ApplicationResponseDto applicationResponse = saveApplication(meetingResponse, applicant.getUsername());

        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(
                List.of(applicationResponse.getId())
        );
        List<ConfirmMeetingResponseDto> response = meetingService.confirmMeetingMember(creator.getUsername(), meetingResponse.getId(), requestDto);

        assertEquals(2, response.size());
        assertEquals(ApplicationStatus.APPROVED, applicationRepository.findById(applicationResponse.getId()).get().getStatus());
        assertTrue(meetingRepository.findById(meetingResponse.getId()).get().getIsConfirmed());
    }

    @Test
    void confirmMeetingMember_shouldThrowMeetingNotFoundException() {

        MeetingResponseDto meetingResponse = saveMeeting();
        ApplicationResponseDto applicationResponse = saveApplication(meetingResponse, applicant.getUsername());

        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(List.of(applicationResponse.getId()));

        assertThrows(MeetingNotFoundException.class, () -> {
            meetingService.confirmMeetingMember(creator.getUsername(), 9999L, requestDto);
        });
    }

    @Test
    void confirmMeetingMember_shouldThrowUnauthorizedException() {

        MeetingResponseDto meetingResponse = saveMeeting();
        ApplicationResponseDto responseDto = saveApplication(meetingResponse, applicant.getUsername());

        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(List.of(responseDto.getId()));

        assertThrows(CreatorAuthorizationException.class, () -> {
            meetingService.confirmMeetingMember(applicant.getUsername(), meetingResponse.getId(), requestDto);
        });
    }

    @Test
    void confirmMeetingMember_shouldInvalidApplicationIdException() {

        MeetingResponseDto meetingResponse = saveMeeting();
        saveApplication(meetingResponse, applicant.getUsername());

        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(List.of(9999L));

        assertThrows(InvalidApplicationIdException.class, () -> {
            meetingService.confirmMeetingMember(creator.getUsername(), meetingResponse.getId(), requestDto);
        });
    }


    @Test
    void getConfirmedMembers_Success_AsCreator() {

        MeetingResponseDto meetingResponse = confirmingMeeting();

        List<ConfirmMeetingResponseDto> response = meetingService.getConfirmedMembers(creator.getUsername(), meetingResponse.getId());
        assertEquals(2, response.size());
        assertEquals(creator.getName(), response.get(0).getName());
        assertEquals(applicant.getName(), response.get(1).getName());
    }

    @Test
    void getConfirmedMembers_Success_AsApplicant() {

        MeetingResponseDto meetingResponse = confirmingMeeting();

        List<ConfirmMeetingResponseDto> response = meetingService.getConfirmedMembers(applicant.getUsername(), meetingResponse.getId());
        assertEquals(2, response.size());
        assertEquals(creator.getName(), response.get(0).getName());
        assertEquals(applicant.getName(), response.get(1).getName());
    }

    @Test
    void getConfirmedMembers_shouldThrowApplicationNotFoundException() {
        MeetingResponseDto meetingResponse = confirmingMeeting();

        assertThrows(ApplicationNotFoundException.class, () -> {
            meetingService.getConfirmedMembers(failedApplicant.getUsername(), meetingResponse.getId());
        });
    }

    @Test
    void getConfirmedMembers_shouldThrowUserNotFoundException() {
        MeetingResponseDto meetingResponse = confirmingMeeting();

        assertThrows(UserNotFoundException.class, () -> {
            meetingService.getConfirmedMembers("invalidUser", meetingResponse.getId());
        });
    }

    @Test
    void getConfirmedMembers_shouldThrowStudyNotConfirmedException() {
        MeetingResponseDto meetingResponse = saveMeeting();

        assertThrows(StudyNotConfirmedException.class, () -> {
            meetingService.getConfirmedMembers(creator.getUsername(), meetingResponse.getId());
        });
    }

    @Test
    void getConfirmedMembers_WhenApplicantIsNotConfirmed() {
        MeetingResponseDto meetingResponse = saveMeeting();
        saveApplication(meetingResponse, applicant.getUsername());

        assertThrows(StudyNotConfirmedException.class, () -> {
            meetingService.getConfirmedMembers(creator.getUsername(), meetingResponse.getId());
        });
    }

    private ApplicationResponseDto saveApplication(MeetingResponseDto meetingResponse, String applicant) {
        ApplicationRequestDto applicationRequestDto = buildApplicationRequestDto();
        ApplicationResponseDto responseDto = applicationService.createApplication(applicant, meetingResponse.getId(), applicationRequestDto);
        return responseDto;
    }

    private MeetingResponseDto saveMeeting() {
        CreateMeetingRequestDto createMeetingRequestDto = buildCreateMeetingRequestDto();
        MeetingResponseDto meetingResponse = meetingService.createMeeting(creator.getUsername(), createMeetingRequestDto);
        return meetingResponse;
    }

    private UpdateMeetingRequestDto buildUpdateMeetingRequestDto() {
        UpdateMeetingRequestDto requestDto = new UpdateMeetingRequestDto(
                RecruitmentType.PROJECT, 10, Duration.THREE_MONTHS,
                LocalDate.now().plusMonths(4), "서울 마포구",
                37.5555, 126.9999, "업데이트된 모임",
                "업데이트된 설명", "새로운 목표", "업데이트된 기타 정보",
                List.of(techStackList.get(1).getId()), List.of("GIT")
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

    private CreateMeetingRequestDto buildCreateMeetingRequestDto() {
        CreateMeetingRequestDto requestDto = new CreateMeetingRequestDto(
                RecruitmentType.STUDY, 5, Duration.ONE_MONTH,
                LocalDate.now().plusMonths(2), "서울 강남구",
                37.5665, 126.9780, "백엔드 스터디",
                "스프링과 JPA 학습", "포트폴리오 제작", "기타 정보", List.of(techStackList.get(0).getId()), List.of("GraphQL")
        );
        return requestDto;
    }

    private ApplicationRequestDto buildApplicationRequestDto() {
        ApplicationRequestDto requestDto = ApplicationRequestDto.builder()
                .content("This is a test application.")
                .build();
        return requestDto;
    }

    private MeetingResponseDto confirmingMeeting() {
        MeetingResponseDto meetingResponse = saveMeeting();
        ApplicationResponseDto applicationResponse = saveApplication(meetingResponse, applicant.getUsername());

        ConfirmMeetingRequestDto requestDto = new ConfirmMeetingRequestDto(
                List.of(applicationResponse.getId())
        );
        meetingService.confirmMeetingMember(creator.getUsername(), meetingResponse.getId(), requestDto);
        return meetingResponse;
    }

}