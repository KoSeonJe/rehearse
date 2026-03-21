package com.rehearse.api.infra.ai.persona;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Position + TechStack 조합으로 ResolvedProfile을 생성하는 컴포넌트.
 *
 * <p>애플리케이션 시작 시 ProfileYamlLoader를 통해 YAML을 1회 로드하고 캐싱한다.
 * YAML 로딩 실패 또는 매핑 누락 시에도 최소 동작을 보장하는 fallback을 제공한다.
 */
@Slf4j
@Component
public class PersonaResolver {

    /** YAML 로딩 실패 또는 base profile 누락 시 사용하는 하드코딩 최소 프로필 */
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

    /**
     * Position과 TechStack을 기반으로 병합된 ResolvedProfile을 반환한다.
     *
     * <p>병합 규칙:
     * <ul>
     *   <li>base 없음 → DEFAULT_BASE_PROFILE 사용</li>
     *   <li>overlay 없음 → base만으로 프로필 생성 (fromBaseOnly)</li>
     *   <li>둘 다 있음 → 스펙의 APPEND/REPLACE/KEEP 규칙 적용</li>
     * </ul>
     *
     * @param position  면접 직군
     * @param techStack 기술 스택 (null 불가 — Interview.getEffectiveTechStack() 사용 권장)
     * @return 병합된 최종 페르소나 프로필
     */
    public ResolvedProfile resolve(Position position, TechStack techStack) {
        BaseProfile base = baseProfiles.get(position);

        // base profile 누락 방어 — YAML 로딩 실패 또는 신규 Position 미지원 시
        if (base == null) {
            log.warn("Base 프로필 없음: position={}, 기본 fallback 사용", position);
            base = DEFAULT_BASE_PROFILE;
        }

        StackOverlay overlay = stackOverlays.get(techStack);

        // overlay 없으면 base만으로 동작 — 신규 TechStack 추가 전까지의 graceful degradation
        if (overlay == null) {
            log.debug("Stack 오버레이 없음: techStack={}, base only 프로필 반환", techStack);
            return ResolvedProfile.fromBaseOnly(base);
        }

        // APPEND / REPLACE / KEEP 병합
        return new ResolvedProfile(
                base.personaBlock() + "\n" + overlay.fullPersona(),           // APPEND
                overlay.mediumPersona(),                                        // REPLACE
                overlay.minimalPersona(),                                       // REPLACE
                base.evaluationPerspective(),                                   // KEEP
                overlay.interviewTypeGuideMap(),                                // REPLACE
                base.followUpDepth() + "\n" + overlay.followUpDepthAppend(),   // APPEND
                overlay.verbalExpertise()                                       // REPLACE
        );
    }
}
