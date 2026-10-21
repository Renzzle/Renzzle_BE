package com.renzzle.backend.domain.admin.service;

import com.renzzle.backend.domain.admin.api.request.AddAdminAccountRequest;
import com.renzzle.backend.domain.admin.api.response.GetAdminAccountResponse;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static com.renzzle.backend.domain.auth.domain.AdminPermission.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminAccountService adminAccountService;

    private final UserEntity me = TestUserEntityBuilder.builder().withId(1L).withEmail("me@renzzle.com").withNickname("me").withStatus(Status.getDefaultStatus()).build();
    private final UserEntity other = TestUserEntityBuilder.builder().withId(2L).withEmail("other@renzzle.com").withNickname("other").withStatus(Status.getDefaultStatus()).build();

    @Test
    void addAdminAccount_WhenUserSignedUp_ThenSavesAdminWithThePermissions() {
        // given
        when(userRepository.findByEmail("other@renzzle.com")).thenReturn(Optional.of(other));
        when(adminRepository.existsByUser(other)).thenReturn(false);
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        GetAdminAccountResponse response = adminAccountService.addAdminAccount(
                new AddAdminAccountRequest(" Other@Renzzle.com ", Set.of(COMMUNITY_MANAGE, PACK_MANAGE)), me);

        // then
        assertThat(response.email()).isEqualTo("other@renzzle.com");
        assertThat(response.permissions()).containsExactly(PACK_MANAGE, COMMUNITY_MANAGE);
        assertThat(response.isMe()).isFalse();
    }

    @Test
    void addAdminAccount_WhenNoAccountUsesTheEmail_ThenThrowsCannotFindUser() {
        // given
        when(userRepository.findByEmail("nobody@renzzle.com")).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class, () -> adminAccountService.addAdminAccount(
                new AddAdminAccountRequest("nobody@renzzle.com", Set.of(PACK_MANAGE)), me));

        // then
        assertEquals(ErrorCode.CANNOT_FIND_USER, exception.getErrorCode());
        verify(adminRepository, never()).save(any());
    }

    @Test
    void addAdminAccount_WhenAlreadyAdmin_ThenThrowsWithoutSaving() {
        // given
        when(userRepository.findByEmail("other@renzzle.com")).thenReturn(Optional.of(other));
        when(adminRepository.existsByUser(other)).thenReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> adminAccountService.addAdminAccount(
                new AddAdminAccountRequest("other@renzzle.com", Set.of(PACK_MANAGE)), me));

        // then
        assertEquals(ErrorCode.ALREADY_ADMIN, exception.getErrorCode());
        verify(adminRepository, never()).save(any());
    }

    @Test
    void updatePermissions_WhenRemovingOwnOperationPermission_ThenThrowsWithoutChanging() {
        // given
        Admin myAdmin = admin(10L, me, EnumSet.allOf(AdminPermission.class));
        when(adminRepository.findById(10L)).thenReturn(Optional.of(myAdmin));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> adminAccountService.updatePermissions(10L, Set.of(PACK_MANAGE), me));

        // then
        assertEquals(ErrorCode.CANNOT_REVOKE_OWN_ADMIN_ACCESS, exception.getErrorCode());
        assertThat(myAdmin.getPermissions()).containsExactlyInAnyOrder(AdminPermission.values());
    }

    @Test
    void updatePermissions_WhenChangingOwnOtherPermissions_ThenKeepsOperation() {
        // given
        Admin myAdmin = admin(10L, me, EnumSet.allOf(AdminPermission.class));
        when(adminRepository.findById(10L)).thenReturn(Optional.of(myAdmin));

        // when
        GetAdminAccountResponse response = adminAccountService.updatePermissions(10L, Set.of(OPERATION_MANAGE), me);

        // then
        assertThat(response.permissions()).containsExactly(OPERATION_MANAGE);
        assertThat(response.isMe()).isTrue();
    }

    @Test
    void updatePermissions_WhenAnotherAdmin_ThenReplacesTheirPermissions() {
        // given
        Admin otherAdmin = admin(20L, other, EnumSet.of(OPERATION_MANAGE));
        when(adminRepository.findById(20L)).thenReturn(Optional.of(otherAdmin));

        // when
        adminAccountService.updatePermissions(20L, Set.of(AI_RESPONSE_MANAGE), me);

        // then
        assertThat(otherAdmin.getPermissions()).containsExactly(AI_RESPONSE_MANAGE);
    }

    @Test
    void updatePermissions_WhenAdminMissing_ThenThrowsCannotFindAdmin() {
        // given
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> adminAccountService.updatePermissions(99L, Set.of(PACK_MANAGE), me));

        // then
        assertEquals(ErrorCode.CANNOT_FIND_ADMIN, exception.getErrorCode());
    }

    @Test
    void removeAdminAccount_WhenRemovingMyself_ThenThrowsWithoutDeleting() {
        // given
        Admin myAdmin = admin(10L, me, EnumSet.allOf(AdminPermission.class));
        when(adminRepository.findById(10L)).thenReturn(Optional.of(myAdmin));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> adminAccountService.removeAdminAccount(10L, me));

        // then
        assertEquals(ErrorCode.CANNOT_REVOKE_OWN_ADMIN_ACCESS, exception.getErrorCode());
        verify(adminRepository, never()).delete(any());
    }

    @Test
    void removeAdminAccount_WhenAnotherAdmin_ThenDeletesOnlyTheAdminRow() {
        // given
        Admin otherAdmin = admin(20L, other, EnumSet.of(PACK_MANAGE));
        when(adminRepository.findById(20L)).thenReturn(Optional.of(otherAdmin));

        // when
        adminAccountService.removeAdminAccount(20L, me);

        // then
        verify(adminRepository).delete(otherAdmin);
        verify(userRepository, never()).delete(any());
    }

    private static Admin admin(Long id, UserEntity user, Set<AdminPermission> permissions) {
        return Admin.builder().id(id).user(user).permissions(EnumSet.copyOf(permissions)).build();
    }
}
