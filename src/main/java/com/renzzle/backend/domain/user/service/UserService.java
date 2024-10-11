package com.renzzle.backend.domain.user.service;

import com.renzzle.backend.domain.test.dao.JdbcEntityDao;
import com.renzzle.backend.domain.user.api.response.SubscriptionResponse;
import com.renzzle.backend.domain.user.api.response.UserResponse;
import com.renzzle.backend.domain.user.dao.SubscriptionRepository;
import com.renzzle.backend.domain.user.dao.UserLevelRepository;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.SubscriptionEntity;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.user.domain.UserLevel;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserLevelRepository userLevelRepository;

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
    public List<SubscriptionResponse> getUserSubscriptions(Long userId, Long id, Pageable pageable) {

        List<SubscriptionEntity> subscriptions = subscriptionRepository.findUserSubscriptions(userId, id, pageable);

        if (subscriptions.isEmpty()) {
            log.info("No subscriptions found for userId: {} and id: {}", userId, id);
        } else {
            log.info("Found {} subscriptions for userId: {} and id: {}", subscriptions.size(), userId, id);
        }

        log.info("Fetched subscriptions: {}", subscriptions);

        // SubscriptionEntity를 SubscriptionResponse로 변환하여 반환
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
}
