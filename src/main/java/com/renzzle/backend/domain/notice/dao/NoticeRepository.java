package com.renzzle.backend.domain.notice.dao;

import com.renzzle.backend.domain.notice.domain.Notice;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByUser(UserEntity user);

    void deleteAllByUser(UserEntity user);

}
