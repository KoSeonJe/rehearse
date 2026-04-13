package com.rehearse.api.infra.ai.dto.claude;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CacheControl {

    private final String type;

    public static CacheControl ephemeral() {
        return new CacheControl("ephemeral");
    }
}
