package com.renzzle.backend.domain.puzzle.api;

import com.renzzle.backend.domain.puzzle.api.request.AddLessonPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.request.GetLessonPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.request.SolveLessonPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.response.GetLessonProgressResponse;
import com.renzzle.backend.domain.puzzle.api.response.GetLessonPuzzleResponse;
import com.renzzle.backend.domain.puzzle.api.response.SolveLessonPuzzleResponse;
import com.renzzle.backend.domain.puzzle.domain.LessonPuzzle;
import com.renzzle.backend.domain.puzzle.service.LessonService;
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
@RequestMapping("/api/lesson")
@RequiredArgsConstructor
@Tag(name = "Lesson Puzzle API", description = "Lesson Puzzle API")
public class LessonController {

    private final LessonService lessonService;

    @Operation(summary = "Add lesson puzzle", description = "Add lesson puzzle & Only admins are available")
    @PostMapping("")
    public ApiResponse<Long> addLessonPuzzle(
            @Valid @RequestBody AddLessonPuzzleRequest request,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        LessonPuzzle puzzle = lessonService.createLessonPuzzle(request);

        return ApiUtils.success(puzzle.getId());
    }

    @Operation(summary = "Delete lesson puzzle", description = "Delete lesson puzzle & Only admins are available")
    @DeleteMapping("/{lessonId}")
    public ApiResponse<Object> deleteLessonPuzzle(@PathVariable("lessonId") Long lessonId) {
        lessonService.deleteLessonPuzzle(lessonId);
        return ApiUtils.success(null);
    }

    @Operation(summary = "Solve lesson puzzle", description = "Return unlocked lesson puzzle id (can be null)")
    @PostMapping("/solve")
    public ApiResponse<SolveLessonPuzzleResponse> solveLessonPuzzle(
            @Valid @RequestBody SolveLessonPuzzleRequest request,
            @AuthenticationPrincipal UserDetailsImpl user,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        Long unlockedId = lessonService.solveLessonPuzzle(user.getUser(), request.puzzleId());

        return ApiUtils.success(SolveLessonPuzzleResponse.builder()
                .unlockedId(unlockedId)
                .build());
    }

    @Operation(summary = "Get lesson puzzle data", description = "Return lesson puzzle list")
    @GetMapping("/{chapter}")
    public ApiResponse<List<GetLessonPuzzleResponse>> getLessonPuzzle(
            @PathVariable("chapter") Integer chapter,
            @AuthenticationPrincipal UserDetailsImpl user,
            @ModelAttribute GetLessonPuzzleRequest request,
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

    @Operation(summary = "Check lesson progress", description = "Return lesson progress by chapter")
    @GetMapping("/{chapter}/progress")
    public ApiResponse<GetLessonProgressResponse> getLessonProgress(
            @PathVariable("chapter") Integer chapter,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        if(chapter == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR);
        }
        double progress = lessonService.getLessonProgress(user.getUser(), chapter);

        return ApiUtils.success(GetLessonProgressResponse.builder()
                .progress(progress)
                .build());
    }

}
