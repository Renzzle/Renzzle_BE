package com.renzzle.backend.domain.puzzle.training.api;

import com.renzzle.backend.domain.puzzle.training.api.response.*;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.puzzle.training.api.request.*;
import com.renzzle.backend.domain.puzzle.training.service.TrainingService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.renzzle.backend.global.util.ErrorUtils.getErrorMessages;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
@Tag(name = "Training Puzzle API", description = "Training Puzzle API")
public class TrainingController {

    private final TrainingService trainingService;

    @Operation(summary = "Add training puzzle", description = "Add training puzzle & Only admins are available")
    @PostMapping("/puzzle")
    public ApiResponse<Long> addTrainingPuzzle(
            @Valid @RequestBody AddTrainingPuzzleRequest request
    ) {
        TrainingPuzzle puzzle = trainingService.createTrainingPuzzle(request);

        return ApiUtils.success(puzzle.getId());
    }

    @Operation(summary = "Modify training puzzle", description = "Modify training puzzle & Only admins are available")
    @PatchMapping("/puzzle/{puzzleId}")
    public ApiResponse<Long> modifyTrainingPuzzle(
            @PathVariable("puzzleId") Long puzzleId,
            @Valid @RequestBody ModifyTrainingPuzzleRequest request
    ) {
        TrainingPuzzle puzzle = trainingService.modifyTrainingPuzzle(puzzleId, request);

        return ApiUtils.success(puzzle.getId());
    }

    @Operation(summary = "Delete training puzzle", description = "Delete training puzzle & Only admins are available")
    @DeleteMapping("puzzle/{puzzleId}")
    public ApiResponse<Object> deleteTrainingPuzzle(@PathVariable("puzzleId") Long puzzleId) {
        trainingService.deleteTrainingPuzzle(puzzleId);
        return ApiUtils.success(null);
    }

    @Operation(summary = "Solve training puzzle", description = "Return unlocked training puzzle id (can be null???)")
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

    @Operation(summary = "Create Pack", description = "Create pack & Only admins are available")
    @PostMapping("/pack")
    public ApiResponse<Long> addTrainingPack(
            @Valid @RequestBody CreateTrainingPackRequest request
    ) {

        Pack pack = trainingService.createPack(request);

        return ApiUtils.success(pack.getId());
    }

    @Operation(summary = "Add Translation", description = "Add Translation & Only admins are available")
    @PostMapping("/pack/translation")
    public ApiResponse<Long> addTranslation(
            @Valid @RequestBody TranslationRequest request
    ) {
        trainingService.addTranslation(request);

        return ApiUtils.success(null);
    }

    @Operation(summary = "Get Training Packs", description = "Get Training Packs")
    @GetMapping("/pack")
    public ApiResponse<List<GetPackResponse>> getTrainingPack(
            @Valid @ModelAttribute GetTrainingPackRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ){

        List<GetPackResponse> packs = trainingService.getTrainingPackList(user.getUser(), request);

        return ApiUtils.success(packs);
    }

    @Operation(summary = "Purchase Training Pack", description = "Purchase Training Pack")
    @PostMapping("/pack/purchase")
    public ApiResponse<GetPackPurchaseResponse> PurchaseTrainingPack(
            @Valid @RequestBody PurchaseTrainingPackRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ){

        GetPackPurchaseResponse response = trainingService.purchaseTrainingPack(user.getUser(), request);

        return ApiUtils.success(response);
    }

    @Operation(summary = "Purchase Training Puzzle Answer", description = "Purchase Training Puzzle Answer")
    @PostMapping("/puzzle/{puzzleId}/answer")
    public ApiResponse<GetTrainingPuzzleAnswerResponse> PurchaseTrainingPuzzleAnswer(
            @Valid @RequestBody PurchaseTrainingPuzzleAnswerRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ){
        GetTrainingPuzzleAnswerResponse response = trainingService.purchaseTrainingPuzzleAnswer(user.getUser(), request);

        return ApiUtils.success(response);
    }

}
