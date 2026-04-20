package com.rehearse.api.infra.ai;

import com.rehearse.api.infra.ai.adapter.FollowUpGenerationAdapter;
import com.rehearse.api.infra.ai.adapter.QuestionGenerationAdapter;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AiClient 구현 기반 클래스.
 *
 * <p>legacy 3개 메서드({@code generateQuestions}, {@code generateFollowUpQuestion},
 * {@code generateFollowUpWithAudio})를 어댑터를 통해 {@code chat()} 경유로 위임한다.
 * 구현체({@link ResilientAiClient}, {@link MockAiClient})는 이 클래스를 상속하고
 * {@code chat()} 만 구현하면 된다.</p>
 *
 * <p>{@link MockAiClient} 의 경우 테스트 목적으로 override 가능하다.</p>
 */
public abstract class AbstractAiClient implements AiClient {

    protected final QuestionGenerationAdapter questionAdapter;
    protected final FollowUpGenerationAdapter followUpAdapter;
    protected final SttService sttService;

    protected AbstractAiClient(
            QuestionGenerationAdapter questionAdapter,
            FollowUpGenerationAdapter followUpAdapter,
            SttService sttService) {
        this.questionAdapter = questionAdapter;
        this.followUpAdapter = followUpAdapter;
        this.sttService = sttService;
    }

    @Override
    public List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request) {
        return questionAdapter.adapt(this, request);
    }

    @Override
    public GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request) {
        return followUpAdapter.adapt(this, request);
    }

    @Override
    public GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request) {
        return followUpAdapter.adaptWithAudio(this, audioFile, request, sttService);
    }
}
