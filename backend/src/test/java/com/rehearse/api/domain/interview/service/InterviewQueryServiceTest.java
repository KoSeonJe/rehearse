package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewQueryService - 면접 세션 조회")
class InterviewQueryServiceTest {

    @InjectMocks
    private InterviewQueryService interviewQueryService;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Nested
    @DisplayName("getInterview 메서드")
    class GetInterview {

        @Test
        @DisplayName("존재하지 않는 면접 세션 조회 시 BusinessException이 발생한다")
        void getInterview_notFound() {
            // given
            given(interviewFinder.findByIdAndValidateOwner(999L, 1L))
                    .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

            // when & then
            assertThatThrownBy(() -> interviewQueryService.getInterview(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                    });
        }

        @Test
        @DisplayName("면접 세션 조회 성공")
        void getInterview_success() {
            // given
            Interview interview = createMockInterview();
            given(interviewFinder.findByIdAndValidateOwner(1L, 1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of());

            // when
            InterviewResponse response = interviewQueryService.getInterview(1L, 1L);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getPosition()).isEqualTo(Position.BACKEND);
            assertThat(response.getStatus()).isEqualTo(InterviewStatus.READY);
            assertThat(response.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("getInterviewByPublicId 메서드")
    class GetInterviewByPublicId {

        @Test
        @DisplayName("본인 publicId 조회 성공")
        void getInterviewByPublicId_owner_success() {
            Interview interview = createMockInterview();
            String publicId = "test-public-uuid";
            ReflectionTestUtils.setField(interview, "publicId", publicId);
            given(interviewFinder.findByPublicIdAndValidateOwner(publicId, 1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of());

            InterviewResponse response = interviewQueryService.getInterviewByPublicId(publicId, 1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getInterviewTypes()).containsExactly(InterviewType.CS_FUNDAMENTAL);
        }

        @Test
        @DisplayName("타 유저 publicId 조회 시 FORBIDDEN 예외")
        void getInterviewByPublicId_otherUser_forbidden() {
            String publicId = "test-public-uuid";
            given(interviewFinder.findByPublicIdAndValidateOwner(publicId, 2L))
                    .willThrow(new BusinessException(HttpStatus.FORBIDDEN, "INTERVIEW_008", "면접 세션 접근 권한이 없습니다."));

            assertThatThrownBy(() -> interviewQueryService.getInterviewByPublicId(publicId, 2L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_008");
                    });
        }
    }

    private Interview createMockInterview() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
        return interview;
    }
}
