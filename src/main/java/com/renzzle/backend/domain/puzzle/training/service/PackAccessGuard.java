package com.renzzle.backend.domain.puzzle.training.service;

import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.domain.puzzle.training.dao.PackRepository;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

// Operators manage every pack; an admin with only the pack permission manages just the packs they created.
// A missing pack or puzzle passes, so the service still answers with its own not-found error.
@Component
@RequiredArgsConstructor
public class PackAccessGuard {

    private final PackRepository packRepository;
    private final TrainingPuzzleRepository trainingPuzzleRepository;

    public boolean managesAllPacks(UserDetailsImpl admin) {
        return admin.getAdminPermissions().contains(AdminPermission.OPERATION_MANAGE);
    }

    // AI response pages look up puzzles in every pack, but reading is all they get
    public boolean readsAllPacks(UserDetailsImpl admin) {
        Set<AdminPermission> permissions = admin.getAdminPermissions();
        return permissions.contains(AdminPermission.OPERATION_MANAGE) || permissions.contains(AdminPermission.AI_RESPONSE_MANAGE);
    }

    // The creator to filter the pack list by, or null to list every pack
    public Long listCreatorFilter(UserDetailsImpl admin, boolean manageableOnly) {
        if (managesAllPacks(admin) || (!manageableOnly && readsAllPacks(admin))) {
            return null;
        }
        return admin.getUser().getId();
    }

    public boolean isForeignPack(UserDetailsImpl admin, Long packId) {
        return packId != null && packRepository.findById(packId)
                .map(pack -> !canManage(admin, pack))
                .orElse(false);
    }

    public void checkManagePack(UserDetailsImpl admin, Long packId) {
        if (isForeignPack(admin, packId)) {
            throw new CustomException(ErrorCode.ADMIN_PERMISSION_DENIED);
        }
    }

    public void checkManagePuzzle(UserDetailsImpl admin, Long puzzleId) {
        if (puzzleId == null) {
            return;
        }
        trainingPuzzleRepository.findById(puzzleId).ifPresent(puzzle -> {
            if (!canManage(admin, puzzle.getPack())) {
                throw new CustomException(ErrorCode.ADMIN_PERMISSION_DENIED);
            }
        });
    }

    public void checkReadPack(UserDetailsImpl admin, Long packId) {
        if (!readsAllPacks(admin)) {
            checkManagePack(admin, packId);
        }
    }

    public void checkReadPuzzle(UserDetailsImpl admin, Long puzzleId) {
        if (!readsAllPacks(admin)) {
            checkManagePuzzle(admin, puzzleId);
        }
    }

    private boolean canManage(UserDetailsImpl admin, Pack pack) {
        return managesAllPacks(admin) || Objects.equals(pack.getCreatedBy(), admin.getUser().getId());
    }
}
