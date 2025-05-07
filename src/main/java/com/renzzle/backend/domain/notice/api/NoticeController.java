package com.renzzle.backend.domain.notice.api;

import com.renzzle.backend.domain.notice.api.request.GetPersonalNoticeRequest;
import com.renzzle.backend.domain.notice.api.request.GetPublicNoticeRequest;
import com.renzzle.backend.domain.notice.api.response.GetPersonalNoticeResponse;
import com.renzzle.backend.domain.notice.api.response.GetPublicNoticeResponse;
import com.renzzle.backend.domain.notice.service.NoticeService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
@Tag(name = "Notice API", description = "Notice API")
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "Get personal notice", description = "Get list of personal notices")
    @PostMapping("/personal")
    public ApiResponse<GetPersonalNoticeResponse> getPersonalNotice(
            @Valid @ModelAttribute GetPersonalNoticeRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiUtils.success(noticeService.getPersonalNotice(request, user.getUser()));
    }

    @Operation(summary = "Get public notice", description = "Get list of public notices that not expired")
    @PostMapping("/public")
    public ApiResponse<List<GetPublicNoticeResponse>> getPublicNotice(@Valid @ModelAttribute GetPublicNoticeRequest request) {
        return ApiUtils.success(noticeService.getPublicNotice(request));
    }

}
