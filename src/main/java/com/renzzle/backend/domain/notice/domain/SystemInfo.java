package com.renzzle.backend.domain.notice.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "system_info")
public class SystemInfo {

    @Id
    @Column(name = "id")
    private final Long id = 1L;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "system_check", nullable = false)
    private boolean isSystemCheck;

}
