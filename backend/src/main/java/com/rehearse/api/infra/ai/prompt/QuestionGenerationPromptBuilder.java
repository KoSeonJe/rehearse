package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.persona.PersonaResolver;
import com.rehearse.api.infra.ai.persona.ResolvedProfile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionGenerationPromptBuilder {

    private final PersonaResolver personaResolver;
    private String template;

    @PostConstruct
    void init() {
        try (var stream = getClass().getResourceAsStream("/prompts/template/question-generation.txt")) {
            if (stream == null) {
                throw new IllegalStateException("question-generation.txt 템플릿 파일을 찾을 수 없습니다.");
            }
            template = new String(stream.readAllBytes());
            log.info("질문 생성 프롬프트 템플릿 로드 완료");
        } catch (IOException e) {
            throw new IllegalStateException("question-generation.txt 템플릿 로드 실패", e);
        }
    }

    public String buildSystemPrompt(QuestionGenerationRequest req) {
        TechStack effectiveStack = resolveEffectiveStack(req.position(), req.techStack());
        ResolvedProfile profile = personaResolver.resolve(req.position(), effectiveStack);

        String typeGuide = profile.interviewTypeGuideMap().entrySet().stream()
            .filter(e -> req.interviewTypes().stream().anyMatch(t -> t.name().equals(e.getKey())))
            .map(e -> "- " + e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining("\n"));

        String csBlock = "";
        if (req.interviewTypes().contains(InterviewType.CS_FUNDAMENTAL)) {
            if (req.csSubTopics() != null && !req.csSubTopics().isEmpty()) {
                csBlock = "## CS 세부 주제\n" + String.join(", ", req.csSubTopics()) + "에서만 출제.";
            } else {
                csBlock = "## CS 세부 주제\n자료구조, 운영체제, 네트워크, 데이터베이스에서만 출제. 그 외 주제(함수형 프로그래밍, 디자인 패턴 등)는 제외.";
            }
        }

        String levelGuide = LevelGuideProvider.get(req.level());

        String resumeBlock = (req.resumeText() != null && !req.resumeText().isBlank())
            ? "## 이력서 활용\nRESUME_BASED 질문은 이력서의 프로젝트, 기술, 성과를 구체적으로 언급하여 생성하되, 성능 수치 외에도 의사결정 과정·장애 대응·팀 협업·테스트 전략·유지보수성 등 다양한 관점에서 질문하세요."
            : "";

        return template
            .replace("{FULL_PERSONA}", profile.fullPersona())
            .replace("{BASE_EVALUATION_PERSPECTIVE}", profile.evaluationPerspective())
            .replace("{FILTERED_INTERVIEW_TYPE_GUIDE}", typeGuide)
            .replace("{CONDITIONAL_CS_SUBTOPIC_BLOCK}", csBlock)
            .replace("{SINGLE_LEVEL_GUIDE}", levelGuide)
            .replace("{CONDITIONAL_RESUME_BLOCK}", resumeBlock);
    }

    public String buildUserPrompt(QuestionGenerationRequest req) {
        TechStack effectiveStack = resolveEffectiveStack(req.position(), req.techStack());
        int questionCount = QuestionCountCalculator.calculate(
            req.durationMinutes(), req.interviewTypes().size());

        StringBuilder sb = new StringBuilder();
        sb.append("직무: ").append(positionKorean(req.position()))
          .append(" (").append(effectiveStack.getDisplayName()).append(")\n");
        sb.append("레벨: ").append(levelKorean(req.level())).append("\n");
        sb.append("유형: ").append(typesKorean(req.interviewTypes())).append("\n");
        sb.append("질문 수: ").append(questionCount).append("개\n");

        if (req.interviewTypes().contains(InterviewType.CS_FUNDAMENTAL)) {
            if (req.csSubTopics() != null && !req.csSubTopics().isEmpty()) {
                sb.append("CS 세부: ").append(String.join(", ", req.csSubTopics())).append("\n");
            } else {
                sb.append("CS 세부: 자료구조, 운영체제, 네트워크, 데이터베이스\n");
            }
        }
        if (req.resumeText() != null && !req.resumeText().isBlank()) {
            sb.append("이력서:\n").append(req.resumeText()).append("\n");
        }

        sb.append("세션: ").append(UUID.randomUUID()).append("\n");
        sb.append("중복 없는 새 관점의 질문을 생성하세요.");
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

    private static String typesKorean(Set<InterviewType> types) {
        return types.stream()
            .map(t -> switch (t) {
                case CS_FUNDAMENTAL -> "CS 기초";
                case BEHAVIORAL -> "Behavioral";
                case RESUME_BASED -> "이력서 기반";
                case LANGUAGE_FRAMEWORK -> "언어/프레임워크";
                case SYSTEM_DESIGN -> "시스템 설계";
                case FULLSTACK_STACK -> "풀스택 기술";
                case UI_FRAMEWORK -> "UI 프레임워크";
                case BROWSER_PERFORMANCE -> "브라우저 성능";
                case INFRA_CICD -> "인프라/CI-CD";
                case CLOUD -> "클라우드";
                case DATA_PIPELINE -> "데이터 파이프라인";
                case SQL_MODELING -> "SQL/모델링";
            })
            .collect(Collectors.joining(", "));
    }
}
