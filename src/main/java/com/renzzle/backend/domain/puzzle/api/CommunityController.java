package com.renzzle.backend.domain.puzzle.api;

import com.renzzle.backend.domain.puzzle.service.CommunityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Tag(name = "Community Puzzle API", description = "Community Puzzle API")
public class CommunityController {

    private final CommunityService communityService;

}
