package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeSkeletonEntity;
import com.rehearse.api.domain.resume.repository.ResumeSkeletonRepository;
import com.rehearse.api.domain.resume.service.ResumeSkeletonCodec;
import com.rehearse.api.domain.resume.service.ResumeSkeletonPersister;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeSkeletonPersister - DB read/write 위임 (직렬화는 Codec 담당)")
class ResumeSkeletonPersisterTest {

    @InjectMocks
    private ResumeSkeletonPersister store;

    @Mock
    private ResumeSkeletonRepository skeletonRepository;

    @Mock
    private ResumeSkeletonCodec resumeSkeletonCodec;

    @Test
    @DisplayName("findByInterviewId_returns_codec_deserialized_skeleton_when_entity_exists")
    void findByInterviewId_returns_codec_deserialized_skeleton_when_entity_exists() {
        ResumeSkeletonEntity entity = createEntity("abc123");
        ResumeSkeleton skeleton = createSkeleton("abc123");

        given(skeletonRepository.findByInterviewId(1L)).willReturn(Optional.of(entity));
        given(resumeSkeletonCodec.deserialize(entity)).willReturn(skeleton);

        Optional<ResumeSkeleton> result = store.findByInterviewId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().fileHash()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("findByInterviewId_returns_empty_when_no_entity")
    void findByInterviewId_returns_empty_when_no_entity() {
        given(skeletonRepository.findByInterviewId(99L)).willReturn(Optional.empty());

        Optional<ResumeSkeleton> result = store.findByInterviewId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save_codec_로_직렬화_후_repository_저장")
    void save_persists_entity_with_codec_serialized_json() {
        ResumeSkeleton skeleton = createSkeleton("abc123");
        given(resumeSkeletonCodec.serialize(skeleton)).willReturn("{\"fileHash\":\"abc123\"}");

        store.save(1L, skeleton);

        then(resumeSkeletonCodec).should().serialize(skeleton);
        then(skeletonRepository).should().save(any(ResumeSkeletonEntity.class));
    }

    private ResumeSkeletonEntity createEntity(String fileHash) {
        return ResumeSkeletonEntity.builder()
                .interviewId(1L)
                .fileHash(fileHash)
                .candidateLevel("JUNIOR")
                .targetDomain("backend")
                .skeletonJson("{}")
                .build();
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
