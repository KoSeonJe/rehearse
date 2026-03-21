package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.dto.FollowUpRequest;
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
import java.util.stream.Collectors;

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
        TechStack effectiveStack = req.techStack() != null
            ? req.techStack() : TechStack.getDefaultForPosition(req.position());
        ResolvedProfile profile = personaResolver.resolve(req.position(), effectiveStack);

        // [M2] 단일 플레이스홀더: profile.followUpDepth()가 이미 base+overlay merge 결과
        return template
            .replace("{MEDIUM_PERSONA}", profile.mediumPersona())
            .replace("{FOLLOWUP_DEPTH}", profile.followUpDepth());
    }

    public String buildUserPrompt(FollowUpGenerationRequest req) {
        TechStack effectiveStack = req.techStack() != null
            ? req.techStack() : TechStack.getDefaultForPosition(req.position());

        StringBuilder sb = new StringBuilder();
        sb.append("직무: ").append(positionKorean(req.position()))
          .append(" (").append(effectiveStack.getDisplayName()).append(") | ")
          .append("레벨: ").append(levelKorean(req.level())).append("\n\n");

        sb.append("질문: ").append(req.questionContent()).append("\n");
        sb.append("답변: ").append(req.answerText()).append("\n");
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

        sb.append("\n새 후속 질문을 생성하세요.");
        return sb.toString();
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
