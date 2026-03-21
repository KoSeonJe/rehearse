package com.rehearse.api.infra.ai.persona;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PersonaResolver {

    private static final BaseProfile DEFAULT_BASE_PROFILE = new BaseProfile(
            "당신은 한국 IT 기업의 시니어 개발자 면접관입니다.",
            "- 기술적 정확성\n- 논리적 사고력\n- 실무 적용 능력",
            "후속 질문에서 기술적 깊이를 추구하세요."
    );

    private final Map<Position, BaseProfile> baseProfiles;
    private final Map<TechStack, StackOverlay> stackOverlays;

    public PersonaResolver(ProfileYamlLoader loader) {
        this.baseProfiles = loader.loadBaseProfiles();
        this.stackOverlays = loader.loadStackOverlays();
        log.info("PersonaResolver 초기화 완료: base={}개, overlay={}개",
                baseProfiles.size(), stackOverlays.size());
    }

    public ResolvedProfile resolve(Position position, TechStack techStack) {
        BaseProfile base = baseProfiles.get(position);

        if (base == null) {
            log.warn("Base 프로필 없음: position={}, 기본 fallback 사용", position);
            base = DEFAULT_BASE_PROFILE;
        }

        StackOverlay overlay = stackOverlays.get(techStack);

        if (overlay == null) {
            log.debug("Stack 오버레이 없음: techStack={}, base only 프로필 반환", techStack);
            return ResolvedProfile.fromBaseOnly(base);
        }

        return new ResolvedProfile(
                base.personaBlock() + "\n" + overlay.fullPersona(),
                overlay.mediumPersona(),
                overlay.minimalPersona(),
                base.evaluationPerspective(),
                overlay.interviewTypeGuideMap(),
                base.followUpDepth() + "\n" + overlay.followUpDepthAppend(),
                overlay.verbalExpertise()
        );
    }
}
