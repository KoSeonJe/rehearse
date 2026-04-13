package com.rehearse.api.global.util;

import org.springframework.http.ResponseCookie;

public final class CookieUtils {

    private static final String TOKEN_COOKIE_NAME = "rehearse_token";
    private static final String COOKIE_PATH = "/";
    private static final int MAX_AGE_SECONDS = 7 * 24 * 60 * 60; // 7일

    private CookieUtils() {}

    public static ResponseCookie createTokenCookie(String token) {
        return ResponseCookie.from(TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(MAX_AGE_SECONDS)
                .sameSite("Lax")
                .build();
    }

    public static ResponseCookie deleteTokenCookie() {
        return ResponseCookie.from(TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
