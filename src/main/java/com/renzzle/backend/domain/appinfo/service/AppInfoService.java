package com.renzzle.backend.domain.appinfo.service;

import com.renzzle.backend.domain.appinfo.api.response.GetAppInfoResponse;
import com.renzzle.backend.domain.appinfo.dao.AppInfoRepository;
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
                .map(appInfo -> GetAppInfoResponse.builder()
                        .tag(appInfo.getTag())
                        .value(appInfo.getValue())
                        .build())
                .toList();
    }

}
