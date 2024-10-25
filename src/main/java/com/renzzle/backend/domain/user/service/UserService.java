package com.renzzle.backend.domain.user.service;

import com.renzzle.backend.domain.puzzle.api.response.GetCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.domain.Tag;
import com.renzzle.backend.domain.puzzle.domain.UserCommunityPuzzle;
import com.renzzle.backend.domain.user.api.response.LikeResponse;
import com.renzzle.backend.domain.user.api.response.SubscriptionResponse;
import com.renzzle.backend.domain.user.api.response.UserResponse;
import com.renzzle.backend.domain.user.dao.SubscriptionRepository;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.SubscriptionEntity;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.user.domain.UserLevel;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.renzzle.backend.global.common.constant.StringConstant.DELETED_USER;
import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;


    public UserResponse getUser(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .level(user.getLevel().getName())
                .profile(user.getColor().getName())
                .build();
    }

    @Transactional
    public Long deleteUser(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        subscriptionRepository.deleteByUserId(userId);

        userRepository.deleteById(userId);
        return userId;
    }

    @Transactional
    public UserResponse updateUserLevel(UserEntity user, String levelName) {

        UserLevel newLevel = UserLevel.getLevel(levelName);

        // 사용자 레벨 업데이트
        user.setUserLevel(newLevel);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .level(user.getLevel().getName())
                .profile(user.getColor().getName())
                .build();
    }


    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getUserSubscriptions(Long userId, Long id, int size) {
        List<SubscriptionEntity> subscriptions;

        if (id == null) {
            // ID가 없으면 처음부터 조회
            subscriptions = subscriptionRepository.findFirstUserSubscriptions(userId, size);
        } else {
            // ID가 있으면 해당 ID 이후부터 조회
            subscriptions = subscriptionRepository.findUserSubscriptionsFromId(userId, id, size);
        }

        if (subscriptions.isEmpty()) {
            log.info("No subscriptions found for userId: {} and id: {}", userId, id);
        } else {
            log.info("Found {} subscriptions for userId: {} and id: {}", subscriptions.size(), userId, id);
        }

        log.info("Fetched subscriptions: {}", subscriptions);

        return subscriptions.stream()
                .map(this::createSubscriptionResponse)
                .collect(Collectors.toList());
    }

    // SubscriptionEntity를 SubscriptionResponse로 변환
    private SubscriptionResponse createSubscriptionResponse(SubscriptionEntity subscriptionEntity) {
        return SubscriptionResponse.builder()
                .userId(subscriptionEntity.getSubscribedTo().getId())
                .nickname(subscriptionEntity.getSubscribedTo().getNickname())
                .profile(subscriptionEntity.getSubscribedTo().getColor())
                .build();
    }



    @Transactional
    public Boolean changeSubscription(Long subscriberId, Long subscribedToId) {

        if (subscriberId.equals(subscribedToId)) {
            throw new CustomException(ErrorCode.INVALID_SUBSCRIPTION_REQUEST);
        }

        UserEntity subscribedTo = userRepository.findById(subscribedToId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        Optional<SubscriptionEntity> existingSubscription = subscriptionRepository
                .findBySubscriberAndSubscribedTo(subscriberId, subscribedToId);

        if (existingSubscription.isPresent()) {
            subscriptionRepository.delete(existingSubscription.get());
            return false;
        } else {
            SubscriptionEntity newSubscription = SubscriptionEntity.builder()
                    .subscriber(userRepository.findById(subscriberId)
                            .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER)))
                    .subscribedTo(subscribedTo)
                    .build();
            subscriptionRepository.save(newSubscription);
            return true;
        }
    }

    public List<GetCommunityPuzzleResponse> getUserCommunityPuzzleList(Long userid, long id, int size) {
        List<CommunityPuzzle> puzzleList;

        puzzleList = getRecommendCommunityPuzzleList(userid, id, size);


        return buildGetCommunityPuzzleResponse(puzzleList);
    }

    private List<CommunityPuzzle> getRecommendCommunityPuzzleList(Long userId, Long id, int size) {

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

        return communityPuzzleRepository.findUserPuzzlesSortByCreatedAt(userId, lastCreatedAt, lastId, size);
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


//


    public void deleteUserPuzzle(Long puzzleId, UserEntity user) {
        Optional<CommunityPuzzle> puzzle = communityPuzzleRepository.findById(puzzleId);

        if(puzzle.isEmpty())
            throw new CustomException(ErrorCode.CANNOT_FIND_PUZZLE);

        if (!puzzle.get().getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACTION);
        }


        communityPuzzleRepository.deleteById(puzzleId);
    }

    public boolean toggleLike(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_PUZZLE));

        UserCommunityPuzzle userPuzzle = userCommunityPuzzleRepository.findUserPuzzleInfo(user.getId(), puzzleId)
                .orElseGet(() -> {
                    UserCommunityPuzzle newUserPuzzle = UserCommunityPuzzle.builder()
                            .user(user)
                            .puzzle(puzzle)
                            .lastTriedAt(Instant.now())
                            .build();
                    return userCommunityPuzzleRepository.save(newUserPuzzle);
                });

        // 좋아요 상태 토글
        boolean newLikeStatus = userPuzzle.toggleLike();
        userCommunityPuzzleRepository.save(userPuzzle);

        puzzle.changeLike(newLikeStatus);
        return newLikeStatus;
    }

    public List<LikeResponse> getUserLike(Long userId, Long id, int size) {
        List<CommunityPuzzle> likePuzzles;

        List<Long> puzzleIds = userCommunityPuzzleRepository.findCommunityIdsByUserId(userId);

        if (puzzleIds.isEmpty()) {
            log.info("No liked puzzles found for userId: {}", userId);
            return Collections.emptyList();
        }
        Long lastId;

        lastId = Objects.requireNonNullElse(id, -1L);

        likePuzzles = communityPuzzleRepository.findPuzzlesByIdsWithPagination(puzzleIds, lastId, size);

        return createLikeResponse(likePuzzles);


    }

    private List<LikeResponse> createLikeResponse(List<CommunityPuzzle> likeList) {
            List<LikeResponse> response = new ArrayList<>();
            for(CommunityPuzzle like : likeList) {
                double correctRate = 0.0;
                if(like.getSolvedCount() != 0)
                    correctRate = (double) like.getSolvedCount() / (like.getSolvedCount() + like.getFailedCount()) * 100;
                List<String> tags = getTags(like);
                String authorName = (like.getUser().getStatus() != Status.getStatus(Status.StatusName.DELETED)) ?
                        like.getUser().getNickname() : DELETED_USER;

                response.add(
                        LikeResponse.builder()
                                .id(like.getId())
                                .title(like.getTitle())
                                .boardStatus(like.getBoardStatus())
                                .authorId(like.getUser().getId())
                                .authorName(authorName)
                                .solvedCount(like.getSolvedCount())
                                .correctRate(correctRate)
                                .depth(like.getDepth())
                                .difficulty(like.getDifficulty().getName())
                                .winColor(like.getWinColor().getName())
                                .likeCount(like.getLikeCount())
                                .tag(tags)
                                .build()
                );
            }
            return response;
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
}
