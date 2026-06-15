package com.rehearse.api.infra.ai.dto;

public record CachePolicy(
        ProviderCache providerCache,
        boolean allowMiss
) {

    public enum ProviderCache {
        AUTO,      // OpenAI Automatic Prompt Caching 만, Claude 는 캐시 없음
        EXPLICIT,  // Claude cache_control: ephemeral 마킹
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

    public CachePolicy withAllowMiss(boolean allowMiss) {
        return new CachePolicy(this.providerCache, allowMiss);
    }
}
