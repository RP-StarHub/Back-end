package com.example.starhub.controller;

import com.example.starhub.controller.docs.MeetingControllerDocs;
import com.example.starhub.dto.request.ConfirmMeetingRequestDto;
import com.example.starhub.dto.request.CreateMeetingRequestDto;
import com.example.starhub.dto.request.UpdateMeetingRequestDto;
import com.example.starhub.dto.response.ConfirmMeetingResponseDto;
import com.example.starhub.dto.response.MeetingDetailResponseDto;
import com.example.starhub.dto.response.MeetingResponseDto;
import com.example.starhub.dto.response.MeetingSummaryResponseDto;
import com.example.starhub.dto.security.CustomUserDetails;
import com.example.starhub.entity.enums.Duration;
import com.example.starhub.response.code.ResponseCode;
import com.example.starhub.response.dto.ResponseDto;
import com.example.starhub.service.MeetingService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController implements MeetingControllerDocs {

    private final MeetingService meetingService;

    /**
     * 모임(스터디 및 프로젝트) 업로드
     */
    @PostMapping
    public ResponseEntity<ResponseDto<MeetingResponseDto>> createMeeting(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody CreateMeetingRequestDto createMeetingRequestDto) {

        MeetingResponseDto res = meetingService.createMeeting(customUserDetails.getUsername(), createMeetingRequestDto);
        return ResponseEntity
                .status(ResponseCode.SUCCESS_CREATE_MEETING.getStatus().value())
                .body(new ResponseDto<>(ResponseCode.SUCCESS_CREATE_MEETING, res));
    }

    /**
     * 모임 목록 불러오기 (메인 화면에 쓰일 API)
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<Page<MeetingSummaryResponseDto>>> searchMeetings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer minParticipants,
            @RequestParam(required = false) Integer maxParticipants,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Duration duration,
            @RequestParam(required = true) Double minLatitude,
            @RequestParam(required = true) Double maxLatitude,
            @RequestParam(required = true) Double minLongitude,
            @RequestParam(required = true) Double maxLongitude,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {

        String username = customUserDetails != null ? customUserDetails.getUsername() : null;
        Page<MeetingSummaryResponseDto> res = meetingService.searchMeetings(username, title, minParticipants, maxParticipants, techStacks, location, duration,
                minLatitude, maxLatitude, minLongitude, maxLongitude, page, size);
        return ResponseEntity
                .status(ResponseCode.SUCCESS_GET_MEETING_LIST.getStatus().value())
                .body(new ResponseDto<>(ResponseCode.SUCCESS_GET_MEETING_LIST, res));
    }

    /**
     * 모임 상세 불러오기
     */
    @GetMapping("/{meetingId}")
    public ResponseEntity<ResponseDto<MeetingDetailResponseDto>> getMeetingDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long meetingId) {

        // 익명 사용자일 경우 null 전달, 인증된 사용자일 경우 customUserDetails 전달
        String username = customUserDetails != null ? customUserDetails.getUsername() : null;
        MeetingDetailResponseDto res = meetingService.getMeetingDetail(username, meetingId);
        return ResponseEntity
                .status(ResponseCode.SUCCESS_GET_MEETING_DETAIL.getStatus().value())
                .body(new ResponseDto<>(ResponseCode.SUCCESS_GET_MEETING_DETAIL, res));
    }

    /**
     * 모임 수정하기
     */
    @PatchMapping("/{meetingId}")
    public ResponseEntity<ResponseDto<MeetingResponseDto>> updateMeeting(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long meetingId,
            @RequestBody UpdateMeetingRequestDto updateMeetingRequestDto) {

        MeetingResponseDto res = meetingService.updateMeeting(customUserDetails.getUsername(), meetingId, updateMeetingRequestDto);
        return ResponseEntity
                .status(ResponseCode.SUCCESS_UPDATE_MEETING.getStatus().value())
                .body(new ResponseDto<>(ResponseCode.SUCCESS_UPDATE_MEETING, res));
    }

    /**
     * 모임 삭제하기
     */
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<ResponseDto> deleteMeeting(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long meetingId) {

        meetingService.deleteMeeting(customUserDetails.getUsername(), meetingId);
        return ResponseEntity
                .status(ResponseCode.SUCCESS_DELETE_MEETING.getStatus().value())
                .body(new ResponseDto<>(ResponseCode.SUCCESS_DELETE_MEETING, null));
    }

    /**
     * 모임원 확정하기
     */
    @PatchMapping("/{meetingId}/confirm")
    public ResponseEntity<ResponseDto<List<ConfirmMeetingResponseDto>>> confirmMeetingMember(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long meetingId,
            @RequestBody ConfirmMeetingRequestDto confirmMeetingRequestDto) {
        List<ConfirmMeetingResponseDto> res = meetingService.confirmMeetingMember(customUserDetails.getUsername(), meetingId, confirmMeetingRequestDto);
        return ResponseEntity
                .status(ResponseCode.SUCCESS_CONFIRM_MEETING_MEMBER.getStatus().value())
                .body(new ResponseDto<>(ResponseCode.SUCCESS_CONFIRM_MEETING_MEMBER, res));
    }


    /**
     * 확정된 모임원 불러오기
     */
    @GetMapping("/{meetingId}/confirmed")
    public ResponseEntity<ResponseDto<List<ConfirmMeetingResponseDto>>> getConfirmedMembers(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long meetingId) {
        List<ConfirmMeetingResponseDto> res = meetingService.getConfirmedMembers(customUserDetails.getUsername(), meetingId);
        return ResponseEntity
                .status(ResponseCode.SUCCESS_GET_CONFIRMED_MEMBERS.getStatus().value())
                .body(new ResponseDto<>(ResponseCode.SUCCESS_GET_CONFIRMED_MEMBERS, res));
    }
}
