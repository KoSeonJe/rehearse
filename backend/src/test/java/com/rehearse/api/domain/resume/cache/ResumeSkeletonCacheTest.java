package com.rehearse.api.domain.resume.cache;

import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.resume.domain.CandidateLevel;
import com.rehearse.api.domain.resume.cache.ResumeSkeletonCache;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeSkeletonCache - RuntimeState 기반 인메모리 캐시")
class ResumeSkeletonCacheTest {

    @InjectMocks
    private ResumeSkeletonCache cache;

    @Mock
    private InterviewRuntimeStateStore runtimeStateStore;

    @Test
    @DisplayName("read_returns_skeleton_when_hash_matches")
    void read_returns_skeleton_when_hash_matches() {
        ResumeSkeleton skeleton = createSkeleton("abc123");
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);

        given(runtimeStateStore.get(1L)).willReturn(state);

        ResumeSkeleton result = cache.read(1L, "abc123");

        assertThat(result).isEqualTo(skeleton);
    }

    @Test
    @DisplayName("read_returns_null_when_hash_mismatch")
    void read_returns_null_when_hash_mismatch() {
        ResumeSkeleton skeleton = createSkeleton("abc123");
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);

        given(runtimeStateStore.get(1L)).willReturn(state);

        ResumeSkeleton result = cache.read(1L, "different_hash");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("read_returns_null_when_cache_is_empty")
    void read_returns_null_when_cache_is_empty() {
        InterviewRuntimeState state = new InterviewRuntimeState("MID", null);

        given(runtimeStateStore.get(1L)).willReturn(state);

        ResumeSkeleton result = cache.read(1L, "abc123");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("read_returns_null_when_session_not_initialized")
    void read_returns_null_when_session_not_initialized() {
        given(runtimeStateStore.get(1L)).willThrow(new IllegalStateException("not initialized"));

        ResumeSkeleton result = cache.read(1L, "abc123");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("write_updates_runtime_state_with_skeleton")
    void write_updates_runtime_state_with_skeleton() {
        ResumeSkeleton skeleton = createSkeleton("abc123");
        willDoNothing().given(runtimeStateStore).update(eq(1L), any());

        cache.write(1L, skeleton);

        then(runtimeStateStore).should().update(eq(1L), any());
    }

    @Test
    @DisplayName("write_silently_skips_when_session_not_initialized")
    void write_silently_skips_when_session_not_initialized() {
        ResumeSkeleton skeleton = createSkeleton("abc123");
        willThrow(new IllegalStateException("not initialized"))
                .given(runtimeStateStore).update(eq(1L), any());

        cache.write(1L, skeleton);

        then(runtimeStateStore).should().update(eq(1L), any());
    }

    private ResumeSkeleton createSkeleton(String fileHash) {
        return new ResumeSkeleton(
                "r_test",
                fileHash,
                CandidateLevel.JUNIOR,
                "backend",
                List.of(),
                Map.of()
        );
    }
}
