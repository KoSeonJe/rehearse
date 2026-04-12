package com.rehearse.api.domain.auth.controller;

import com.rehearse.api.domain.auth.dto.UserResponse;
import com.rehearse.api.domain.auth.exception.AuthErrorCode;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.service.UserService;
import com.rehearse.api.global.common.ApiResponse;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
        }
        User user = userService.findById(userId);
        return ApiResponse.ok(UserResponse.from(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.deleteTokenCookie().toString());
        return ApiResponse.ok();
    }
}
