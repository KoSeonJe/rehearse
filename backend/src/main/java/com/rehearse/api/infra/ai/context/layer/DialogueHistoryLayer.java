package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.global.config.ContextEngineeringProperties;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.compaction.DialogueCompactor;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DialogueHistoryLayer implements ContextLayer {

    static final String RUNTIME_STATE_KEY = "interviewRuntimeState";
    static final String INTERVIEW_ID_KEY = "interviewId";

    private final DialogueCompactor dialogueCompactor;
    private final ContextEngineeringProperties properties;

    public DialogueHistoryLayer(DialogueCompactor dialogueCompactor, ContextEngineeringProperties properties) {
        this.dialogueCompactor = dialogueCompactor;
        this.properties = properties;
    }

    @Override
    public List<ChatMessage> build(ContextBuildRequest req) {
        List<FollowUpExchange> exchanges = req.exchanges();
        if (exchanges == null || exchanges.isEmpty()) {
            return List.of();
        }

        int recentWindow = properties.l3RecentWindow();
        if (exchanges.size() <= recentWindow) {
            return renderAlternating(exchanges);
        }

        return buildWithCompaction(req, exchanges, recentWindow);
    }

    private List<ChatMessage> buildWithCompaction(ContextBuildRequest req, List<FollowUpExchange> exchanges,
                                                  int recentWindow) {
        int windowEnd = exchanges.size() - recentWindow;
        List<FollowUpExchange> olderTurns = exchanges.subList(0, windowEnd);
        List<FollowUpExchange> recentTurns = exchanges.subList(windowEnd, exchanges.size());

        InterviewRuntimeState runtimeState =
                (InterviewRuntimeState) req.runtimeState().get(RUNTIME_STATE_KEY);
        Long interviewId = (Long) req.runtimeState().get(INTERVIEW_ID_KEY);

        List<ChatMessage> result = new ArrayList<>();

        if (runtimeState != null) {
            runtimeState.getCompactedSummary(windowEnd).ifPresentOrElse(
                    summary -> result.add(buildSummaryMessage(summary, windowEnd)),
                    () -> triggerCompactionIfPossible(interviewId, windowEnd, olderTurns, runtimeState)
            );
        }

        result.addAll(renderAlternating(recentTurns));
        return result;
    }

    private void triggerCompactionIfPossible(Long interviewId, int windowEnd,
                                             List<FollowUpExchange> olderTurns,
                                             InterviewRuntimeState runtimeState) {
        if (runtimeState.hasCompactionInFlight(windowEnd)) {
            return;
        }
        log.info("compaction triggered for windowEnd={}", windowEnd);
        dialogueCompactor.compactAsync(interviewId, windowEnd, olderTurns, runtimeState);
    }

    private ChatMessage buildSummaryMessage(String summary, int windowEnd) {
        String content = "## DIALOGUE SUMMARY (turns 1.." + windowEnd + ")\n" + summary;
        return ChatMessage.of(ChatMessage.Role.SYSTEM, content);
    }

    private List<ChatMessage> renderAlternating(List<FollowUpExchange> exchanges) {
        List<ChatMessage> messages = new ArrayList<>(exchanges.size() * 2);
        for (FollowUpExchange ex : exchanges) {
            messages.add(ChatMessage.of(ChatMessage.Role.USER, ex.getQuestion()));
            messages.add(ChatMessage.of(ChatMessage.Role.ASSISTANT, ex.getAnswer()));
        }
        return messages;
    }
}
