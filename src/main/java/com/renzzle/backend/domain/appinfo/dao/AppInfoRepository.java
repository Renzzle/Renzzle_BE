package com.renzzle.backend.domain.appinfo.dao;

import com.renzzle.backend.domain.appinfo.domain.AppInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppInfoRepository extends JpaRepository<AppInfo, Long> {

    List<AppInfo> findAllByOrderByTagAsc();

    Optional<AppInfo> findByTag(String tag);

}
