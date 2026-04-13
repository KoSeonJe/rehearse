package com.rehearse.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("RehearseApiApplication - 스프링 컨텍스트 로딩")
class RehearseApiApplicationTest {

    @Test
    @DisplayName("애플리케이션 컨텍스트가 예외 없이 로드된다")
    void contextLoads() {
        // when & then
        // 컨텍스트 로딩 자체가 검증 대상이므로 별도 assertion 없음
    }
}
