package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.resume.domain.InterrogationChain;
import com.rehearse.api.domain.resume.domain.InterrogationChain.ChainStep;
import com.rehearse.api.domain.resume.domain.InterrogationChain.StepType;
import com.rehearse.api.domain.resume.domain.Project;
import com.rehearse.api.domain.resume.domain.ResumeClaim;
import com.rehearse.api.domain.resume.domain.ResumeClaim.ClaimType;
import com.rehearse.api.domain.resume.domain.ResumeClaim.Priority;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton.CandidateLevel;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.layer.SkeletonCallType;
import com.rehearse.api.infra.ai.dto.CachePolicy;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ExtractedResumeSkeleton;
import com.rehearse.api.infra.ai.dto.ExtractedResumeSkeleton.ExtractedChainStep;
import com.rehearse.api.infra.ai.dto.ExtractedResumeSkeleton.ExtractedClaim;
import com.rehearse.api.infra.ai.dto.ExtractedResumeSkeleton.ExtractedImplicitCsTopic;
import com.rehearse.api.infra.ai.dto.ExtractedResumeSkeleton.ExtractedProject;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.prompt.ResumeExtractorPromptBuilder;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExtractionService {

    private static final double MIN_CONFIDENCE_THRESHOLD = 0.3;
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 4096;

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final ResumeExtractorPromptBuilder promptBuilder;

    public ResumeSkeleton extract(String normalizedResumeText, String fileHash) {
        ChatRequest request = buildChatRequest(normalizedResumeText);
        ChatResponse response = aiClient.chat(request);

        ExtractedResumeSkeleton raw = aiResponseParser.parseOrRetry(
                response, ExtractedResumeSkeleton.class, aiClient, request);

        ResumeSkeleton skeleton = toDomain(raw, fileHash);
        log.info("이력서 추출 완료: resumeId={}, projects={}, level={}",
                skeleton.resumeId(), skeleton.projects().size(), skeleton.candidateLevel());
        return skeleton;
    }

    private ChatRequest buildChatRequest(String normalizedResumeText) {
        return ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.ofCached(ChatMessage.Role.SYSTEM, promptBuilder.buildSystemPrompt()),
                        ChatMessage.of(ChatMessage.Role.USER, promptBuilder.buildUserPrompt(normalizedResumeText))
                ))
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .cachePolicy(CachePolicy.explicit())
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType(SkeletonCallType.RESUME_EXTRACTOR.value())
                .build();
    }

    private ResumeSkeleton toDomain(ExtractedResumeSkeleton raw, String fileHash) {
        List<Project> projects = mapProjects(raw.getProjects());
        Map<String, List<String>> priorityMap = raw.getInterrogationPriorityMap() != null
                ? raw.getInterrogationPriorityMap()
                : Map.of();

        return new ResumeSkeleton(
                raw.getResumeId(),
                fileHash,
                parseCandidateLevel(raw.getCandidateLevel()),
                raw.getTargetDomain(),
                projects,
                priorityMap
        );
    }

    private List<Project> mapProjects(List<ExtractedProject> rawProjects) {
        if (rawProjects == null) {
            return List.of();
        }
        return rawProjects.stream()
                .map(this::mapProject)
                .toList();
    }

    private Project mapProject(ExtractedProject raw) {
        List<ResumeClaim> claims = mapClaims(raw.getClaims());
        List<InterrogationChain> chains = mapChains(raw.getImplicitCsTopics());
        return new Project(raw.getProjectId(), claims, chains);
    }

    private List<ResumeClaim> mapClaims(List<ExtractedClaim> rawClaims) {
        if (rawClaims == null) {
            return List.of();
        }
        return rawClaims.stream()
                .map(this::mapClaim)
                .toList();
    }

    private ResumeClaim mapClaim(ExtractedClaim raw) {
        return new ResumeClaim(
                raw.getClaimId(),
                raw.getText(),
                ClaimType.fromOrDefault(raw.getClaimType(), ClaimType.IMPLEMENTATION),
                Priority.fromOrDefault(raw.getPriority(), Priority.MEDIUM),
                raw.getDepthHooks() != null ? raw.getDepthHooks() : List.of()
        );
    }

    private List<InterrogationChain> mapChains(List<ExtractedImplicitCsTopic> rawTopics) {
        if (rawTopics == null) {
            return List.of();
        }
        return rawTopics.stream()
                .filter(t -> t.getConfidence() >= MIN_CONFIDENCE_THRESHOLD)
                .map(this::mapChain)
                .filter(chain -> chain != null)
                .toList();
    }

    private InterrogationChain mapChain(ExtractedImplicitCsTopic raw) {
        List<ChainStep> steps = mapChainSteps(raw.getInterrogationChain());
        try {
            return new InterrogationChain(raw.getTopic(), raw.getConfidence(), steps);
        } catch (IllegalArgumentException e) {
            log.warn("InterrogationChain invariant 위반으로 드롭: topic={}, reason={}", raw.getTopic(), e.getMessage());
            return null;
        }
    }

    private List<ChainStep> mapChainSteps(List<ExtractedChainStep> rawSteps) {
        if (rawSteps == null) {
            return List.of();
        }
        return rawSteps.stream()
                .map(s -> new ChainStep(s.getLevel(), StepType.fromOrDefault(s.getType(), StepType.WHAT), s.getQuestion()))
                .toList();
    }

    private CandidateLevel parseCandidateLevel(String value) {
        if (value == null) {
            return CandidateLevel.JUNIOR;
        }
        return switch (value.toLowerCase()) {
            case "mid" -> CandidateLevel.MID;
            case "senior" -> CandidateLevel.SENIOR;
            default -> CandidateLevel.JUNIOR;
        };
    }
}
