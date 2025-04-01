package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.domain.puzzle.community.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.AddPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.*;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    @Transactional
    public AddPuzzleResponse addCommunityPuzzle(AddCommunityPuzzleRequest request, UserEntity user) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        CommunityPuzzle puzzle = CommunityPuzzle.builder()
                .boardStatus(request.boardStatus())
                .boardKey(boardKey)
                .answer(request.answer())
                .depth(request.depth())
                .rating(request.depth() * 200.0) // TODO: rating formula
                .description(request.description())
                .user(user)
                .winColor(WinColor.getWinColor(request.winColor()))
                .build();

        CommunityPuzzle result = communityPuzzleRepository.save(puzzle);

        return AddPuzzleResponse.builder()
                .puzzleId(result.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<GetCommunityPuzzleResponse> getCommunityPuzzleList(GetCommunityPuzzleRequest request, UserEntity user) {
        List<CommunityPuzzle> puzzleList = communityPuzzleRepository.searchCommunityPuzzles(request, user.getId());

        return buildGetCommunityPuzzleResponse(puzzleList, user);
    }

    private List<GetCommunityPuzzleResponse> buildGetCommunityPuzzleResponse(List<CommunityPuzzle> puzzleList, UserEntity user) {
        List<GetCommunityPuzzleResponse> response = new ArrayList<>();
        for(CommunityPuzzle puzzle : puzzleList) {
            boolean isSolved = communityPuzzleRepository.checkIsSolvedPuzzle(puzzle.getId(), user.getId());

            response.add(
                    GetCommunityPuzzleResponse.builder()
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

//    @Transactional(readOnly = true)
//    public CommunityPuzzle findCommunityPuzzleById(Long puzzleId) {
//        Optional<CommunityPuzzle> puzzle = communityPuzzleRepository.findById(puzzleId);
//        return puzzle.orElse(null);
//    }
//
//    @Transactional
//    public int solveCommunityPuzzle(Long puzzleId, UserEntity user) {
//        CommunityPuzzle puzzle = findCommunityPuzzleById(puzzleId);
//        if(puzzle == null)
//            throw new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE);
//
//        Optional<UserCommunityPuzzle> findResult = userCommunityPuzzleRepository.findUserPuzzleInfo(user.getId(), puzzle.getId());
//        UserCommunityPuzzle userPuzzleInfo = findResult.orElseGet(() -> userCommunityPuzzleRepository.save(
//                UserCommunityPuzzle.builder()
//                        .user(user)
//                        .puzzle(puzzle)
//                        .lastTriedAt(Instant.now())
//                        .build()));
//
//        puzzle.addSolve();
//
//        return userPuzzleInfo.addSolve();
//    }
//
//    @Transactional
//    public int failCommunityPuzzle(Long puzzleId, UserEntity user) {
//        CommunityPuzzle puzzle = findCommunityPuzzleById(puzzleId);
//        if(puzzle == null)
//            throw new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE);
//
//        Optional<UserCommunityPuzzle> findResult = userCommunityPuzzleRepository.findUserPuzzleInfo(user.getId(), puzzle.getId());
//        UserCommunityPuzzle userPuzzleInfo = findResult.orElseGet(() -> userCommunityPuzzleRepository.save(
//                UserCommunityPuzzle.builder()
//                        .user(user)
//                        .puzzle(puzzle)
//                        .lastTriedAt(Instant.now())
//                        .build()));
//
//        puzzle.addFail();
//
//        return userPuzzleInfo.addFail();
//    }

//    private List<String> getTags(CommunityPuzzle puzzle) {
//        List<String> tags = new ArrayList<>();
//
//        // add solved tag
//        long puzzleId = puzzle.getId();
//        long userId = puzzle.getUser().getId();
//
//        Optional<UserCommunityPuzzle> userPuzzleInfo = userCommunityPuzzleRepository.findUserPuzzleInfo(userId, puzzleId);
//        if(userPuzzleInfo.isPresent()) {
//            if(userPuzzleInfo.get().getSolvedCount() > 0)
//                tags.add(Tag.SOLVED.name());
//        }
//
//        return tags;
//    }
//
//    @Transactional(readOnly = true)
//    public List<GetCommunityPuzzleResponse> searchCommunityPuzzle(String query) {
//        List<CommunityPuzzle> puzzles = new ArrayList<>();
//
//        Optional<CommunityPuzzle> searchById = findByIdQuery(query);
//        searchById.ifPresent(puzzles::add);
//
//        List<CommunityPuzzle> searchByTitle = communityPuzzleRepository.findByTitleContaining(query);
//        puzzles.addAll(searchByTitle);
//
//        List<CommunityPuzzle> searchByAuthor = communityPuzzleRepository.findByAuthorName(query);
//        puzzles.addAll(searchByAuthor);
//
//        return buildGetCommunityPuzzleResponse(puzzles.stream().distinct().toList());
//    }
//
//    private Optional<CommunityPuzzle> findByIdQuery(String query) {
//        if(query == null || query.isEmpty())
//            return Optional.empty();
//
//        try {
//            long id = Long.parseLong(query);
//            return communityPuzzleRepository.findById(id);
//        } catch(Exception e) {
//            return Optional.empty();
//        }
//    }

}
