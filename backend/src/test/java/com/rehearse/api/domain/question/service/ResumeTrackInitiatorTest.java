package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.QuestionGenerationStatus;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.models.service.ResumeQuestionGenerator;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.models.service.ResumeSkeletonExtractor;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions.GeneratedResumeQuestion;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("ResumeTrackInitiator — 이력서 트랙 면접 시작 (opener + main 일괄 LLM 생성 + DB 분리 적재)")
class ResumeTrackInitiatorTest extends ServiceIntegrationSupport {

    private static final byte[] RESUME_PDF = "pdf-content".getBytes();

    @Autowired
    private ResumeTrackInitiator initiator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private QuestionSetRepository questionSetRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @MockitoBean
    private ResumeQuestionGenerator resumeQuestionGenerator;

    @MockitoBean
    private ResumeSkeletonExtractor resumeSkeletonExtractor;

    @Test
    @DisplayName("opener 1 + main 3 응답 → QSet 4개 분리 적재 + orderIndex 0..3 + Interview 상태 COMPLETED")
    void initiate_persistsOpenerAndMains_andCompletesInterview() {
        Long interviewId = persistInterview();
        stubExtractor(skeletonWithProjects("hash-success"), "hash-success");
        stubGenerator(1, 3);

        initiator.initiate(interviewId, "hash-success", RESUME_PDF, 30);

        List<QuestionSet> persistedSets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
        assertThat(persistedSets).hasSize(4);
        assertThat(persistedSets).extracting(QuestionSet::getCategory)
                .containsOnly(InterviewType.RESUME_BASED);
        assertThat(persistedSets).extracting(QuestionSet::getOrderIndex)
                .containsExactly(0, 1, 2, 3);

        List<Question> openerQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(persistedSets.get(0).getId());
        assertThat(openerQuestions).hasSize(1);
        assertThat(openerQuestions.get(0).getQuestionType()).isEqualTo(QuestionType.RESUME_OPENER);

        for (int i = 1; i < persistedSets.size(); i++) {
            List<Question> mains = questionRepository
                    .findByQuestionSetIdOrderByOrderIndex(persistedSets.get(i).getId());
            assertThat(mains).hasSize(1);
            assertThat(mains.get(0).getQuestionType()).isEqualTo(QuestionType.RESUME_MAIN);
        }

        Interview reloaded = interviewRepository.findById(interviewId).orElseThrow();
        assertThat(reloaded.getQuestionGenerationStatus())
                .isEqualTo(QuestionGenerationStatus.COMPLETED);
    }

    @Test
    @DisplayName("LLM 실패 시 Interview 상태 FAILED 로 전이 + 예외 재전파")
    void initiate_marksInterviewFailed_andRethrows_whenLlmFails() {
        Long interviewId = persistInterview();
        stubExtractor(skeletonWithProjects("hash-fail"), "hash-fail");
        when(resumeQuestionGenerator.generate(any(ResumeSkeleton.class), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("LLM 호출 실패"));

        Assertions.assertThatThrownBy(() ->
                        initiator.initiate(interviewId, "hash-fail", RESUME_PDF, 30))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LLM 호출 실패");

        Interview reloaded = interviewRepository.findById(interviewId).orElseThrow();
        assertThat(reloaded.getQuestionGenerationStatus())
                .isEqualTo(QuestionGenerationStatus.FAILED);
        assertThat(questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId)).isEmpty();
    }

    private Long persistInterview() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("resume-initiator@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-resume-initiator")
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);
        return interview.getId();
    }

    private void stubExtractor(GeneratedResumeSkeleton skeleton, String fileHash) {
        when(resumeSkeletonExtractor.extract(any(byte[].class), eq(fileHash))).thenReturn(skeleton);
    }

    private void stubGenerator(int openerCount, int mainCount) {
        List<GeneratedResumeQuestion> openers = new ArrayList<>();
        for (int i = 0; i < openerCount; i++) {
            openers.add(new GeneratedResumeQuestion("opener-" + i, "tts-O" + i, "best-O" + i));
        }
        List<GeneratedResumeQuestion> mains = new ArrayList<>();
        for (int i = 0; i < mainCount; i++) {
            mains.add(new GeneratedResumeQuestion("main-" + i, "tts-M" + i, "best-M" + i));
        }
        when(resumeQuestionGenerator.generate(any(ResumeSkeleton.class), anyInt(), anyInt()))
                .thenReturn(new GeneratedResumeQuestions(openers, mains));
    }

    private GeneratedResumeSkeleton skeletonWithProjects(String resumeId) {
        return new GeneratedResumeSkeleton(
                resumeId,
                "MID",
                "backend",
                List.of(
                        new GeneratedResumeSkeleton.GeneratedProject(
                                "p1", "주문 캐싱 개선",
                                List.of("Redis", "MySQL"),
                                "백엔드", "Cache-Aside",
                                List.of("TTL 5분"))
                )
        );
    }
}
