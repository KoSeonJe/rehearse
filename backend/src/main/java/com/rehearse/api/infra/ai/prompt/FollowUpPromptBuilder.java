package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.persona.PersonaResolver;
import com.rehearse.api.infra.ai.persona.ResolvedProfile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpPromptBuilder {

    private final PersonaResolver personaResolver;
    private String template;

    @PostConstruct
    void init() {
        try (var stream = getClass().getResourceAsStream("/prompts/template/follow-up.txt")) {
            if (stream == null) {
                throw new IllegalStateException("follow-up.txt 템플릿 파일을 찾을 수 없습니다.");
            }
            template = new String(stream.readAllBytes());
            log.info("후속 질문 프롬프트 템플릿 로드 완료");
        } catch (IOException e) {
            throw new IllegalStateException("follow-up.txt 템플릿 로드 실패", e);
        }
    }

    public String buildSystemPrompt(FollowUpGenerationRequest req) {
        TechStack effectiveStack = resolveEffectiveStack(req.position(), req.techStack());
        ResolvedProfile profile = personaResolver.resolve(req.position(), effectiveStack);

        return template
            .replace("{MEDIUM_PERSONA}", profile.mediumPersona())
            .replace("{FOLLOWUP_DEPTH}", profile.followUpDepth());
    }

    public String buildUserPromptForAudio(FollowUpGenerationRequest req) {
        return buildUserPromptInternal(req,
                "[첨부된 오디오를 전사하여 사용하세요]",
                "오디오를 전사한 뒤, 그 내용을 바탕으로 후속 질문을 생성하세요. answerText 필드에 전사 결과를 포함하세요.");
    }

    public String buildUserPrompt(FollowUpGenerationRequest req) {
        return buildUserPromptInternal(req, req.answerText(), "새 후속 질문을 생성하세요.");
    }

    private String buildUserPromptInternal(FollowUpGenerationRequest req, String answerSection, String instruction) {
        TechStack effectiveStack = resolveEffectiveStack(req.position(), req.techStack());

        StringBuilder sb = new StringBuilder();
        sb.append("직무: ").append(positionKorean(req.position()))
          .append(" (").append(effectiveStack.getDisplayName()).append(") | ")
          .append("레벨: ").append(levelKorean(req.level())).append("\n\n");

        sb.append("질문: ").append(req.questionContent()).append("\n");
        sb.append("답변: ").append(answerSection).append("\n");
        sb.append("비언어: ").append(
            req.nonVerbalSummary() != null ? req.nonVerbalSummary() : "없음").append("\n");

        if (req.previousExchanges() != null && !req.previousExchanges().isEmpty()) {
            sb.append("\n이전 후속:\n");
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
