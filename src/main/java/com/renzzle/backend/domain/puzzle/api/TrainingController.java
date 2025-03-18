package com.renzzle.backend.domain.puzzle.api;

import com.renzzle.backend.domain.puzzle.api.request.*;
import com.renzzle.backend.domain.puzzle.api.response.GetPackResponse;
import com.renzzle.backend.domain.puzzle.api.response.GetTrainingPuzzleAnswerResponse;
import com.renzzle.backend.domain.puzzle.api.response.GetTrainingPuzzleResponse;
import com.renzzle.backend.domain.puzzle.api.response.SolveTrainingPuzzleResponse;
import com.renzzle.backend.domain.puzzle.domain.Pack;
import com.renzzle.backend.domain.puzzle.domain.TrainingPuzzle;
import com.renzzle.backend.domain.puzzle.service.TrainingService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.renzzle.backend.global.util.ErrorUtils.getErrorMessages;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
@Tag(name = "Training Puzzle API", description = "Training Puzzle API")
public class TrainingController {

    private final TrainingService trainingService;

    //완료 0311
    @Operation(summary = "Add training puzzle", description = "Add training puzzle & Only admins are available")
    @PostMapping("/puzzle")
    public ApiResponse<Long> addTrainingPuzzle(
            @Valid @RequestBody AddTrainingPuzzleRequest request,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        TrainingPuzzle puzzle = trainingService.createTrainingPuzzle(request);

        return ApiUtils.success(puzzle.getId());
    }

    //완료 0311
    @Operation(summary = "Delete training puzzle", description = "Delete training puzzle & Only admins are available")
    @DeleteMapping("puzzle/{puzzleId}")
    public ApiResponse<Object> deleteTrainingPuzzle(@PathVariable("puzzleId") Long puzzleId) {
        trainingService.deleteTrainingPuzzle(puzzleId);
        return ApiUtils.success(null);
    }

    //완료 0311
    @Operation(summary = "Solve training puzzle", description = "Return unlocked training puzzle id (can be null???)")
    @PostMapping("puzzle/{puzzleId}/solve")
    public ApiResponse<SolveTrainingPuzzleResponse> solveTrainingPuzzle(
            @PathVariable("puzzleId") Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl user) {

        trainingService.solveLessonPuzzle(user.getUser(), puzzleId);

        return ApiUtils.success(null);
    }

    //완료 0311
//    @Operation(summary = "Get training puzzle data", description = "Return training puzzle list")
//    @GetMapping("puzzle/{pack}")
//    public ApiResponse<List<GetTrainingPuzzleResponse>> getTrainingPuzzle(
//            @PathVariable("pack") Long pack,
//            @AuthenticationPrincipal UserDetailsImpl user
//    ) {
//        if(pack == null) {
//            throw new CustomException(ErrorCode.VALIDATION_ERROR);
//        }
//
//        return ApiUtils.success(trainingService.getTrainingPuzzleList(user.getUser(), pack));
//    }

    //완료 0313
    @Operation(summary = "Create Pack", description = "Create pack & Only admins are available")
    @PostMapping("/pack")
    public ApiResponse<Long> addTrainingPuzzle(
            @Valid @RequestBody CreatePackRequest request,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        Pack pack = trainingService.createPack(request);

        return ApiUtils.success(pack.getId());
    }

    //완료 0313
    @Operation(summary = "Add Translation", description = "Add Translation & Only admins are available")
    @PostMapping("/pack/translation")
    public ApiResponse<Long> addTranslation(
            @Valid @RequestBody TranslationRequest request,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }
        trainingService.addTranslation(request);


//        Pack pack = lessonService.createPack(request);

        return ApiUtils.success(null);
    }

//    //완료 0314
//    @Operation(summary = "Get Training Packs", description = "Get Training Packs")
//    @GetMapping("/pack")
//    public ApiResponse<List<GetPackResponse>> getTrainigPack(
//            @RequestParam(name = "difficulty", required = true) String difficulty,
//            @RequestParam(name = "lang", defaultValue = "en") String lang,
//            @AuthenticationPrincipal UserDetailsImpl user
//    ){
//        List<GetPackResponse> packs = trainingService.getTrainingPackList(user.getUser(), difficulty, lang);
//
//        return ApiUtils.success(packs);
//    }

    // 0315
    @Operation(summary = "Purchase Training Pack", description = "Purchase Training Pack")
    @PostMapping("/pack/purchase")
    public ApiResponse<Integer> PurchaseTrainingPack(
            @Valid @RequestBody PurchaseTrainingPackRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetailsImpl user
    ){

        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        Integer currency = trainingService.purchaseTrainingPack(user.getUser(), request);

        return ApiUtils.success(currency);
    }
    // 0315
    @Operation(summary = "Purchase Training Puzzle Answer", description = "Purchase Training Puzzle Answer")
    @PostMapping("/puzzle/{puzzleId}/answer")
    public ApiResponse<GetTrainingPuzzleAnswerResponse> PurchaseTrainingPuzzleAnswer(
            @Valid @RequestBody PurchaseTrainingPuzzleAnswerRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetailsImpl user
    ){

        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        GetTrainingPuzzleAnswerResponse response = trainingService.purchaseTrainingPuzzleAnswer(user.getUser(), request);

        return ApiUtils.success(response);
    }
}
