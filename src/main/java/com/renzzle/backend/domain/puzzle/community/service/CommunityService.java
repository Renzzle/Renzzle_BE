package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.domain.puzzle.community.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.AddPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.*;
import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.constant.SortOption;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.renzzle.backend.global.common.constant.StringConstant.DELETED_USER;
import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    @Transactional
    public AddPuzzleResponse addCommunityPuzzle(AddCommunityPuzzleRequest request, UserEntity user) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        CommunityPuzzle puzzle = CommunityPuzzle.builder()
                .title(request.title())
                .boardStatus(request.boardStatus())
                .boardKey(boardKey)
                .depth(request.depth())
                .user(user)
                .difficulty(Difficulty.getDifficulty(request.difficulty()))
                .winColor(WinColor.getWinColor(request.winColor()))
                .build();

        CommunityPuzzle result = communityPuzzleRepository.save(puzzle);

        return AddPuzzleResponse.builder()
                .id(result.getId())
                .title(result.getTitle())
                .build();
    }

    @Transactional(readOnly = true)
    public CommunityPuzzle findCommunityPuzzleById(Long puzzleId) {
        Optional<CommunityPuzzle> puzzle = communityPuzzleRepository.findById(puzzleId);
        return puzzle.orElse(null);
    }

    @Transactional
    public int solveCommunityPuzzle(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = findCommunityPuzzleById(puzzleId);
        if(puzzle == null)
            throw new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE);

        Optional<UserCommunityPuzzle> findResult = userCommunityPuzzleRepository.findUserPuzzleInfo(user.getId(), puzzle.getId());
        UserCommunityPuzzle userPuzzleInfo = findResult.orElseGet(() -> userCommunityPuzzleRepository.save(
                UserCommunityPuzzle.builder()
                        .user(user)
                        .puzzle(puzzle)
                        .lastTriedAt(Instant.now())
                        .build()));

        puzzle.addSolve();

        return userPuzzleInfo.addSolve();
    }

    @Transactional
    public int failCommunityPuzzle(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = findCommunityPuzzleById(puzzleId);
        if(puzzle == null)
            throw new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE);

        Optional<UserCommunityPuzzle> findResult = userCommunityPuzzleRepository.findUserPuzzleInfo(user.getId(), puzzle.getId());
        UserCommunityPuzzle userPuzzleInfo = findResult.orElseGet(() -> userCommunityPuzzleRepository.save(
                UserCommunityPuzzle.builder()
                        .user(user)
                        .puzzle(puzzle)
                        .lastTriedAt(Instant.now())
                        .build()));

        puzzle.addFail();

        return userPuzzleInfo.addFail();
    }

    @Transactional(readOnly = true)
    public List<GetCommunityPuzzleResponse> getCommunityPuzzleList(Long id, Integer size, SortOption sortOption) {
        List<CommunityPuzzle> puzzleList;

        switch (sortOption) {
            case LATEST -> {
                puzzleList = getCommunityPuzzleListSortByCreatedAt(id, size);
            }
            case LIKE -> {
                puzzleList = getCommunityPuzzleListSortByLike(id, size);
            }
            default -> { // recommend is default
                puzzleList = getRecommendCommunityPuzzleList(id, size);
            }
        }

        return buildGetCommunityPuzzleResponse(puzzleList);
    }

    private List<GetCommunityPuzzleResponse> buildGetCommunityPuzzleResponse(List<CommunityPuzzle> puzzleList) {
        List<GetCommunityPuzzleResponse> response = new ArrayList<>();
        for(CommunityPuzzle puzzle : puzzleList) {
            double correctRate = 0.0;
            if(puzzle.getSolvedCount() != 0)
                correctRate = (double) puzzle.getSolvedCount() / (puzzle.getSolvedCount() + puzzle.getFailedCount()) * 100;
            List<String> tags = getTags(puzzle);
            String authorName = (puzzle.getUser().getStatus() != Status.getStatus(Status.StatusName.DELETED)) ?
                    puzzle.getUser().getNickname() : DELETED_USER;

            response.add(
                    GetCommunityPuzzleResponse.builder()
                            .id(puzzle.getId())
                            .title(puzzle.getTitle())
                            .boardStatus(puzzle.getBoardStatus())
                            .authorId(puzzle.getUser().getId())
                            .authorName(authorName)
                            .solvedCount(puzzle.getSolvedCount())
                            .correctRate(correctRate)
                            .depth(puzzle.getDepth())
                            .difficulty(puzzle.getDifficulty().getName())
                            .winColor(puzzle.getWinColor().getName())
                            .likeCount(puzzle.getLikeCount())
                            .tag(tags)
                            .build()
            );
        }

        return response;
    }

    private List<CommunityPuzzle> getRecommendCommunityPuzzleList(Long id, Integer size) {
        // TODO: add recommend algorithm
        Instant lastCreatedAt;
        long lastId;

        if(id == null) {
            lastId = -1L;
            lastCreatedAt = CONST_FUTURE_INSTANT;
        } else {
            lastId = id;
            CommunityPuzzle puzzle = communityPuzzleRepository.findById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));
            lastCreatedAt = puzzle.getCreatedAt();
        }

        return communityPuzzleRepository.findPuzzlesSortByCreatedAt(lastCreatedAt, lastId, size);
    }

    private List<CommunityPuzzle> getCommunityPuzzleListSortByLike(Long id, Integer size) {
        int lastLikeCnt;
        long lastId;

        if(id == null) {
            lastId = -1L;
            lastLikeCnt = Integer.MAX_VALUE;
        } else {
            lastId = id;
            CommunityPuzzle puzzle = communityPuzzleRepository.findById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));
            lastLikeCnt = puzzle.getLikeCount();
        }

        return communityPuzzleRepository.findPuzzlesSortByLike(lastLikeCnt, lastId, size);
    }

    private List<CommunityPuzzle> getCommunityPuzzleListSortByCreatedAt(Long id, Integer size) {
        Instant lastCreatedAt;
        long lastId;

        if(id == null) {
            lastId = -1L;
            lastCreatedAt = CONST_FUTURE_INSTANT;
        } else {
            lastId = id;
            CommunityPuzzle puzzle = communityPuzzleRepository.findById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));
            lastCreatedAt = puzzle.getCreatedAt();
        }

        return communityPuzzleRepository.findPuzzlesSortByCreatedAt(lastCreatedAt, lastId, size);
    }

    private List<String> getTags(CommunityPuzzle puzzle) {
        List<String> tags = new ArrayList<>();

        // add solved tag
        long puzzleId = puzzle.getId();
        long userId = puzzle.getUser().getId();

        Optional<UserCommunityPuzzle> userPuzzleInfo = userCommunityPuzzleRepository.findUserPuzzleInfo(userId, puzzleId);
        if(userPuzzleInfo.isPresent()) {
            if(userPuzzleInfo.get().getSolvedCount() > 0)
                tags.add(Tag.SOLVED.name());
        }

        return tags;
    }

    @Transactional(readOnly = true)
    public List<GetCommunityPuzzleResponse> searchCommunityPuzzle(String query) {
        List<CommunityPuzzle> puzzles = new ArrayList<>();

        Optional<CommunityPuzzle> searchById = findByIdQuery(query);
        searchById.ifPresent(puzzles::add);

        List<CommunityPuzzle> searchByTitle = communityPuzzleRepository.findByTitleContaining(query);
        puzzles.addAll(searchByTitle);

        List<CommunityPuzzle> searchByAuthor = communityPuzzleRepository.findByAuthorName(query);
        puzzles.addAll(searchByAuthor);

        return buildGetCommunityPuzzleResponse(puzzles.stream().distinct().toList());
    }

    private Optional<CommunityPuzzle> findByIdQuery(String query) {
        if(query == null || query.isEmpty())
            return Optional.empty();

        try {
            long id = Long.parseLong(query);
            return communityPuzzleRepository.findById(id);
        } catch(Exception e) {
            return Optional.empty();
        }
    }

}
