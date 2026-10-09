package com.renzzle.backend.domain.user.service;

import com.renzzle.backend.domain.appinfo.service.AppInfoService;
import com.renzzle.backend.domain.auth.service.AuthService;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.user.api.response.ChangeNicknameResponse;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.constant.ItemPrice;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.util.Optional;

import static com.renzzle.backend.global.common.constant.ItemPrice.CHANGE_NICKNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private Clock clock;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommunityPuzzleRepository communityPuzzleRepository;
    @Mock
    private UserCommunityPuzzleRepository userCommunityPuzzleRepository;
    @Mock
    private AppInfoService appInfoService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setup() {
        lenient().when(appInfoService.getPrice(any())).thenAnswer(invocation -> invocation.<ItemPrice>getArgument(0).getDefaultPrice());
    }

    @Test
    void changeNickname_WhenNicknameIsFree_ThenChargesAndRenames() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder()
                .withId(1L)
                .withStatus(Status.getDefaultStatus())
                .withCurrency(CHANGE_NICKNAME.getDefaultPrice())
                .build();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

        // When
        ChangeNicknameResponse response = userService.changeNickname(user, "newName");

        // Then
        assertThat(user.getNickname()).isEqualTo("newName");
        assertThat(user.getCurrency()).isZero();
        assertThat(response.price()).isEqualTo(CHANGE_NICKNAME.getDefaultPrice());
    }

    @Test
    void changeNickname_WhenAnotherUserTakesTheNicknameMeanwhile_ThenThrowsDuplicateNickname() {
        // Given: the name was free when checked, but a concurrent change committed it first
        UserEntity user = TestUserEntityBuilder.builder()
                .withId(1L)
                .withStatus(Status.getDefaultStatus())
                .withCurrency(CHANGE_NICKNAME.getDefaultPrice())
                .build();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        doThrow(new DataIntegrityViolationException("Duplicate entry")).when(userRepository).flush();

        // When
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.changeNickname(user, "taken"));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

}
