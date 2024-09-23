package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.domain.puzzle.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.dao.UserCommunityPuzzleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;

}
