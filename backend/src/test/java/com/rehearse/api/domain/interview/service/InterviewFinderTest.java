package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InterviewFinderTest {

    @InjectMocks
    private InterviewFinder interviewFinder;

    @Mock
    private InterviewRepository interviewRepository;

    @Nested
    @DisplayName("findById 메서드")
    class FindById {

        @Test
        @DisplayName("findById - 정상: 면접이 존재하면 Interview를 반환한다")
        void findById_exists_returnsInterview() {
            // given
            Interview interview = TestFixtures.createInterview();
            ReflectionTestUtils.setField(interview, "id", 1L);
            given(interviewRepository.findByIdWithElementCollections(1L)).willReturn(Optional.of(interview));

            // when
            Interview result = interviewFinder.findById(1L);

            // then
            assertThat(result).isEqualTo(interview);
        }

        @Test
        @DisplayName("findById - 예외: 면접이 존재하지 않으면 NOT_FOUND BusinessException이 발생한다")
        void findById_notExists_throwsBusinessException() {
            // given
            given(interviewRepository.findByIdWithElementCollections(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> interviewFinder.findById(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                    });
        }

        @Test
        @DisplayName("findById - ElementCollection 쿼리 메서드(findByIdWithElementCollections)를 사용한다")
        void findById_usesElementCollectionQuery() {
            // given
            Interview interview = TestFixtures.createInterview();
            ReflectionTestUtils.setField(interview, "id", 1L);
            given(interviewRepository.findByIdWithElementCollections(1L)).willReturn(Optional.of(interview));

            // when
            interviewFinder.findById(1L);

            // then: 기본 findById가 아닌 ElementCollection 전용 메서드 호출을 검증
            then(interviewRepository).should().findByIdWithElementCollections(1L);
            then(interviewRepository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("findByPublicId 메서드")
    class FindByPublicId {

        @Test
        @DisplayName("findByPublicId - 정상: publicId로 면접을 조회하면 Interview를 반환한다")
        void findByPublicId_exists_returnsInterview() {
            // given
            Interview interview = TestFixtures.createInterview();
            String publicId = "test-public-uuid-1234";
            ReflectionTestUtils.setField(interview, "publicId", publicId);
            given(interviewRepository.findByPublicId(publicId)).willReturn(Optional.of(interview));

            // when
            Interview result = interviewFinder.findByPublicId(publicId);

            // then
            assertThat(result).isEqualTo(interview);
        }

        @Test
        @DisplayName("findByPublicId - 예외: publicId에 해당하는 면접이 없으면 NOT_FOUND BusinessException이 발생한다")
        void findByPublicId_notExists_throwsBusinessException() {
            // given
            String publicId = "non-existent-uuid";
            given(interviewRepository.findByPublicId(publicId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> interviewFinder.findByPublicId(publicId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                    });
        }
    }
}
