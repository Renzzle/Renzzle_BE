package com.renzzle.backend.domain.puzzle.training.api;

import com.renzzle.backend.domain.puzzle.training.api.response.*;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.puzzle.training.api.request.*;
import com.renzzle.backend.domain.puzzle.training.service.PackAccessGuard;
import com.renzzle.backend.domain.puzzle.training.service.TrainingService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
@Tag(name = "Training Puzzle API", description = "Training Puzzle API")
public class TrainingController {

    private final TrainingService trainingService;
    private final PackAccessGuard packAccessGuard;

    @Operation(summary = "Add training puzzle", description = "Add training puzzle & Only admins are available")
    @PostMapping("/puzzle")
    public ApiResponse<Long> addTrainingPuzzle(
            @Valid @RequestBody AddTrainingPuzzleRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin
    ) {
        packAccessGuard.checkManagePack(admin, request.packId());
        TrainingPuzzle puzzle = trainingService.createTrainingPuzzle(request);

        return ApiUtils.success(puzzle.getId());
    }

    @Operation(summary = "Modify training puzzle", description = "Modify training puzzle & Only admins are available")
    @PatchMapping("/puzzle/{puzzleId}")
    public ApiResponse<Long> modifyTrainingPuzzle(
            @PathVariable("puzzleId") Long puzzleId,
            @Valid @RequestBody ModifyTrainingPuzzleRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin
    ) {
        // A packId moves the puzzle, so the destination pack has to be the admin's too
        packAccessGuard.checkManagePuzzle(admin, puzzleId);
        packAccessGuard.checkManagePack(admin, request.packId());
        TrainingPuzzle puzzle = trainingService.modifyTrainingPuzzle(puzzleId, request);

        return ApiUtils.success(puzzle.getId());
    }

    @Operation(summary = "Delete training puzzle", description = "Delete training puzzle & Only admins are available")
    @DeleteMapping("puzzle/{puzzleId}")
    public ApiResponse<Object> deleteTrainingPuzzle(
            @PathVariable("puzzleId") Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl admin
    ) {
        packAccessGuard.checkManagePuzzle(admin, puzzleId);
        trainingService.deleteTrainingPuzzle(puzzleId);
        return ApiUtils.success(null);
    }

    @Operation(summary = "Solve training puzzle", description = "Return currency reward earned for solving (0 if already solved before)")
    @PostMapping("puzzle/{puzzleId}/solve")
    public ApiResponse<SolveTrainingPuzzleResponse> solveTrainingPuzzle(
            @PathVariable("puzzleId") Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl user) {

        SolveTrainingPuzzleResponse response = trainingService.solveTrainingPuzzle(user.getUser(), puzzleId, true);

        return ApiUtils.success(response);
    }

    @Operation(summary = "Get training puzzle data", description = "Return training puzzle list")
    @GetMapping("puzzle/{pack}")
    public ApiResponse<List<GetTrainingPuzzleResponse>> getTrainingPuzzle(
            @PathVariable("pack") Long pack,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiUtils.success(trainingService.getTrainingPuzzleList(user.getUser(), pack));
    }

    @Operation(summary = "Create Pack", description = "Create pack (private unless isPublic is true) & Only admins are available")
    @PostMapping("/pack")
    public ApiResponse<Long> addTrainingPack(
            @Valid @RequestBody CreateTrainingPackRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin
    ) {

        Pack pack = trainingService.createPack(request, admin.getUser().getId());

        return ApiUtils.success(pack.getId());
    }

    @Operation(summary = "Add Translation", description = "Add Translation & Only admins are available")
    @PostMapping("/pack/translation")
    public ApiResponse<Long> addTranslation(
            @Valid @RequestBody TranslationRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin
    ) {
        packAccessGuard.checkManagePack(admin, request.packId());
        trainingService.addTranslation(request);

        return ApiUtils.success(null);
    }

    @Operation(summary = "Update Pack", description = "Update pack translations, price, difficulty, visibility & Only admins are available")
    @PatchMapping("/pack/{packId}")
    public ApiResponse<Long> updateTrainingPack(
            @PathVariable("packId") Long packId,
            @Valid @RequestBody UpdateTrainingPackRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin
    ) {
        packAccessGuard.checkManagePack(admin, packId);
        Pack pack = trainingService.updatePack(packId, request);
        return ApiUtils.success(pack.getId());
    }

    @Operation(summary = "Delete Pack", description = "Delete pack and its training puzzles & Only admins are available")
    @DeleteMapping("/pack/{packId}")
    public ApiResponse<Object> deleteTrainingPack(
            @PathVariable("packId") Long packId,
            @AuthenticationPrincipal UserDetailsImpl admin
    ) {
        packAccessGuard.checkManagePack(admin, packId);
        trainingService.deletePack(packId);
        return ApiUtils.success(null);
    }

    @Operation(summary = "Get Training Packs", description = "Get Training Packs")
    @GetMapping("/pack")
    public ApiResponse<List<GetPackResponse>> getTrainingPack(
            @Valid @ParameterObject @ModelAttribute GetTrainingPackRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ){

        List<GetPackResponse> packs = trainingService.getTrainingPackList(user.getUser(), request);

        return ApiUtils.success(packs);
    }

    @Operation(summary = "Purchase Training Pack", description = "Purchase Training Pack")
    @PostMapping("/pack/purchase")
    public ApiResponse<GetPackPurchaseResponse> purchaseTrainingPack(
            @Valid @RequestBody PurchaseTrainingPackRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ){

        GetPackPurchaseResponse response = trainingService.purchaseTrainingPack(user.getUser(), request);

        return ApiUtils.success(response);
    }

    @Operation(summary = "Purchase Training Puzzle Answer", description = "Purchase Training Puzzle Answer")
    @PostMapping("/puzzle/{puzzleId}/answer")
    public ApiResponse<GetTrainingPuzzleAnswerResponse> purchaseTrainingPuzzleAnswer(
            @PathVariable Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl user
    ){
        GetTrainingPuzzleAnswerResponse response = trainingService.purchaseTrainingPuzzleAnswer(user.getUser(), puzzleId);

        return ApiUtils.success(response);
    }

}
