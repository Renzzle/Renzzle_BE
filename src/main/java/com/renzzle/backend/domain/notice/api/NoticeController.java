package com.renzzle.backend.domain.notice.api;

import com.renzzle.backend.domain.notice.api.request.GetNoticeWithLangCodeRequest;
import com.renzzle.backend.domain.notice.api.response.GetPersonalNoticeResponse;
import com.renzzle.backend.domain.notice.api.response.GetPublicNoticeResponse;
import com.renzzle.backend.domain.notice.api.response.GetVersionCodeResponse;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
@Tag(name = "Notice API", description = "Notice API")
public class NoticeController {

    @Operation(summary = "Get personal notice", description = "Get list of personal notices")
    @PostMapping("/personal")
    public ApiResponse<GetPersonalNoticeResponse> getPersonalNotice(@ModelAttribute GetNoticeWithLangCodeRequest request) {
        return ApiUtils.success(null);
    }

    @Operation(summary = "Get public notice", description = "Get list of public notices that not expired")
    @PostMapping("/personal")
    public ApiResponse<GetPublicNoticeResponse> getPublicNotice(@ModelAttribute GetNoticeWithLangCodeRequest request) {
        return ApiUtils.success(null);
    }

    @Operation(summary = "Get app version", description = "Get list of personal notices")
    @PostMapping("/personal")
    public ApiResponse<GetVersionCodeResponse> getVersionCode() {
        return ApiUtils.success(null);
    }

}
