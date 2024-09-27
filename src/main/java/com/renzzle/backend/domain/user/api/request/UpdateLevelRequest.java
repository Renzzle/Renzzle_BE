package com.renzzle.backend.domain.user.api.request;

import com.renzzle.backend.domain.user.domain.UserLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateLevelRequest {

    @NotNull(message = "레벨은 반드시 지정되어야 합니다.")
    private UserLevel.LevelName level;
}
