package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ChainStep;
import com.rehearse.api.domain.resume.entity.ClaimType;
import com.rehearse.api.domain.resume.entity.InterrogationChain;
import com.rehearse.api.domain.resume.entity.Priority;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeClaim;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.StepType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.layer.SkeletonCallType;
import com.rehearse.api.infra.ai.dto.CachePolicy;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton.GeneratedChainStep;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton.GeneratedClaim;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton.GeneratedImplicitCsTopic;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton.GeneratedProject;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.prompt.ResumeExtractorPromptBuilder;
import java.util.ArrayList;
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

        GeneratedResumeSkeleton raw = aiResponseParser.parseOrRetry(
                response, GeneratedResumeSkeleton.class, aiClient, request);

        ResumeSkeleton skeleton = toDomain(raw, fileHash);
        long named = skeleton.projects().stream()
                .filter(p -> p.projectName() != null && !p.projectName().isBlank())
                .count();
        log.info("이력서 추출 완료: resumeId={}, projects={}, named={}, level={}",
                skeleton.resumeId(), skeleton.projects().size(), named, skeleton.candidateLevel());
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

    private ResumeSkeleton toDomain(GeneratedResumeSkeleton raw, String fileHash) {
        List<Project> projects = mapProjects(raw.projects());
        Map<String, List<String>> priorityMap = raw.interrogationPriorityMap() != null
                ? raw.interrogationPriorityMap()
                : Map.of();

        return new ResumeSkeleton(
                raw.resumeId(),
                fileHash,
                parseCandidateLevel(raw.candidateLevel()),
                raw.targetDomain(),
                projects,
                priorityMap
        );
    }

    private List<Project> mapProjects(List<GeneratedProject> rawProjects) {
        if (rawProjects == null) {
            return List.of();
        }
        List<Project> projects = new ArrayList<>(rawProjects.size());
        for (GeneratedProject raw : rawProjects) {
            projects.add(mapProject(raw));
        }
        return List.copyOf(projects);
    }

    private Project mapProject(GeneratedProject raw) {
        List<ResumeClaim> claims = mapClaims(raw.claims());
        List<InterrogationChain> chains = mapChains(raw.implicitCsTopics());
        return new Project(
                raw.projectId(),
                raw.projectName(),
                raw.techStack(),
                raw.role(),
                raw.architecture(),
                raw.decisions(),
                claims,
                chains
        );
    }

    private List<ResumeClaim> mapClaims(List<GeneratedClaim> rawClaims) {
        if (rawClaims == null) {
            return List.of();
        }
        return rawClaims.stream()
                .filter(c -> c.text() != null && !c.text().isBlank())
                .map(this::mapClaim)
                .toList();
    }

    private ResumeClaim mapClaim(GeneratedClaim raw) {
        return new ResumeClaim(
                raw.text(),
                ClaimType.fromOrDefault(raw.claimType(), ClaimType.IMPLEMENTATION),
                Priority.fromOrDefault(raw.priority(), Priority.MEDIUM)
        );
    }

    private List<InterrogationChain> mapChains(List<GeneratedImplicitCsTopic> rawTopics) {
        if (rawTopics == null) {
            return List.of();
        }
        return rawTopics.stream()
                .filter(t -> t.confidence() >= MIN_CONFIDENCE_THRESHOLD)
                .map(this::mapChain)
                .filter(chain -> chain != null)
                .toList();
    }

    private InterrogationChain mapChain(GeneratedImplicitCsTopic raw) {
        List<ChainStep> steps = mapChainSteps(raw.interrogationChain());
        try {
            return new InterrogationChain(raw.topic(), raw.confidence(), steps);
        } catch (IllegalArgumentException e) {
            log.warn("InterrogationChain invariant 위반으로 드롭: topic={}, reason={}", raw.topic(), e.getMessage());
            return null;
        }
    }

    private List<ChainStep> mapChainSteps(List<GeneratedChainStep> rawSteps) {
        if (rawSteps == null) {
            return List.of();
        }
        return rawSteps.stream()
                .map(s -> new ChainStep(s.level(), StepType.fromOrDefault(s.type(), StepType.WHAT), s.question()))
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
