package com.rehearse.api.global.config;

import com.rehearse.api.domain.questionset.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.ConvertStatus;
import com.rehearse.api.domain.questionset.entity.QuestionAnswer;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.service.InternalQuestionSetService;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.common.ApiResponse;
import com.rehearse.api.global.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dev")
@Profile("local")
@RequiredArgsConstructor
public class DevController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final InternalQuestionSetService internalQuestionSetService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/login")
    public void devLoginBrowser(
            @RequestParam(defaultValue = "1") Long userId,
            HttpServletResponse response) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userId));

        String token = jwtTokenProvider.createToken(user.getId(), user.getRole().name());

        Cookie cookie = new Cookie("rehearse_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);

        log.info("[DEV] 브라우저 로그인: userId={}, email={}", user.getId(), user.getEmail());
        response.sendRedirect(frontendUrl + "/");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> devLoginApi(
            @RequestParam(defaultValue = "1") Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userId));

        String token = jwtTokenProvider.createToken(user.getId(), user.getRole().name());

        log.info("[DEV] API 토큰 발급: userId={}, email={}", user.getId(), user.getEmail());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "token", token,
                "userId", String.valueOf(user.getId()),
                "email", user.getEmail(),
                "name", user.getName()
        )));
    }

    @PostMapping("/simulate-analysis/{interviewId}/question-sets/{questionSetId}")
    public ResponseEntity<ApiResponse<String>> simulateAnalysis(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId,
            @RequestParam(defaultValue = "2000") long delayMs) {

        QuestionSet questionSet = internalQuestionSetService.getQuestionSet(questionSetId);
        AnalysisStatus currentStatus = questionSet.getEffectiveAnalysisStatus();

        if (currentStatus.isResolved()) {
            return ResponseEntity.ok(ApiResponse.ok("이미 분석 완료 상태입니다: " + currentStatus));
        }

        List<QuestionAnswer> answers = internalQuestionSetService.getAnswers(questionSetId);
        if (answers.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("답변이 없어 분석을 건너뜁니다."));
        }

        simulatePipeline(interviewId, questionSetId, answers, delayMs);

        log.info("[DEV] 분석 시뮬레이션 완료: interviewId={}, questionSetId={}", interviewId, questionSetId);
        return ResponseEntity.ok(ApiResponse.ok("분석 시뮬레이션 완료"));
    }

    @PostMapping("/simulate-full/{interviewId}")
    public ResponseEntity<ApiResponse<String>> simulateFullInterview(
            @PathVariable Long interviewId) {

        var questionSets = findQuestionSetsByInterview(interviewId);
        int completed = 0;

        for (var entry : questionSets) {
            Long qsId = entry.getId();
            AnalysisStatus status = entry.getEffectiveAnalysisStatus();

            if (status.isResolved()) {
                log.info("[DEV] 질문세트 {} 이미 완료, 건너뜀", qsId);
                continue;
            }

            List<QuestionAnswer> answers = internalQuestionSetService.getAnswers(qsId);
            if (answers.isEmpty()) {
                log.info("[DEV] 질문세트 {} 답변 없음, 건너뜀", qsId);
                continue;
            }

            simulatePipeline(interviewId, qsId, answers, 0);
            completed++;
        }

        String message = String.format("면접 %d의 질문세트 %d개 분석 시뮬레이션 완료", interviewId, completed);
        log.info("[DEV] {}", message);
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    private void simulatePipeline(Long interviewId, Long questionSetId,
                                  List<QuestionAnswer> answers, long delayMs) {
        transitProgress(questionSetId, AnalysisStatus.EXTRACTING, delayMs);
        transitProgress(questionSetId, AnalysisStatus.ANALYZING, delayMs);
        transitProgress(questionSetId, AnalysisStatus.FINALIZING, delayMs);

        SaveFeedbackRequest feedbackRequest = buildMockFeedback(answers);
        internalQuestionSetService.saveFeedback(questionSetId, feedbackRequest);

        internalQuestionSetService.updateConvertStatus(questionSetId,
                buildConvertCompleted(interviewId, questionSetId));
    }

    private void transitProgress(Long questionSetId, AnalysisStatus status, long delayMs) {
        if (delayMs > 0) {
            try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        var request = objectMapper.convertValue(
                Map.of("status", status.name()),
                com.rehearse.api.domain.questionset.dto.UpdateProgressRequest.class
        );
        internalQuestionSetService.updateProgress(questionSetId, request);
    }

    private com.rehearse.api.domain.questionset.dto.UpdateConvertStatusRequest buildConvertCompleted(
            Long interviewId, Long questionSetId) {
        return objectMapper.convertValue(
                Map.of(
                        "status", ConvertStatus.COMPLETED.name(),
                        "streamingS3Key", String.format("videos/%d/qs_%d.mp4", interviewId, questionSetId)
                ),
                com.rehearse.api.domain.questionset.dto.UpdateConvertStatusRequest.class
        );
    }

    private SaveFeedbackRequest buildMockFeedback(List<QuestionAnswer> answers) {
        List<Map<String, Object>> timestampFeedbacks = new ArrayList<>();

        for (QuestionAnswer answer : answers) {
            Map<String, Object> commentBlock = Map.of(
                    "positive", "명확하고 구조적인 답변입니다.",
                    "negative", "구체적인 사례가 부족합니다.",
                    "suggestion", "실무 경험을 추가하면 더 좋은 답변이 됩니다."
            );

            timestampFeedbacks.add(Map.ofEntries(
                    Map.entry("questionId", answer.getQuestion().getId()),
                    Map.entry("startMs", answer.getStartMs()),
                    Map.entry("endMs", answer.getEndMs()),
                    Map.entry("transcript", "[DEV] 시뮬레이션된 음성 인식 결과입니다. 실제 Lambda에서는 Whisper STT가 처리합니다."),
                    Map.entry("verbalComment", commentBlock),
                    Map.entry("nonverbalComment", commentBlock),
                    Map.entry("overallComment", commentBlock),
                    Map.entry("vocalComment", commentBlock),
                    Map.entry("fillerWordCount", 2),
                    Map.entry("fillerWords", List.of("음", "어")),
                    Map.entry("speechPace", "적절"),
                    Map.entry("toneConfidenceLevel", "GOOD"),
                    Map.entry("emotionLabel", "자신감"),
                    Map.entry("eyeContactLevel", "GOOD"),
                    Map.entry("postureLevel", "GOOD"),
                    Map.entry("expressionLabel", "CONFIDENT"),
                    Map.entry("accuracyIssues", "[]"),
                    Map.entry("coachingStructure", "개념→원리→적용 순서로 구조화했습니다."),
                    Map.entry("coachingImprovement", "실무 경험을 추가하면 더 깊이 있는 답변이 됩니다.")
            ));
        }

        return objectMapper.convertValue(
                Map.of(
                        "questionSetComment", "[DEV] 시뮬레이션된 분석 결과입니다.",
                        "timestampFeedbacks", timestampFeedbacks,
                        "isVerbalCompleted", true,
                        "isNonverbalCompleted", true
                ),
                SaveFeedbackRequest.class
        );
    }

    private List<QuestionSet> findQuestionSetsByInterview(Long interviewId) {
        return internalQuestionSetService.getQuestionSetsByInterview(interviewId);
    }
}
