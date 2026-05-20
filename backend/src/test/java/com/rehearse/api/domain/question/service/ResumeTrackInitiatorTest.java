package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.QuestionGenerationStatus;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionDepthType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.models.service.ResumeQuestionGenerator;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("ResumeTrackInitiator — PDF 직접 입력 기반 opener + main 일괄 LLM 생성 + DB 분리 적재")
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

    @Test
    @DisplayName("opener 1 + main 3 응답 → QSet 4개 분리 적재 + orderIndex 0..3 + Interview 상태 COMPLETED")
    void initiate_persistsOpenerAndMains_andCompletesInterview() {
        Long interviewId = persistInterview();
        stubGenerator(generated(1, 3));

        initiator.initiate(interviewId, "hash-success", RESUME_PDF, 30, Position.BACKEND, TechStack.JAVA_SPRING);

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
    @DisplayName("ResumeQuestionGenerator.generate 에 PDF 바이트 + position + techStack 그대로 전달된다")
    void initiate_passes_pdfBytes_and_persona_to_generator() {
        Long interviewId = persistInterview();
        stubGenerator(generated(1, 1));

        initiator.initiate(interviewId, "hash-pass", RESUME_PDF, 21, Position.BACKEND, TechStack.JAVA_SPRING);

        org.mockito.Mockito.verify(resumeQuestionGenerator)
                .generate(eq(RESUME_PDF), anyInt(), anyInt(), eq(Position.BACKEND), eq(TechStack.JAVA_SPRING));
    }

    @Test
    @DisplayName("LLM 응답 depth_type 값이 main Question.depthType 컬럼에 적재되고 opener 는 NULL 로 유지된다")
    void initiate_persistsDepthType_forMains_andNullForOpener() {
        Long interviewId = persistInterview();
        stubGenerator(generatedWithDepthTypes(1,
                List.of("TRADEOFF", "LIMITATION", "QUANTITATIVE", "ALTERNATIVE", "PRINCIPLE")));

        initiator.initiate(interviewId, "hash-depth", RESUME_PDF, 30,
                Position.BACKEND, TechStack.JAVA_SPRING);

        List<QuestionSet> persistedSets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
        assertThat(persistedSets).hasSize(6);

        Question openerQuestion = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(persistedSets.get(0).getId()).get(0);
        assertThat(openerQuestion.getQuestionType()).isEqualTo(QuestionType.RESUME_OPENER);
        assertThat(openerQuestion.getDepthType()).isNull();

        List<QuestionDepthType> mainDepthTypes = new ArrayList<>();
        for (int i = 1; i < persistedSets.size(); i++) {
            Question main = questionRepository
                    .findByQuestionSetIdOrderByOrderIndex(persistedSets.get(i).getId()).get(0);
            mainDepthTypes.add(main.getDepthType());
        }
        assertThat(mainDepthTypes).containsExactly(
                QuestionDepthType.TRADEOFF,
                QuestionDepthType.LIMITATION,
                QuestionDepthType.QUANTITATIVE,
                QuestionDepthType.ALTERNATIVE,
                QuestionDepthType.PRINCIPLE);
    }

    @Test
    @DisplayName("mains 중 depth_type 누락 시 BusinessException(RESPONSE_INVALID) + Interview 상태 FAILED")
    void initiate_throwsAndMarksFailed_whenMainsDepthTypeMissing() {
        Long interviewId = persistInterview();
        stubGenerator(generatedWithPartialDepthTypes(1,
                Arrays.asList("TRADEOFF", null, "QUANTITATIVE")));

        Assertions.assertThatThrownBy(() ->
                        initiator.initiate(interviewId, "hash-depth-missing", RESUME_PDF, 30,
                                Position.BACKEND, TechStack.JAVA_SPRING))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.RESPONSE_INVALID);

        Interview reloaded = interviewRepository.findById(interviewId).orElseThrow();
        assertThat(reloaded.getQuestionGenerationStatus())
                .isEqualTo(QuestionGenerationStatus.FAILED);
        assertThat(questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId)).isEmpty();
    }

    @Test
    @DisplayName("Generator 실패 시 Interview 상태 FAILED 로 전이 + 예외 재전파")
    void initiate_marksInterviewFailed_andRethrows_whenLlmFails() {
        Long interviewId = persistInterview();
        when(resumeQuestionGenerator.generate(any(), anyInt(), anyInt(), any(), any()))
                .thenThrow(new RuntimeException("LLM 호출 실패"));

        Assertions.assertThatThrownBy(() ->
                        initiator.initiate(interviewId, "hash-fail", RESUME_PDF, 30,
                                Position.BACKEND, TechStack.JAVA_SPRING))
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

    private void stubGenerator(GeneratedResumeQuestions response) {
        when(resumeQuestionGenerator.generate(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(response);
    }

    private GeneratedResumeQuestions generated(int openerCount, int mainCount) {
        List<GeneratedResumeQuestions.GeneratedResumeQuestion> openers = new ArrayList<>();
        for (int i = 0; i < openerCount; i++) {
            openers.add(new GeneratedResumeQuestions.GeneratedResumeQuestion(
                    "메인 프로젝트 설명" + i, "TTS-O" + i, "best-O" + i, null));
        }
        List<GeneratedResumeQuestions.GeneratedResumeQuestion> mains = new ArrayList<>();
        for (int i = 0; i < mainCount; i++) {
            mains.add(new GeneratedResumeQuestions.GeneratedResumeQuestion(
                    "주요 의사결정 " + i, "TTS-M" + i, "best-M" + i, QuestionDepthType.TRADEOFF));
        }
        return new GeneratedResumeQuestions(openers, mains);
    }

    private GeneratedResumeQuestions generatedWithDepthTypes(int openerCount, List<String> mainDepthTypes) {
        List<GeneratedResumeQuestions.GeneratedResumeQuestion> openers = new ArrayList<>();
        for (int i = 0; i < openerCount; i++) {
            openers.add(new GeneratedResumeQuestions.GeneratedResumeQuestion(
                    "메인 프로젝트 설명" + i, "TTS-O" + i, "best-O" + i, null));
        }
        List<GeneratedResumeQuestions.GeneratedResumeQuestion> mains = new ArrayList<>();
        for (int i = 0; i < mainDepthTypes.size(); i++) {
            mains.add(new GeneratedResumeQuestions.GeneratedResumeQuestion(
                    "주요 의사결정 " + i, "TTS-M" + i, "best-M" + i,
                    QuestionDepthType.valueOf(mainDepthTypes.get(i))));
        }
        return new GeneratedResumeQuestions(openers, mains);
    }

    private GeneratedResumeQuestions generatedWithPartialDepthTypes(int openerCount, List<String> mainDepthTypes) {
        List<GeneratedResumeQuestions.GeneratedResumeQuestion> openers = new ArrayList<>();
        for (int i = 0; i < openerCount; i++) {
            openers.add(new GeneratedResumeQuestions.GeneratedResumeQuestion(
                    "메인 프로젝트 설명" + i, "TTS-O" + i, "best-O" + i, null));
        }
        List<GeneratedResumeQuestions.GeneratedResumeQuestion> mains = new ArrayList<>();
        for (int i = 0; i < mainDepthTypes.size(); i++) {
            String dt = mainDepthTypes.get(i);
            mains.add(new GeneratedResumeQuestions.GeneratedResumeQuestion(
                    "주요 의사결정 " + i, "TTS-M" + i, "best-M" + i,
                    dt == null ? null : QuestionDepthType.valueOf(dt)));
        }
        return new GeneratedResumeQuestions(openers, mains);
    }
}
