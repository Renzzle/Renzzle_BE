package com.renzzle.backend.domain.notice.domain;

import com.renzzle.backend.global.common.domain.LangCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "announcement")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lang_code", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LangCode langCode;

    @Column(name = "title", nullable = false, length = 127)
    private String title;

    @Column(name = "context", nullable = false, length = 1023)
    private String context;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

}
