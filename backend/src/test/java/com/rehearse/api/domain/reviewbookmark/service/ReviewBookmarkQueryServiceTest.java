package com.rehearse.api.domain.reviewbookmark.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetFeedback;
import com.rehearse.api.domain.questionset.entity.QuestionType;
import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import com.rehearse.api.domain.reviewbookmark.dto.BookmarkExistsResponse;
import com.rehearse.api.domain.reviewbookmark.dto.BookmarkIdPair;
import com.rehearse.api.domain.reviewbookmark.dto.BookmarkStatusFilter;
import com.rehearse.api.domain.reviewbookmark.dto.ReviewBookmarkListItem;
import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;
import com.rehearse.api.domain.reviewbookmark.repository.ReviewBookmarkRepository;
import com.rehearse.api.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReviewBookmarkQueryServiceTest {

    @InjectMocks
    private ReviewBookmarkQueryService reviewBookmarkQueryService;

    @Mock
    private ReviewBookmarkRepository reviewBookmarkRepository;

    @Test
    @DisplayName("listByUser - status=all 이면 전체 반환")
    void listByUser_all() {
        Long userId = 1L;
        ReviewBookmark b1 = createBookmark(1L, false);
        ReviewBookmark b2 = createBookmark(2L, true);

        given(reviewBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of(b1, b2));

        List<ReviewBookmarkListItem> result = reviewBookmarkQueryService.listByUser(userId, BookmarkStatusFilter.ALL);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("listByUser - status=in_progress 이면 DB-레벨로 미해결만 조회")
    void listByUser_inProgress() {
        Long userId = 1L;
        ReviewBookmark inProgress = createBookmark(1L, false);

        given(reviewBookmarkRepository.findByUserIdAndResolvedAtIsNullOrderByCreatedAtDesc(userId))
                .willReturn(List.of(inProgress));

        List<ReviewBookmarkListItem> result = reviewBookmarkQueryService.listByUser(userId, BookmarkStatusFilter.IN_PROGRESS);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("listByUser - status=resolved 이면 DB-레벨로 해결된 것만 조회")
    void listByUser_resolved() {
        Long userId = 1L;
        ReviewBookmark resolved = createBookmark(2L, true);

        given(reviewBookmarkRepository.findByUserIdAndResolvedAtIsNotNullOrderByCreatedAtDesc(userId))
                .willReturn(List.of(resolved));

        List<ReviewBookmarkListItem> result = reviewBookmarkQueryService.listByUser(userId, BookmarkStatusFilter.RESOLVED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("listByUser - 북마크가 없으면 빈 목록 반환")
    void listByUser_empty() {
        Long userId = 1L;

        given(reviewBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of());

        List<ReviewBookmarkListItem> result = reviewBookmarkQueryService.listByUser(userId, BookmarkStatusFilter.ALL);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listByUser - question이 null이어도 NPE 없이 매핑됨")
    void listByUser_nullableQuestion() {
        Long userId = 1L;
        ReviewBookmark bookmark = createBookmarkWithNullQuestion(10L);

        given(reviewBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of(bookmark));

        List<ReviewBookmarkListItem> result = reviewBookmarkQueryService.listByUser(userId, BookmarkStatusFilter.ALL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).questionText()).isNull();
        assertThat(result.get(0).modelAnswer()).isNull();
    }

    @Test
    @DisplayName("findBookmarkPairs - userId와 tsfIds로 BookmarkIdPair 목록 반환")
    void findBookmarkPairs_returnsPairs() {
        Long userId = 1L;
        List<Long> tsfIds = List.of(10L, 20L);
        List<BookmarkIdPair> pairs = List.of(
                new BookmarkIdPair(10L, 100L),
                new BookmarkIdPair(20L, 200L)
        );

        given(reviewBookmarkRepository.findBookmarkPairs(userId, tsfIds)).willReturn(pairs);

        BookmarkExistsResponse response = reviewBookmarkQueryService.findBookmarkPairs(userId, tsfIds);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).timestampFeedbackId()).isEqualTo(10L);
        assertThat(response.items().get(0).bookmarkId()).isEqualTo(100L);
    }

    // ====== helpers ======

    private ReviewBookmark createBookmark(Long bookmarkId, boolean resolved) {
        Interview interview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .durationMinutes(30)
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
        ReflectionTestUtils.setField(interview, "createdAt", LocalDateTime.now());

        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category("EXPERIENCE")
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(qs, "id", 1L);

        QuestionSetFeedback qsf = QuestionSetFeedback.builder()
                .questionSet(qs)
                .questionSetComment("전반적으로 좋았습니다.")
                .build();
        ReflectionTestUtils.setField(qsf, "id", 1L);

        Question question = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("자기소개를 해주세요.")
                .modelAnswer("모범 답변입니다.")
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(question, "id", 1L);

        TimestampFeedback tsf = TimestampFeedback.builder()
                .question(question)
                .startMs(0L)
                .endMs(5000L)
                .transcript("안녕하세요.")
                .coachingImprovement("더 자세히 설명하세요.")
                .isAnalyzed(true)
                .build();
        ReflectionTestUtils.setField(tsf, "id", bookmarkId * 10);
        ReflectionTestUtils.setField(tsf, "questionSetFeedback", qsf);

        User user = org.mockito.Mockito.mock(User.class);
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(tsf)
                .build();
        ReflectionTestUtils.setField(bookmark, "id", bookmarkId);
        ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.now());

        if (resolved) {
            bookmark.markResolved();
        }

        return bookmark;
    }

    private ReviewBookmark createBookmarkWithNullQuestion(Long bookmarkId) {
        Interview interview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .durationMinutes(30)
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
        ReflectionTestUtils.setField(interview, "createdAt", LocalDateTime.now());

        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category("CONCEPT")
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(qs, "id", 1L);

        QuestionSetFeedback qsf = QuestionSetFeedback.builder()
                .questionSet(qs)
                .questionSetComment("전반적으로 좋았습니다.")
                .build();
        ReflectionTestUtils.setField(qsf, "id", 1L);

        TimestampFeedback tsf = TimestampFeedback.builder()
                .startMs(0L)
                .endMs(5000L)
                .isAnalyzed(true)
                .build();
        ReflectionTestUtils.setField(tsf, "id", bookmarkId * 10);
        ReflectionTestUtils.setField(tsf, "questionSetFeedback", qsf);

        User user = org.mockito.Mockito.mock(User.class);
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(tsf)
                .build();
        ReflectionTestUtils.setField(bookmark, "id", bookmarkId);
        ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.now());

        return bookmark;
    }
}
