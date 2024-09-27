package com.renzzle.backend.domain.user.service;

import com.renzzle.backend.domain.user.api.response.LikeResponse;
import com.renzzle.backend.domain.user.api.response.SubscriptionResponse;
import com.renzzle.backend.domain.user.api.response.UserResponse;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.user.domain.UserLevel;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
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
public class UserService {

    private final UserRepository userRepository;
//    private final LikeRepository likeRepository;

    public UserResponse getUser(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .level(user.getLevel())
                .profile(user.getColor())
                .build();
    }

    @Transactional
    public Long deleteUser(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        user.getStatus().setStatus(Status.StatusName.DELETED);

        return userId;
    }

    @Transactional
    public UserResponse updateUserLevel(Long userId, UserLevel.LevelName levelName) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_LOAD_USER_INFO));

        // 사용자 레벨 업데이트
        user.getLevel().setLevel(levelName);

        // 업데이트된 사용자 정보 반환
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .level(user.getLevel())
                .profile(user.getColor())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getUserSubscriptions(Long userId, Long id, int size) {
        // 구독한 유저 목록을 ID와 페이지 사이즈 기준으로 조회
        List<SubscriptionEntity> subscriptions = subscriptionRepository.findUserSubscriptions(userId, id, size);

        // SubscriptionEntity를 SubscriptionResponse로 변환하여 반환
        return subscriptions.stream()
                .map(this::createSubscriptionResponse)
                .collect(Collectors.toList());
    }

    // SubscriptionEntity를 SubscriptionResponse로 변환
    private SubscriptionResponse createSubscriptionResponse(SubscriptionEntity subscriptionEntity) {
        return SubscriptionResponse.builder()
                .userId(subscriptionEntity.getSubscribedUser().getId())  // 구독된 유저 ID
                .nickname(subscriptionEntity.getSubscribedUser().getNickname())  // 구독된 유저 닉네임
                .profile(subscriptionEntity.getSubscribedUser().getColor().getName())  // 구독된 유저 프로필 색상
                .build();
    }


}
