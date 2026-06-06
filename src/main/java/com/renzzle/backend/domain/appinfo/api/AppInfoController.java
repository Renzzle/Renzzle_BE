package com.renzzle.backend.domain.appinfo.api;

import com.renzzle.backend.domain.appinfo.api.response.GetAppInfoResponse;
import com.renzzle.backend.domain.appinfo.service.AppInfoService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/app-info")
@RequiredArgsConstructor
@Tag(name = "App Info API", description = "App info API")
public class AppInfoController {

    private final AppInfoService appInfoService;

    @Operation(summary = "Get app info list", description = "Get list of app info entries")
    @GetMapping
    public ApiResponse<List<GetAppInfoResponse>> getAppInfoList() {
        return ApiUtils.success(appInfoService.getAppInfoList());
    }

}
