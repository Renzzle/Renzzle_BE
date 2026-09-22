package com.renzzle.backend.domain.notice.domain;

import com.renzzle.backend.global.common.domain.AppPlatform;
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

    @Column(name = "android_version", nullable = false)
    private String androidVersion;

    @Column(name = "ios_version", nullable = false)
    private String iosVersion;

    @Column(name = "system_check", nullable = false)
    private boolean isSystemCheck;

    public String getRequiredVersion(AppPlatform platform) {
        return platform == AppPlatform.IOS ? iosVersion : androidVersion;
    }

    public void update(String androidVersion, String iosVersion, boolean isSystemCheck) {
        this.androidVersion = androidVersion;
        this.iosVersion = iosVersion;
        this.isSystemCheck = isSystemCheck;
    }

}
