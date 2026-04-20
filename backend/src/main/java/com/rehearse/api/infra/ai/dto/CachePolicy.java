package com.rehearse.api.infra.ai.dto;

/**
 * 프로바이더 캐시 정책.
 *
 * @param providerCache 캐시 전략
 * @param allowMiss     캐시 미스 허용 여부 — fallback 경로에서 true 로 설정해 콜드 미스 수용
 */
public record CachePolicy(
        ProviderCache providerCache,
        boolean allowMiss
) {

    public enum ProviderCache {
        /** OpenAI: Automatic Prompt Caching, Claude: 캐시 없음 (기본 경로) */
        AUTO,
        /** Claude: cache_control ephemeral 명시 마킹 */
        EXPLICIT,
        /** 캐시 비활성화 */
        NONE
    }

    public static CachePolicy defaults() {
        return new CachePolicy(ProviderCache.AUTO, false);
    }

    public static CachePolicy explicit() {
        return new CachePolicy(ProviderCache.EXPLICIT, false);
    }

    public static CachePolicy none() {
        return new CachePolicy(ProviderCache.NONE, false);
    }

    /** allowMiss 값만 변경한 새 CachePolicy 반환 */
    public CachePolicy withAllowMiss(boolean allowMiss) {
        return new CachePolicy(this.providerCache, allowMiss);
    }
}
