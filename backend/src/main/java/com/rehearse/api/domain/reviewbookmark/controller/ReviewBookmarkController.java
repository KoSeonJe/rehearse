package com.rehearse.api.domain.reviewbookmark.controller;

import com.rehearse.api.domain.reviewbookmark.dto.BookmarkExistsResponse;
import com.rehearse.api.domain.reviewbookmark.dto.BookmarkStatusFilter;
import com.rehearse.api.domain.reviewbookmark.dto.CreateReviewBookmarkRequest;
import com.rehearse.api.domain.reviewbookmark.dto.ReviewBookmarkListItem;
import com.rehearse.api.domain.reviewbookmark.dto.ReviewBookmarkListResponse;
import com.rehearse.api.domain.reviewbookmark.dto.ReviewBookmarkResponse;
import com.rehearse.api.domain.reviewbookmark.dto.UpdateBookmarkStatusRequest;
import com.rehearse.api.domain.reviewbookmark.service.ReviewBookmarkQueryService;
import com.rehearse.api.domain.reviewbookmark.service.ReviewBookmarkService;
import com.rehearse.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/review-bookmarks")
@RequiredArgsConstructor
public class ReviewBookmarkController {

    private final ReviewBookmarkService reviewBookmarkService;
    private final ReviewBookmarkQueryService reviewBookmarkQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewBookmarkResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateReviewBookmarkRequest request) {
        ReviewBookmarkResponse response = reviewBookmarkService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        reviewBookmarkService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ReviewBookmarkListResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "all") String status) {
        BookmarkStatusFilter statusFilter = BookmarkStatusFilter.from(status);
        List<ReviewBookmarkListItem> items = reviewBookmarkQueryService.listByUser(userId, statusFilter);
        return ResponseEntity.ok(ApiResponse.ok(ReviewBookmarkListResponse.from(items)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReviewBookmarkResponse>> updateStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookmarkStatusRequest request) {
        ReviewBookmarkResponse response = reviewBookmarkService.updateStatus(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<BookmarkExistsResponse>> exists(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) List<Long> timestampFeedbackIds) {
        if (timestampFeedbackIds == null || timestampFeedbackIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(new BookmarkExistsResponse(List.of())));
        }
        BookmarkExistsResponse response =
                reviewBookmarkQueryService.findBookmarkPairs(userId, timestampFeedbackIds);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
