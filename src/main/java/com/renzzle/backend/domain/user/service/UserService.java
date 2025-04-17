package com.renzzle.backend.domain.user.service;

import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.user.api.response.ChangeNicknameResponse;
import com.renzzle.backend.domain.user.api.response.GetUserLikedPuzzlesResponse;
import com.renzzle.backend.domain.user.api.response.UserResponse;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final Clock clock;
    private final UserRepository userRepository;
    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    public UserResponse getUserResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .currency(user.getCurrency())
                .build();
    }

    @Transactional
    public Long deleteUser(UserEntity user) {
        int updatedRows = userRepository.softDelete(user.getId(), clock.instant());
        if (updatedRows == 0) {
            throw new CustomException(ErrorCode.CANNOT_FIND_USER);
        }
        return user.getId();
    }

    @Transactional
    public ChangeNicknameResponse changeNickname(UserEntity user, String nickname) {
        Optional<UserEntity> persistedUser = userRepository.findById(user.getId());

        if (persistedUser.isEmpty()) {
            throw new CustomException(ErrorCode.CANNOT_FIND_USER);
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        persistedUser.get().changeNickname(nickname);
        return ChangeNicknameResponse.builder()
                .currency(persistedUser.get().getCurrency())
                .build();
    }

    @Transactional(readOnly = true)
    public List<GetCommunityPuzzlesResponse> getUserLikedPuzzleList(UserEntity user, Long cursorId, int size) {
        List<CommunityPuzzle> puzzles = communityPuzzleRepository.getUserLikedPuzzles(user.getId(), cursorId, size);

        List<GetCommunityPuzzlesResponse> response = new ArrayList<>();
        for (CommunityPuzzle puzzle : puzzles) {
            boolean isSolved = userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId());

            response.add(
                    GetCommunityPuzzlesResponse.builder()
                            .id(puzzle.getId())
                            .boardStatus(puzzle.getBoardStatus())
                            .authorId(puzzle.getUser().getId())
                            .authorName(puzzle.getUser().getNickname())
                            .depth(puzzle.getDepth())
                            .winColor(puzzle.getWinColor().getName())
                            .likeCount(puzzle.getLikeCount())
                            .createdAt(puzzle.getCreatedAt().toString())
                            .isSolved(isSolved)
                            .isVerified(puzzle.getIsVerified())
                            .build()
            );
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<GetCommunityPuzzlesResponse> getUserPuzzleList(UserEntity user, Long cursorId, int size) {
        List<CommunityPuzzle> puzzles = communityPuzzleRepository.getUserPuzzles(user.getId(), cursorId, size);

        List<GetCommunityPuzzlesResponse> response = new ArrayList<>();
        for (CommunityPuzzle puzzle : puzzles) {
            boolean isSolved = userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId());

            response.add(
                    GetCommunityPuzzlesResponse.builder()
                            .id(puzzle.getId())
                            .boardStatus(puzzle.getBoardStatus())
                            .authorId(puzzle.getUser().getId())
                            .authorName(puzzle.getUser().getNickname())
                            .depth(puzzle.getDepth())
                            .winColor(puzzle.getWinColor().getName())
                            .likeCount(puzzle.getLikeCount())
                            .createdAt(puzzle.getCreatedAt().toString())
                            .isSolved(isSolved)
                            .isVerified(puzzle.getIsVerified())
                            .build()
            );
        }

        return response;
    }

    @Transactional
    public Long deleteUserPuzzle(UserEntity user, Long puzzleId) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        if (!Objects.equals(puzzle.getUser().getId(), user.getId())) {
            throw new CustomException(ErrorCode.COMMUNITY_PUZZLE_ACCESS_DENIED);
        }

        int updatedRows = communityPuzzleRepository.softDelete(puzzleId, clock.instant());
        if (updatedRows == 0) {
            throw new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE);
        }

        return puzzleId;
    }

}
