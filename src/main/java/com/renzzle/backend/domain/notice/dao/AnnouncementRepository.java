package com.renzzle.backend.domain.notice.dao;

import com.renzzle.backend.domain.notice.domain.Announcement;
import com.renzzle.backend.global.common.domain.LangCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findAllByLangCodeAndExpiredAtAfter(LangCode langCode, Instant now);

}
