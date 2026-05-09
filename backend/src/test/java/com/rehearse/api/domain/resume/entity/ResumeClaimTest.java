package com.rehearse.api.domain.resume.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ResumeClaim record - text 단일 진실 invariant")
class ResumeClaimTest {

    @Nested
    @DisplayName("정상 생성")
    class HappyPath {

        @Test
        @DisplayName("text + claimType + priority 가 모두 유효하면 ResumeClaim 이 생성된다")
        void create_when_required_fields_present() {
            ResumeClaim claim = new ResumeClaim(
                    "Redis Cache-Aside 도입으로 응답 시간 50% 단축",
                    ClaimType.IMPACT_METRIC,
                    Priority.HIGH
            );

            assertThat(claim.text()).isEqualTo("Redis Cache-Aside 도입으로 응답 시간 50% 단축");
            assertThat(claim.claimType()).isEqualTo(ClaimType.IMPACT_METRIC);
            assertThat(claim.priority()).isEqualTo(Priority.HIGH);
        }
    }

    @Nested
    @DisplayName("text invariant")
    class TextInvariant {

        @Test
        @DisplayName("text 가 null 이면 예외가 발생한다")
        void throw_when_text_null() {
            assertThatThrownBy(() -> new ResumeClaim(null, ClaimType.IMPLEMENTATION, Priority.MEDIUM))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("text");
        }

        @Test
        @DisplayName("text 가 공백이면 예외가 발생한다")
        void throw_when_text_blank() {
            assertThatThrownBy(() -> new ResumeClaim("   ", ClaimType.IMPLEMENTATION, Priority.MEDIUM))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("text");
        }
    }

    @Nested
    @DisplayName("claimType / priority invariant")
    class EnumInvariant {

        @Test
        @DisplayName("claimType 이 null 이면 예외가 발생한다")
        void throw_when_claimType_null() {
            assertThatThrownBy(() -> new ResumeClaim("text", null, Priority.MEDIUM))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("claimType");
        }

        @Test
        @DisplayName("priority 가 null 이면 예외가 발생한다")
        void throw_when_priority_null() {
            assertThatThrownBy(() -> new ResumeClaim("text", ClaimType.IMPLEMENTATION, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("priority");
        }
    }
}
