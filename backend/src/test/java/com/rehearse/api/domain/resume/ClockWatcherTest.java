package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClockWatcher - 세션 경과 시간 계산")
class ClockWatcherTest {

    @Mock
    private InterviewRuntimeStateStore runtimeStateStore;

    private InterviewRuntimeState state;

    private static final Instant FIXED_NOW = Instant.parse("2026-04-28T10:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
    }

    private ClockWatcher clockWatcher(Instant now) {
        Clock fixed = Clock.fixed(now, ZONE);
        return new ClockWatcher(runtimeStateStore, fixed);
    }

    @Nested
    @DisplayName("remainingMinutes 계산")
    class RemainingMinutes {

        @Test
        @DisplayName("startedAt 이 null 이면 durationMinutes 전체를 반환한다")
        void remainingMinutes_noStartedAt_returnsDuration() {
            given(runtimeStateStore.get(1L)).willReturn(state);

            long remaining = clockWatcher(FIXED_NOW).remainingMinutes(1L, 30);

            assertThat(remaining).isEqualTo(30);
        }

        @Test
        @DisplayName("세션 시작 후 정확히 10분 경과 시 20을 반환한다")
        void remainingMinutes_10minutesElapsed_returns20() {
            Instant startedAt = FIXED_NOW.minus(10, ChronoUnit.MINUTES);
            state.setStartedAt(startedAt);
            given(runtimeStateStore.get(1L)).willReturn(state);

            long remaining = clockWatcher(FIXED_NOW).remainingMinutes(1L, 30);

            assertThat(remaining).isEqualTo(20);
        }

        @Test
        @DisplayName("세션 시간이 초과되면 0을 반환한다 — 음수 반환 금지")
        void remainingMinutes_overtime_returnsZero() {
            Instant startedAt = FIXED_NOW.minus(60, ChronoUnit.MINUTES);
            state.setStartedAt(startedAt);
            given(runtimeStateStore.get(1L)).willReturn(state);

            long remaining = clockWatcher(FIXED_NOW).remainingMinutes(1L, 30);

            assertThat(remaining).isEqualTo(0);
        }

        @Test
        @DisplayName("남은 시간이 정확히 threshold 경계(2분)일 때 2를 반환한다")
        void remainingMinutes_exactlyAtThreshold_returns2() {
            Instant startedAt = FIXED_NOW.minus(28, ChronoUnit.MINUTES);
            state.setStartedAt(startedAt);
            given(runtimeStateStore.get(1L)).willReturn(state);

            long remaining = clockWatcher(FIXED_NOW).remainingMinutes(1L, 30);

            assertThat(remaining).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("markStart")
    class MarkStart {

        @Test
        @DisplayName("최초 markStart 호출 시 startedAt 이 Clock 기준 시각으로 설정된다")
        void markStart_setsStartedAt() {
            willAnswer(inv -> {
                java.util.function.Consumer<InterviewRuntimeState> mutator = inv.getArgument(1);
                mutator.accept(state);
                return null;
            }).given(runtimeStateStore).update(eq(1L), any());

            clockWatcher(FIXED_NOW).markStart(1L);

            assertThat(state.getStartedAt()).isEqualTo(FIXED_NOW);
        }

        @Test
        @DisplayName("startedAt 이 이미 설정된 경우 덮어쓰지 않는다 — idempotent")
        void markStart_idempotent_doesNotOverwrite() {
            Instant original = FIXED_NOW.minus(5, ChronoUnit.MINUTES);
            state.setStartedAt(original);
            willAnswer(inv -> {
                java.util.function.Consumer<InterviewRuntimeState> mutator = inv.getArgument(1);
                mutator.accept(state);
                return null;
            }).given(runtimeStateStore).update(eq(1L), any());

            clockWatcher(FIXED_NOW).markStart(1L);

            assertThat(state.getStartedAt()).isEqualTo(original);
        }
    }
}
