package com.renzzle.backend.domain.user.service;

import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.user.api.response.ChangeNicknameResponse;
import com.renzzle.backend.domain.user.api.response.UserResponse;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

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
        Optional<UserEntity> persistedUser = userRepository.findById(user.getId());
        persistedUser.ifPresent(UserEntity::softDelete);
        return user.getId();
    }

    @Transactional
    public ChangeNicknameResponse changeNickname(UserEntity user, String nickname) {
        Optional<UserEntity> persistedUser = userRepository.findById(user.getId());

        if(persistedUser.isEmpty())
            throw new CustomException(ErrorCode.CANNOT_FIND_USER);

        persistedUser.get().changeNickname(nickname);
        return ChangeNicknameResponse.builder()
                .currency(persistedUser.get().getCurrency())
                .build();
    }

//    public List<GetCommunityPuzzleResponse> getUserCommunityPuzzleList(Long userid, long id, int size) {
//        List<CommunityPuzzle> puzzleList;
//
//        puzzleList = getRecommendCommunityPuzzleList(userid, id, size);
//        log.info("puzzleList in getUserCommunityPuzzleList {}", puzzleList);
//
//        return buildGetCommunityPuzzleResponse(puzzleList);
//    }
//
//    private List<CommunityPuzzle> getRecommendCommunityPuzzleList(Long userId, Long id, int size) {
//
//        Instant lastCreatedAt;
//        long lastId;
//
//        if(id == null) {
//            lastId = -1L;
//            lastCreatedAt = CONST_FUTURE_INSTANT;
//        } else {
//            lastId = id;
//            CommunityPuzzle puzzle = communityPuzzleRepository.findById(id)
//                    .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));
//            lastCreatedAt = puzzle.getCreatedAt();
//        }
//
//        log.info("Fetching puzzles for userId: {}, lastCreatedAt: {}, lastId: {}, size: {}", userId, lastCreatedAt, lastId, size);
//
//        List<CommunityPuzzle> puzzles = communityPuzzleRepository.findUserPuzzlesSortByCreatedAt(userId, lastCreatedAt, lastId, size);
//
//        // 쿼리 결과 로그
//        log.info("Fetched {} puzzles", puzzles.size());
//
//        return puzzles;
//    }
//
//    private List<GetCommunityPuzzleResponse> buildGetCommunityPuzzleResponse(List<CommunityPuzzle> puzzleList) {
//        log.info("puzzleList: {}", puzzleList);
//        List<GetCommunityPuzzleResponse> response = new ArrayList<>();
//        for(CommunityPuzzle puzzle : puzzleList) {
//            double correctRate = 0.0;
//            if(puzzle.getSolvedCount() != 0)
//                correctRate = (double) puzzle.getSolvedCount() / (puzzle.getSolvedCount() + puzzle.getFailedCount()) * 100;
//            List<String> tags = getTags(puzzle);
//            String authorName = (puzzle.getUser().getStatus() != Status.getStatus(Status.StatusName.DELETED)) ?
//                    puzzle.getUser().getNickname() : DELETED_USER;
//
//            response.add(
//                    GetCommunityPuzzleResponse.builder()
//                            .id(puzzle.getId())
//                            .title(puzzle.getTitle())
//                            .boardStatus(puzzle.getBoardStatus())
//                            .authorId(puzzle.getUser().getId())
//                            .authorName(authorName)
//                            .solvedCount(puzzle.getSolvedCount())
//                            .correctRate(correctRate)
//                            .depth(puzzle.getDepth())
//                            .difficulty(puzzle.getDifficulty().getName())
//                            .winColor(puzzle.getWinColor().getName())
//                            .likeCount(puzzle.getLikeCount())
//                            .tag(tags)
//                            .build()
//            );
//            log.info("Added puzzle response: {}", response);
//        }
//        return response;
//    }
//
//    public void deleteUserPuzzle(Long puzzleId, UserEntity user) {
//        Optional<CommunityPuzzle> puzzle = communityPuzzleRepository.findById(puzzleId);
//
//        if(puzzle.isEmpty())
//            throw new CustomException(ErrorCode.CANNOT_FIND_PUZZLE);
//
//        if (!puzzle.get().getUser().getId().equals(user.getId())) {
//            throw new CustomException(ErrorCode.UNAUTHORIZED_ACTION);
//        }
//
//
//        communityPuzzleRepository.deleteById(puzzleId);
//    }
//
//    public boolean toggleLike(Long puzzleId, UserEntity user) {
//        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
//                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_PUZZLE));
//
//        UserCommunityPuzzle userPuzzle = userCommunityPuzzleRepository.findUserPuzzleInfo(user.getId(), puzzleId)
//                .orElseGet(() -> {
//                    UserCommunityPuzzle newUserPuzzle = UserCommunityPuzzle.builder()
//                            .user(user)
//                            .puzzle(puzzle)
//                            .lastTriedAt(Instant.now())
//                            .like(false)
//                            .build();
//
//                    userCommunityPuzzleRepository.save(newUserPuzzle);
//                    return newUserPuzzle;
//                });
//
//        // 좋아요 상태 토글
//        boolean newLikeStatus = userPuzzle.toggleLike();
//        userCommunityPuzzleRepository.save(userPuzzle);
//
//        puzzle.changeLike(newLikeStatus);
//        return newLikeStatus;
//    }
//
//    public List<LikeResponse> getUserLike(Long userId, Long id, int size) {
//        List<CommunityPuzzle> likePuzzles;
//
//        List<Long> puzzleIds = userCommunityPuzzleRepository.findCommunityIdsByUserId(userId);
//
//        if (puzzleIds.isEmpty()) {
//            log.info("No liked puzzles found for userId: {}", userId);
//            return Collections.emptyList();
//        }
//        Long lastId;
//
//        lastId = Objects.requireNonNullElse(id, -1L);
//
//        likePuzzles = communityPuzzleRepository.findPuzzlesByIdsWithPagination(puzzleIds, lastId, size);
//
//        return createLikeResponse(likePuzzles);
//
//
//    }
//
//    private List<LikeResponse> createLikeResponse(List<CommunityPuzzle> likeList) {
//            List<LikeResponse> response = new ArrayList<>();
//            for(CommunityPuzzle like : likeList) {
//                double correctRate = 0.0;
//                if(like.getSolvedCount() != 0)
//                    correctRate = (double) like.getSolvedCount() / (like.getSolvedCount() + like.getFailedCount()) * 100;
//                List<String> tags = getTags(like);
//                String authorName = (like.getUser().getStatus() != Status.getStatus(Status.StatusName.DELETED)) ?
//                        like.getUser().getNickname() : DELETED_USER;
//
//                response.add(
//                        LikeResponse.builder()
//                                .id(like.getId())
//                                .title(like.getTitle())
//                                .boardStatus(like.getBoardStatus())
//                                .authorId(like.getUser().getId())
//                                .authorName(authorName)
//                                .solvedCount(like.getSolvedCount())
//                                .correctRate(correctRate)
//                                .depth(like.getDepth())
//                                .difficulty(like.getDifficulty().getName())
//                                .winColor(like.getWinColor().getName())
//                                .likeCount(like.getLikeCount())
//                                .tag(tags)
//                                .build()
//                );
//            }
//            return response;
//    }

}
