package com.renzzle.backend.domain.admin.api;

import com.renzzle.backend.domain.puzzle.cache.web.PuzzleCachePageController;
import com.renzzle.backend.global.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;
import java.util.stream.Collectors;

// The top tabs only show menus the admin has a permission for
@ControllerAdvice(assignableTypes = {AdminController.class, PuzzleCachePageController.class})
public class AdminPageModelAdvice {

    @ModelAttribute("adminPermissions")
    public Set<String> adminPermissions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return Set.of();
        }
        return userDetails.getAdminPermissions().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}
