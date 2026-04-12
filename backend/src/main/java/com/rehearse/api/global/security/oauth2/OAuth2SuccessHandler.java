package com.rehearse.api.global.security.oauth2;

import com.rehearse.api.global.security.jwt.JwtTokenProvider;
import com.rehearse.api.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        var user = oAuth2User.getUser();

        String token = jwtTokenProvider.createToken(
                user.getId(),
                user.getRole().name()
        );

        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.createTokenCookie(token).toString());

        String redirectUrl = resolveRedirectUrl(request);
        log.info("OAuth2 인증 성공, 리다이렉트: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String resolveRedirectUrl(HttpServletRequest request) {
        String redirect = request.getParameter("redirect");
        if (redirect != null && !redirect.isBlank()
                && redirect.startsWith("/") && !redirect.startsWith("//")) {
            return frontendUrl + redirect;
        }
        return frontendUrl + "/";
    }
}
