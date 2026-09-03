package com.renzzle.backend.domain.notice.dao;

import com.renzzle.backend.domain.notice.domain.SystemInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SystemInfoRepository extends JpaRepository<SystemInfo, Long> {

    @Query("SELECT si FROM SystemInfo si WHERE si.id = 1")
    Optional<SystemInfo> getSystemInfo();

}
