package com.renzzle.backend.domain.notice.dao;

import com.renzzle.backend.domain.notice.domain.SystemInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SystemInfoRepository extends JpaRepository<SystemInfo, Long> {

    @Query("SELECT si FROM SystemInfo si WHERE si.id = 1")
    Optional<SystemInfo> getSystemInfo();

    @Modifying
    @Query("UPDATE SystemInfo si SET si.version = :version WHERE si.id = 1")
    int changeVersionInfo(@Param("version") String version);

    @Modifying
    @Query("UPDATE SystemInfo si SET si.isSystemCheck = :isSystemCheck WHERE si.id = 1")
    int setSystemCheck(@Param("isSystemCheck") boolean isSystemCheck);

}
