package com.renzzle.backend.domain.appinfo.service;

import com.renzzle.backend.domain.appinfo.api.request.UpsertAppInfoRequest;
import com.renzzle.backend.domain.appinfo.api.response.GetAppInfoResponse;
import com.renzzle.backend.domain.appinfo.dao.AppInfoRepository;
import com.renzzle.backend.domain.appinfo.domain.AppInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppInfoService {

    private final AppInfoRepository appInfoRepository;

    @Transactional(readOnly = true)
    public List<GetAppInfoResponse> getAppInfoList() {
        return appInfoRepository.findAllByOrderByTagAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    // An existing tag keeps its row and only has its value replaced
    @Transactional
    public GetAppInfoResponse upsertAppInfo(UpsertAppInfoRequest request) {
        String tag = request.tag().trim();
        AppInfo appInfo = appInfoRepository.findByTag(tag)
                .orElseGet(() -> AppInfo.builder().tag(tag).build());
        appInfo.updateValue(request.value().trim());
        return toResponse(appInfoRepository.save(appInfo));
    }

    private GetAppInfoResponse toResponse(AppInfo appInfo) {
        return GetAppInfoResponse.builder()
                .tag(appInfo.getTag())
                .value(appInfo.getValue())
                .build();
    }

}
