package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.question.dto.AdminQuestionPoolSearchCondition;
import com.rehearse.api.domain.question.dto.CreateQuestionPoolRequest;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.repository.QuestionPoolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminQuestionPoolServiceTest {

    @InjectMocks
    private AdminQuestionPoolService adminQuestionPoolService;

    @Mock
    private QuestionPoolRepository questionPoolRepository;

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("검색 결과를 응답 DTO로 매핑하고 최신순 정렬을 적용한다")
        void mapsResponseAndSorts_when_searching() {
            QuestionPool questionPool = questionPool(1L);
            given(questionPoolRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of(questionPool), PageRequest.of(0, 20), 1));

            var result = adminQuestionPoolService.search(
                    new AdminQuestionPoolSearchCondition("junior", "운영", true, "스레드"),
                    PageRequest.of(0, 20));

            assertThat(result.getContent())
                    .hasSize(1)
                    .first()
                    .satisfies(item -> {
                        assertThat(item.id()).isEqualTo(1L);
                        assertThat(item.cacheKey()).isEqualTo("JUNIOR:CS_FUNDAMENTAL");
                        assertThat(item.category()).isEqualTo("운영체제");
                        assertThat(item.content()).isEqualTo("프로세스와 스레드의 차이는 무엇인가요?");
                        assertThat(item.isActive()).isTrue();
                    });

            ArgumentCaptor<PageRequest> pageableCaptor = ArgumentCaptor.forClass(PageRequest.class);
            then(questionPoolRepository).should().findAll(any(Specification.class), pageableCaptor.capture());
            Sort sort = pageableCaptor.getValue().getSort();
            assertThat(sort.getOrderFor("createdAt")).isNotNull();
            assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(sort.getOrderFor("id")).isNotNull();
            assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("요청 값으로 활성 질문 풀 row를 생성한다")
        void createsActiveQuestionPool_when_requestValid() {
            CreateQuestionPoolRequest request = new CreateQuestionPoolRequest(
                    "JUNIOR:CS_FUNDAMENTAL",
                    "프로세스와 스레드의 차이는 무엇인가요?",
                    "프로세스와 스레드의 차이는 무엇인가요?",
                    "운영체제",
                    "프로세스는 독립 주소 공간을 가집니다.");
            given(questionPoolRepository.save(any(QuestionPool.class))).willAnswer(invocation -> {
                QuestionPool saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 2L);
                ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 5, 16, 10, 30));
                return saved;
            });

            var response = adminQuestionPoolService.create(request);

            assertThat(response.id()).isEqualTo(2L);
            assertThat(response.cacheKey()).isEqualTo("JUNIOR:CS_FUNDAMENTAL");
            assertThat(response.content()).isEqualTo("프로세스와 스레드의 차이는 무엇인가요?");
            assertThat(response.ttsContent()).isEqualTo("프로세스와 스레드의 차이는 무엇인가요?");
            assertThat(response.category()).isEqualTo("운영체제");
            assertThat(response.bestAnswer()).isEqualTo("프로세스는 독립 주소 공간을 가집니다.");
            assertThat(response.isActive()).isTrue();
        }
    }

    private QuestionPool questionPool(Long id) {
        QuestionPool questionPool = QuestionPool.create(
                "JUNIOR:CS_FUNDAMENTAL",
                "프로세스와 스레드의 차이는 무엇인가요?",
                "프로세스와 스레드의 차이는 무엇인가요?",
                "운영체제",
                "프로세스는 독립 주소 공간을 가집니다.");
        ReflectionTestUtils.setField(questionPool, "id", id);
        ReflectionTestUtils.setField(questionPool, "createdAt", LocalDateTime.of(2026, 5, 16, 10, 30));
        return questionPool;
    }
}
