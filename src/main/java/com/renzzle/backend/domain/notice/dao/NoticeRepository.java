package com.renzzle.backend.domain.notice.dao;

import com.renzzle.backend.domain.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
