package com.renzzle.backend.domain.admin.service;

import com.renzzle.backend.domain.admin.api.request.AddAdminAccountRequest;
import com.renzzle.backend.domain.admin.api.response.GetAdminAccountResponse;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.renzzle.backend.domain.auth.domain.AdminPermission.OPERATION_MANAGE;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GetAdminAccountResponse> getAdminAccounts(UserEntity me) {
        return adminRepository.findAllByOrderByIdAsc().stream()
                .map(admin -> toResponse(admin, me))
                .toList();
    }

    @Transactional
    public GetAdminAccountResponse addAdminAccount(AddAdminAccountRequest request, UserEntity me) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));
        if (adminRepository.existsByUser(user)) {
            throw new CustomException(ErrorCode.ALREADY_ADMIN);
        }

        Admin admin = adminRepository.save(Admin.builder()
                .user(user)
                .permissions(EnumSet.copyOf(request.permissions()))
                .build());
        return toResponse(admin, me);
    }

    // Only operation admins get here and none can drop their own access, so one always remains
    @Transactional
    public GetAdminAccountResponse updatePermissions(Long adminId, Set<AdminPermission> permissions, UserEntity me) {
        Admin admin = findAdmin(adminId);
        if (isMe(admin, me) && !permissions.contains(OPERATION_MANAGE)) {
            throw new CustomException(ErrorCode.CANNOT_REVOKE_OWN_ADMIN_ACCESS);
        }

        admin.updatePermissions(permissions);
        return toResponse(admin, me);
    }

    @Transactional
    public void removeAdminAccount(Long adminId, UserEntity me) {
        Admin admin = findAdmin(adminId);
        if (isMe(admin, me)) {
            throw new CustomException(ErrorCode.CANNOT_REVOKE_OWN_ADMIN_ACCESS);
        }
        adminRepository.delete(admin);
    }

    private Admin findAdmin(Long adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_ADMIN));
    }

    private boolean isMe(Admin admin, UserEntity me) {
        return admin.getUser().getId().equals(me.getId());
    }

    private GetAdminAccountResponse toResponse(Admin admin, UserEntity me) {
        List<AdminPermission> permissions = admin.getPermissions().isEmpty()
                ? List.of()
                : List.copyOf(EnumSet.copyOf(admin.getPermissions()));
        return new GetAdminAccountResponse(
                admin.getId(),
                admin.getUser().getEmail(),
                admin.getUser().getNickname(),
                permissions,
                isMe(admin, me)
        );
    }
}
