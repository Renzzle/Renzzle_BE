package com.renzzle.backend.domain.notice.dao;

import com.renzzle.backend.domain.notice.domain.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
}
