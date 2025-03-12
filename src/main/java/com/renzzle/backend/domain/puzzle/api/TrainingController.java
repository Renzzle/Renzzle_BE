package com.renzzle.backend.domain.puzzle.api;

import com.renzzle.backend.domain.puzzle.api.request.AddTrainingPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.response.GetTrainingProgressResponse;
import com.renzzle.backend.domain.puzzle.api.response.GetTrainingPuzzleResponse;
import com.renzzle.backend.domain.puzzle.api.response.SolveTrainingPuzzleResponse;
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

    private final TrainingService lessonService;

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

        TrainingPuzzle puzzle = lessonService.createTrainingPuzzle(request);

        return ApiUtils.success(puzzle.getId());
    }

    //완료 0311
    @Operation(summary = "Delete training puzzle", description = "Delete training puzzle & Only admins are available")
    @DeleteMapping("puzzle/{puzzleId}")
    public ApiResponse<Object> deleteTrainingPuzzle(@PathVariable("puzzleId") Long puzzleId) {
        lessonService.deleteTrainingPuzzle(puzzleId);
        return ApiUtils.success(null);
    }

    //완료 0311
    @Operation(summary = "Solve training puzzle", description = "Return unlocked training puzzle id (can be null???)")
    @PostMapping("puzzle/{puzzleId}/solve")
    public ApiResponse<SolveTrainingPuzzleResponse> solveTrainingPuzzle(
            @PathVariable("puzzleId") Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl user) {

        lessonService.solveLessonPuzzle(user.getUser(), puzzleId);

        return ApiUtils.success(null);
    }

    //완료 0311
    @Operation(summary = "Get training puzzle data", description = "Return training puzzle list")
    @GetMapping("puzzle/{pack}")
    public ApiResponse<List<GetTrainingPuzzleResponse>> getLessonPuzzle(
            @PathVariable("pack") Long pack,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        if(pack == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR);
        }

        return ApiUtils.success(lessonService.getTrainingPuzzleList(user.getUser(), pack));
    }

    /*
    LEGACY : 레슨 문제 삭제
    @Operation(summary = "Delete lesson puzzle", description = "Delete lesson puzzle & Only admins are available")
    @DeleteMapping("/{lessonId}")
    public ApiResponse<Object> deleteLessonPuzzle(@PathVariable("lessonId") Long lessonId) {
        lessonService.deleteLessonPuzzle(lessonId);
        return ApiUtils.success(null);
    }
     */

    /*
    LEGACY : 레슨 문제 성공 요청
    @Operation(summary = "Solve lesson puzzle", description = "Return unlocked lesson puzzle id (can be null)")
    @PostMapping("/solve")
    public ApiResponse<SolveTrainingPuzzleResponse> solveLessonPuzzle(
            @Valid @RequestBody SolveTrainingPuzzleRequest request,
            @AuthenticationPrincipal UserDetailsImpl user,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        Long unlockedId = lessonService.solveLessonPuzzle(user.getUser(), request.puzzleId());

        return ApiUtils.success(SolveTrainingPuzzleResponse.builder()
                .unlockedId(unlockedId)
                .build());
    }

     */



    /*
                LEGACY : 레슨 문제 조회

    @Operation(summary = "Get lesson puzzle data", description = "Return lesson puzzle list")
    @GetMapping("/{chapter}")
    public ApiResponse<List<GetTrainingPuzzleResponse>> getLessonPuzzle(
            @PathVariable("chapter") Integer chapter,
            @AuthenticationPrincipal UserDetailsImpl user,
            @ModelAttribute GetTrainingPuzzleRequest request,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }
        if(chapter == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR);
        }

        int page = (request.page() != null) ? request.page() : 0;
        int size = (request.size() != null) ? request.size() : 10;

        return ApiUtils.success(lessonService.getLessonPuzzleList(user.getUser(), chapter, page, size));
    }
     */

//
//
//    @Operation(summary = "Check lesson progress", description = "Return lesson progress by chapter")
//    @GetMapping("/{chapter}/progress")
//    public ApiResponse<GetTrainingProgressResponse> getLessonProgress(
//            @PathVariable("chapter") Integer chapter,
//            @AuthenticationPrincipal UserDetailsImpl user
//    ) {
//        if(chapter == null) {
//            throw new CustomException(ErrorCode.VALIDATION_ERROR);
//        }
//        double progress = lessonService.getLessonProgress(user.getUser(), chapter);
//
//        return ApiUtils.success(GetTrainingProgressResponse.builder()
//                .progress(progress)
//                .build());
//    }

}
