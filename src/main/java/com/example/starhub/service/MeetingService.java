package com.example.starhub.service;

import com.example.starhub.dto.request.ConfirmMeetingRequestDto;
import com.example.starhub.dto.request.CreateMeetingRequestDto;
import com.example.starhub.dto.request.UpdateMeetingRequestDto;
import com.example.starhub.dto.response.*;
import com.example.starhub.entity.*;
import com.example.starhub.entity.enums.ApplicationStatus;
import com.example.starhub.entity.enums.Duration;
import com.example.starhub.entity.enums.TechCategory;
import com.example.starhub.exception.*;
import com.example.starhub.repository.*;
import com.example.starhub.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final TechStackRepository techStackRepository;
    private final MeetingTechStackRepository meetingTechStackRepository;
    private final LikeRepository likeRepository;
    private final ApplicationRepository applicationRepository;

    /**
     * 공통 검증 로직: 게시글 가져오기 및 상태 확인
     * - 모임이 확정이 안된 상태이여야 함
     * - 개설자 정보와 같이 JOIN FETCH
     */
    private MeetingEntity validateAndGetMeeting(Long meetingId) {
        MeetingEntity meetingEntity = meetingRepository.findWithCreatorById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(ErrorCode.MEETING_NOT_FOUND));

        if (meetingEntity.getIsConfirmed()) {
            throw new StudyConfirmedException(ErrorCode.STUDY_CONFIRMED);
        }

        return meetingEntity;
    }

    /**
     * 공통 검증 로직: 게시글 가져오기 및 상태 확인
     */
    private MeetingEntity validateAndGetConfirmedMeeting(Long meetingId) {
        MeetingEntity meetingEntity = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(ErrorCode.MEETING_NOT_FOUND));

        if (!meetingEntity.getIsConfirmed()) {
            throw new StudyNotConfirmedException(ErrorCode.STUDY_NOT_CONFIRMED);
        }

        return meetingEntity;
    }

    /**
     * 공통 검증 로직: 사용자가 모임의 개설자인지 확인
     */
    private void validateMeetingCreator(MeetingEntity meetingEntity, String username) {
        if (!meetingEntity.getCreator().getUsername().equals(username)) {
            throw new CreatorAuthorizationException(ErrorCode.MEETING_FORBIDDEN);
        }
    }

    /**
     * 새로운 모임(스터디/프로젝트)를 생성하기
     *
     * @param username JWT를 통해 인증된 사용자명
     * @param createMeetingRequestDto 모임 생성에 필요한 데이터를 담고 있는 요청 DTO
     * @return 모임에 대한 응답 DTO
     */
    @Transactional
    public MeetingResponseDto createMeeting(String username, CreateMeetingRequestDto createMeetingRequestDto) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        MeetingEntity meetingEntity = MeetingEntity.createMeeting(user, createMeetingRequestDto);
        MeetingEntity savedMeeting = meetingRepository.save(meetingEntity);

        // 기술 스택 정보를 처리하여 모임와 연결
        List<String> techStackNames = saveMeetingTechStacks(savedMeeting, createMeetingRequestDto);

        return MeetingResponseDto.fromEntity(savedMeeting, techStackNames);
    }

    /**
     * 모임 목록 불러오기 (메인 화면에 쓰일 API)
     * - 모임 요약 정보가 담긴 목록으로 제공
     * - 페이지네이션을 적용하고, 생성일 기준 내림차순으로 정렬
     *
     * @return 모임 목록 응답 DTO
     */
    public Page<MeetingSummaryResponseDto> searchMeetings(String username, String title, Integer minParticipants, Integer maxParticipants,
                                              List<String> techStacks, String location, Duration duration,
                                              Double minLatitude, Double maxLatitude, Double minLongitude, Double maxLongitude,
                                              int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MeetingEntity> meetingPage = meetingRepository.searchMeetings(
                title, minParticipants, maxParticipants,
                techStacks, location, duration, minLatitude, maxLatitude, minLongitude, maxLongitude, pageable);

        return meetingPage.map(meeting -> {
            List<String> techStackNames = getTechStacksForMeeting(meeting);
            LikeDto likeDto = getLikeDtoForMeeting(meeting, username);

            return MeetingSummaryResponseDto.fromEntity(meeting, techStackNames, likeDto);
        });
    }

    /**
     * 특정 모임의 상세 정보를 가져옵니다.
     * - 모임의 생성자인지 확인하고, 지원 상태, 기술 스택, 좋아요 정보를 포함한 상세 정보를 반환합니다.
     *
     * @param username 모임 상세 정보를 요청한 사용자의 사용자명
     * @param meetingId 모임의 고유 ID
     * @return 모임의 상세 정보 DTO (MeetingDetailResponseDto)
     */
    public MeetingDetailResponseDto getMeetingDetail(String username, Long meetingId) {

        // 모임 정보와 개설자 정보 같이 가져오기
        MeetingEntity meetingEntity = meetingRepository.findWithCreatorById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(ErrorCode.MEETING_NOT_FOUND));

        // 사용자 타입: Creator(개설자), Applicant(지원자), Anonymous(익명 사용자)
        String userType = determineUserType(username, meetingEntity);

        // 지원 상태 조회
        ApplicationDetail applicationDetail = getApplicationDetail(username, userType, meetingEntity);

        // 기술 스택 조회
        List<String> techStacks = getTechStacksForMeeting(meetingEntity);

        // 좋아요 정보
        LikeDto likeDto = getLikeDtoForMeeting(meetingEntity, username);

        return MeetingDetailResponseDto.fromEntity(
                userType,
                applicationDetail.isApplication(),
                applicationDetail.applicationStatus(),
                meetingEntity,
                techStacks,
                likeDto);
    }

    /**
     * 모임 수정하기
     * - 개설자만 모임 정보를 수정할 수 있음
     *
     * @param username JWT를 통해 인증된 사용자명
     * @param meetingId 수정할 모임 아이디
     * @param updateMeetingRequestDto 업데이트할 모임 정보가 담긴 DTO
     * @return 모임에 대한 응답 DTO
     */
    @Transactional
    public MeetingResponseDto updateMeeting(String username, Long meetingId, UpdateMeetingRequestDto updateMeetingRequestDto) {

        MeetingEntity meetingEntity = validateAndGetMeeting(meetingId);

        // 개설자가 아닌 경우 예외 처리
        validateMeetingCreator(meetingEntity, username);

        meetingEntity.updateMeeting(updateMeetingRequestDto);

        // 기술 스택 업데이트
        List<String> techStackNames = updateMeetingTechStacks(meetingEntity, updateMeetingRequestDto);

        return MeetingResponseDto.fromEntity(meetingEntity, techStackNames);
    }

    /**
     * 모임 삭제하기
     * - 개설자만 모임 삭제할 수 있음
     *
     * @param username JWT를 통해 인증된 사용자명
     * @param meetingId 삭제할 모임 아이디
     */
    @Transactional
    public void deleteMeeting(String username, Long meetingId) {

        MeetingEntity meetingEntity = validateAndGetMeeting(meetingId);

        // 개설자가 아닌 경우 예외 처리
        validateMeetingCreator(meetingEntity, username);

        meetingTechStackRepository.deleteByMeeting(meetingEntity);

        likeRepository.deleteByMeeting(meetingEntity);

        meetingRepository.delete(meetingEntity);
    }

    /**
     * 모임원 확정하기
     *
     * @param username JWT를 통해 인증된 사용자명
     * @param meetingId 확정할 모임 아이디
     * @param confirmMeetingRequestDto 모임원들의 지원서 아이디 리스트
     * @return 모임원들의 정보가 담긴 DTO
     */
    @Transactional
    public List<ConfirmMeetingResponseDto> confirmMeetingMember(String username, Long meetingId, ConfirmMeetingRequestDto confirmMeetingRequestDto) {

        MeetingEntity meetingEntity = validateAndGetMeeting(meetingId);

        // 개설자가 아닌 경우 예외 처리
        validateMeetingCreator(meetingEntity, username);

        // 지원자 정보와 상태 업데이트
        List<ApplicationEntity> applications = applicationRepository.findByMeeting(meetingEntity);

        List<ConfirmMeetingResponseDto> responseDtos = new ArrayList<>();
        responseDtos.add(convertUserToDto(meetingEntity.getCreator())); // 개설자 처리
        processApplications(applications, confirmMeetingRequestDto.getApplicationIds(), responseDtos); // 지원자 처리

        // 미팅 상태를 확정
        meetingEntity.confirm();

        // 승인된 지원자 정보를 반환
        return responseDtos;
    }

    /**
     * 확정된 모임원 불러오기
     *
     * @param username JWT를 통해 인증된 사용자명
     * @param meetingId 모임 아이디
     * @return 모임원들의 정보가 담긴 DTO
     */
    public List<ConfirmMeetingResponseDto> getConfirmedMembers(String username, Long meetingId) {
        // 모임이 확정된 상태인지 확인
        MeetingEntity meetingEntity = validateAndGetConfirmedMeeting(meetingId);

        // 사용자가 존재하는지 확인
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 개설자 정보
        UserEntity creator = meetingEntity.getCreator();
        ConfirmMeetingResponseDto creatorInfo = convertUserToDto(creator);

        // 개설자인 경우: 지원서가 승인된 회원만 반환
        if (meetingEntity.getCreator().getUsername().equals(username)) {
            return getConfirmedMembersForCreator(meetingEntity, creatorInfo);
        }

        // 개설자가 아니고, 지원자가 승인되지 않은 경우 예외 처리
        ApplicationEntity applicant = applicationRepository.findByApplicantAndMeeting(user, meetingEntity)
                .orElseThrow(() -> new ApplicationNotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

        // 승인된 지원서들 반환
        return getConfirmedMembersForCreator(meetingEntity, creatorInfo);
    }

    /**
     * 모임에 연결된 기술 스택을 저장하는 메서드
     *
     * @param meetingEntity 생성된 모임 엔티티
     * @param createMeetingRequestDto 모임 생성 요청 DTO
     * @return 기술 스택 이름 리스트
     */
    private List<String> saveMeetingTechStacks(MeetingEntity meetingEntity, CreateMeetingRequestDto createMeetingRequestDto) {
        List<String> techStackNames = new ArrayList<>();

        // 기존 기술 스택 처리
        if (createMeetingRequestDto.getTechStackIds() != null) {
            techStackNames.addAll(processExistingTechStacks(meetingEntity, createMeetingRequestDto.getTechStackIds()));
        }

        // 새로운 기타 기술 스택 처리
        if (createMeetingRequestDto.getOtherTechStacks() != null) {
            techStackNames.addAll(processOtherTechStacks(meetingEntity, createMeetingRequestDto.getOtherTechStacks()));
        }

        return techStackNames;
    }

    /**
     * 기존 기술 스택 처리 메서드
     * - 기존 기술 스택 = 서비스에서 기본적으로 제공하는 기술 스택
     *
     * @param meetingEntity 모임 엔티티
     * @param techStackIds 기술 스택 아이디들
     * @return 기술 스택 이름 리스트
     */
    private List<String> processExistingTechStacks(MeetingEntity meetingEntity, List<Long> techStackIds) {
        List<TechStackEntity> techStacks = techStackRepository.findAllById(techStackIds);

        // MeetingTechStackEntity 생성
        List<MeetingTechStackEntity> meetingTechStackEntities = techStacks.stream()
                .map(techStack -> MeetingTechStackEntity.builder()
                        .meeting(meetingEntity)
                        .techStack(techStack)
                        .build())
                .toList();

        // 한 번에 저장
        meetingTechStackRepository.saveAll(meetingTechStackEntities);

        // 이름 반환
        return techStacks.stream()
                .map(TechStackEntity::getName)
                .toList();
    }

    /**
     * 사용자가 새로 입력한 기술 스택 처리 메서드
     *
     * @param meetingEntity 모임 엔티티
     * @param otherTechStackNames 사용자가 입력한 기술 스택 이름들
     * @return 기술 스택 이름 리스트
     */
    private List<String> processOtherTechStacks(MeetingEntity meetingEntity, List<String> otherTechStackNames) {
        otherTechStackNames.forEach(otherTech -> {
            // 기술 스택이 이미 존재하는지 확인하고, 없으면 새로 생성
            TechStackEntity techStack = techStackRepository.findByName(otherTech)
                    .orElseGet(() -> techStackRepository.save(
                            TechStackEntity.builder()
                                    .name(otherTech)
                                    .category(TechCategory.OTHER)
                                    .build()
                    ));

            // 모임과 기술 스택을 연결하여 저장
            meetingTechStackRepository.save(
                    MeetingTechStackEntity.builder()
                            .meeting(meetingEntity)
                            .techStack(techStack)
                            .build()
            );
        });

        return otherTechStackNames.stream()
                .toList();
    }

    /**
     * userType 정의
     * - 익명 사용자, 개설자, 지원자 세가지 상태가 존재
     *
     * @param username 사용자명
     * @param meetingEntity 모임 엔티티
     * @return userType
     */
    private String determineUserType(String username, MeetingEntity meetingEntity) {
        if (username == null) return "Anonymous";
        if (meetingEntity.getCreator().getUsername().equals(username)) return "Creator";
        return "Applicant";
    }

    /**
     * 지원자일 경우 지원자의 상태를 확인하기 위한 메서드
     * - 지원 여부, 지원 상태를 알려줍니다.
     *
     * @param username 사용자명
     * @param userType 유저 타입 - 지원자일 경우 체크
     * @param meetingEntity 모임 엔티티
     * @return 지원 여부 (isApplication)와 지원 상태 (applicationStatus)를 포함하는 ApplicationDetail 객체
     */
    private ApplicationDetail getApplicationDetail(String username, String userType, MeetingEntity meetingEntity) {
        if (!"Applicant".equals(userType)) {
            return new ApplicationDetail(null, null);
        }

        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));
        ApplicationEntity applicationEntity = applicationRepository.findByApplicantAndMeeting(userEntity, meetingEntity)
                .orElse(null);

        Boolean isApplication = (applicationEntity != null);
        ApplicationStatus applicationStatus = (applicationEntity != null) ? applicationEntity.getStatus() : null;

        return new ApplicationDetail(isApplication, applicationStatus);
    }

    private record ApplicationDetail(Boolean isApplication, ApplicationStatus applicationStatus) {}

    /**
     * 모임에 연결된 기술 스택을 반환하는 메서드
     *
     * @param meetingEntity 모임 엔티티
     * @return 기술 스택 이름 리스트
     */
    private List<String> getTechStacksForMeeting(MeetingEntity meetingEntity) {
        return meetingTechStackRepository.findByMeeting(meetingEntity).stream()
                .map(meetingTechStack -> meetingTechStack.getTechStack().getName())
                .collect(Collectors.toList());
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

    /**
     * 모임 연결된 기술 스택을 업데이트하는 메서드
     *
     * @param meetingEntity 모임 엔티티
     * @param updateMeetingRequestDto 업데이트할 모임 정보가 담긴 DTO
     */
    private List<String> updateMeetingTechStacks(MeetingEntity meetingEntity, UpdateMeetingRequestDto updateMeetingRequestDto) {
        if (updateMeetingRequestDto.getTechStackIds() != null || updateMeetingRequestDto.getOtherTechStacks() != null) {
            meetingTechStackRepository.deleteByMeeting(meetingEntity);
        }

        List<String> techStackNames = new ArrayList<>();

        // 기존에 있는 기술 스택 처리
        if (updateMeetingRequestDto.getTechStackIds() != null) {
            techStackNames.addAll(processExistingTechStacks(meetingEntity, updateMeetingRequestDto.getTechStackIds()));
        }

        // 새로운 기타 기술 스택 처리
        if (updateMeetingRequestDto.getOtherTechStacks() != null) {
            techStackNames.addAll(processOtherTechStacks(meetingEntity, updateMeetingRequestDto.getOtherTechStacks()));
        }

        return techStackNames;
    }

    /**
     * 유저 엔티티를 통해 모임 확정 DTO로 바꾸는 메서드
     *
     * @param userEntity 유저 엔티티
     * @return 모임원들의 정보가 담긴 DTO
     */
    private ConfirmMeetingResponseDto convertUserToDto(UserEntity userEntity) {
        return ConfirmMeetingResponseDto.fromEntity(userEntity);
    }

    /**
     * 지원자들의 처리
     * - 유효하지 않은 아이디 제거
     * - 모임 확정된 지원자들은 지원으로 status 변경, 아닌 경우는 거절로 status 변경
     *
     * @param applications 지원서 엔티티
     * @param applicationIds 모임원들의 지원서 아이디 리스트
     * @param responseDtos 모임원들의 정보가 담긴 DTO
     */
    private void processApplications(List<ApplicationEntity> applications, List<Long> applicationIds, List<ConfirmMeetingResponseDto> responseDtos) {
        // 지원서 ID가 유효한지 확인
        Set<Long> validApplicationIds = new HashSet<>(applicationIds); // 중복 제거를 위해 Set 사용
        List<ApplicationEntity> validApplications = new ArrayList<>();

        // 유효한 지원서를 처리하고, 유효하지 않은 ID는 예외 처리
        for (ApplicationEntity applicationEntity : applications) {
            if (validApplicationIds.remove(applicationEntity.getId())) {
                validApplications.add(applicationEntity);
            }
        }

        // 유효하지 않은 ID가 존재하면 예외 발생
        if (!validApplicationIds.isEmpty()) {
            throw new InvalidApplicationIdException(ErrorCode.INVALID_APPLICATION_ID);
        }

        // 승인된 지원서 처리
        processApprovedApplications(validApplications, responseDtos);

        // 거절된 지원서 처리
        rejectInvalidApplications(applications, validApplications);
    }

    /**
     * 승인된 지원서 처리
     * - 지원자의 상태 -> approve
     */
    private void processApprovedApplications(List<ApplicationEntity> validApplications, List<ConfirmMeetingResponseDto> responseDtos) {
        for (ApplicationEntity applicationEntity : validApplications) {
            applicationEntity.approve(); // 지원 확정
            responseDtos.add(convertUserToDto(applicationEntity.getApplicant())); // 승인된 지원자 DTO 변환 후 추가
        }
    }

    /**
     * 거절된 지원서 처리
     * - 지원자의 상태 -> reject
     */
    private void rejectInvalidApplications(List<ApplicationEntity> applications, List<ApplicationEntity> validApplications) {
        for (ApplicationEntity applicationEntity : applications) {
            if (!validApplications.contains(applicationEntity)) {
                applicationEntity.reject(); // 지원 거절
            }
        }
    }

    /**
     * 개설자에게 승인된 지원자 목록을 반환하는 공통 로직
     */
    private List<ConfirmMeetingResponseDto> getConfirmedMembersForCreator(MeetingEntity meetingEntity, ConfirmMeetingResponseDto creatorInfo) {
        // 상태가 APPROVED인 지원서 조회
        List<ApplicationEntity> approvedApplications = applicationRepository
                .findByMeetingAndStatus(meetingEntity, ApplicationStatus.APPROVED);

        // DTO로 변환
        List<ConfirmMeetingResponseDto> responseDtos = approvedApplications.stream()
                .map(application -> ConfirmMeetingResponseDto.fromEntity(application.getApplicant()))
                .collect(Collectors.toList());

        // 개설자 정보 추가
        responseDtos.add(0, creatorInfo);

        return responseDtos;
    }

}
