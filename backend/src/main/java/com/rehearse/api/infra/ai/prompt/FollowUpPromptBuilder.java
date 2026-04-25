package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Claim;
import com.rehearse.api.domain.interview.Perspective;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.persona.PersonaResolver;
import com.rehearse.api.infra.ai.persona.ResolvedProfile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpPromptBuilder {

    private static final String CONCEPT_TEMPLATE_PATH = "/prompts/template/follow-up-concept.txt";
    private static final String EXPERIENCE_TEMPLATE_PATH = "/prompts/template/follow-up-experience.txt";

    private final PersonaResolver personaResolver;
    private String conceptTemplate;
    private String experienceTemplate;

    @PostConstruct
    void init() {
        conceptTemplate = loadTemplate(CONCEPT_TEMPLATE_PATH);
        experienceTemplate = loadTemplate(EXPERIENCE_TEMPLATE_PATH);
        log.info("후속 질문 프롬프트 템플릿 로드 완료 (concept + experience)");
    }

    private String loadTemplate(String path) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(path + " 템플릿 파일을 찾을 수 없습니다.");
            }
            return new String(stream.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException(path + " 템플릿 로드 실패", e);
        }
    }

    public String buildSystemPrompt(FollowUpGenerationRequest req) {
        TechStack effectiveStack = resolveEffectiveStack(req.position(), req.techStack());
        ResolvedProfile profile = personaResolver.resolve(req.position(), effectiveStack);
        String template = resolveTemplate(req.mainReferenceType());

        return template
            .replace("{MEDIUM_PERSONA}", profile.mediumPersona())
            .replace("{FOLLOWUP_DEPTH}", profile.followUpDepth());
    }

    /**
     * 메인 질문의 referenceType에 따라 후속질문 템플릿을 분기한다.
     * - GUIDE       → 이력서·경험 기반 질문 → EXPERIENCE 모드
     * - MODEL_ANSWER or null → CS 개념 설명형 또는 안전 기본값 → CONCEPT 모드
     * 기본값을 CONCEPT로 잡는 이유: 경험 전제 프레이밍이 잘못 나가는 어색함이
     * 개념 질문 누락보다 사용자에게 더 부정적이기 때문.
     */
    private String resolveTemplate(ReferenceType mainReferenceType) {
        if (mainReferenceType == null) {
            return conceptTemplate;
        }
        return switch (mainReferenceType) {
            case GUIDE -> experienceTemplate;
            case MODEL_ANSWER -> conceptTemplate;
        };
    }

    public String buildUserPromptForAudio(FollowUpGenerationRequest req) {
        return buildUserPromptInternal(req,
                "[첨부된 오디오를 전사하여 사용하세요]",
                "오디오를 전사한 뒤, 그 내용을 바탕으로 후속 질문을 생성하세요. answerText 필드에 전사 결과를 포함하세요.");
    }

    public String buildUserPrompt(FollowUpGenerationRequest req) {
        return buildUserPromptInternal(req, req.answerText(), "새 후속 질문을 생성하세요.");
    }

    /**
     * Step A 분석 결과를 포함한 user prompt 생성 (v3 Step B 전용).
     * target_claim_idx 선정과 perspective 선정에 사용할 구조화 입력을 명시 직렬화한다.
     */
    public String buildUserPromptWithAnalysis(
            FollowUpGenerationRequest req,
            AnswerAnalysis analysis,
            List<Perspective> askedPerspectives
    ) {
        StringBuilder sb = new StringBuilder(buildUserPromptInternal(req, req.answerText(),
                "아래 ANSWER_ANALYSIS 를 바탕으로 새 후속 질문을 생성하세요."));
        sb.append("\n\nANSWER_ANALYSIS:\n");
        sb.append(formatClaims(analysis.claims()));
        sb.append("- missing_perspectives: ").append(formatPerspectives(analysis.missingPerspectives())).append("\n");
        sb.append("- unstated_assumptions: ").append(formatStrings(analysis.unstatedAssumptions())).append("\n");
        sb.append("- recommended_next_action: ").append(analysis.recommendedNextAction().name()).append("\n");
        sb.append("- asked_perspectives: ").append(formatPerspectives(askedPerspectives)).append("\n");
        return sb.toString();
    }

    private static String formatClaims(List<Claim> claims) {
        if (claims == null || claims.isEmpty()) {
            return "- claims: (없음)\n";
        }
        StringBuilder sb = new StringBuilder("- claims:\n");
        for (int i = 0; i < claims.size(); i++) {
            Claim c = claims.get(i);
            sb.append("  [").append(i).append("] text=\"").append(c.text())
              .append("\" depth_score=").append(c.depthScore())
              .append(" evidence_strength=").append(c.evidenceStrength().name())
              .append(" topic_tag=").append(c.topicTag() == null ? "(없음)" : c.topicTag())
              .append("\n");
        }
        return sb.toString();
    }

    private static String formatPerspectives(List<Perspective> perspectives) {
        if (perspectives == null || perspectives.isEmpty()) {
            return "(없음)";
        }
        return perspectives.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private static String formatStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "(없음)";
        }
        return values.stream().collect(Collectors.joining(" | "));
    }

    private String buildUserPromptInternal(FollowUpGenerationRequest req, String answerSection, String instruction) {
        TechStack effectiveStack = resolveEffectiveStack(req.position(), req.techStack());

        StringBuilder sb = new StringBuilder();
        sb.append("직무: ").append(positionKorean(req.position()))
          .append(" (").append(effectiveStack.getDisplayName()).append(") | ")
          .append("레벨: ").append(levelKorean(req.level())).append("\n\n");

        sb.append("메인 질문: ").append(req.questionContent()).append("\n");
        sb.append("현재 답변: ").append(answerSection).append("\n");
        sb.append("비언어: ").append(
            req.nonVerbalSummary() != null ? req.nonVerbalSummary() : "없음").append("\n");

        if (req.previousExchanges() != null && !req.previousExchanges().isEmpty()) {
            sb.append("\n이전 후속 대화 (참고용, 중복 회피 목적 — 인용 대상 아님):\n");
            for (int i = 0; i < req.previousExchanges().size(); i++) {
                var ex = req.previousExchanges().get(i);
                sb.append("[").append(i + 1).append("] Q: ").append(ex.getQuestion()).append("\n");
                sb.append("[").append(i + 1).append("] A: ").append(ex.getAnswer()).append("\n");
            }
        }

        sb.append("\n").append(instruction);
        return sb.toString();
    }

    private static TechStack resolveEffectiveStack(Position position, TechStack techStack) {
        return techStack != null ? techStack : TechStack.getDefaultForPosition(position);
    }

    private static String positionKorean(Position p) {
        return switch (p) {
            case BACKEND -> "백엔드";
            case FRONTEND -> "프론트엔드";
            case DEVOPS -> "데브옵스";
            case DATA_ENGINEER -> "데이터 엔지니어";
            case FULLSTACK -> "풀스택";
        };
    }

    private static String levelKorean(InterviewLevel l) {
        return switch (l) {
            case JUNIOR -> "주니어";
            case MID -> "미드";
            case SENIOR -> "시니어";
        };
    }
}
