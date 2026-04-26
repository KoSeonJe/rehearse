package com.rehearse.api.domain.interview.repository;

import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class InterviewRepositoryTest {

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("EntityGraph로 ElementCollection이 세션 종료 후에도 접근 가능하다")
    void findByIdWithElementCollections() {
        // given
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.LANGUAGE_FRAMEWORK))
                .csSubTopics(List.of("자료구조", "운영체제"))
                .durationMinutes(30)
                .build();

        interviewRepository.save(interview);
        entityManager.flush();
        entityManager.clear();

        // when
        Interview found = interviewRepository.findByIdWithElementCollections(interview.getId()).orElseThrow();
        entityManager.clear();

        // then
        assertThat(found.getInterviewTypes()).containsExactlyInAnyOrder(
                InterviewType.CS_FUNDAMENTAL, InterviewType.LANGUAGE_FRAMEWORK);
        assertThat(found.getCsSubTopics()).containsExactlyInAnyOrder("자료구조", "운영체제");
        assertThat(found.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
    }

    @Test
    @DisplayName("EntityGraph로 로드된 면접의 ElementCollection이 세션 종료 후에도 정확한 값으로 노출된다")
    void findByIdWithElementCollections_afterSessionClear_returnsExactValues() {
        Interview interview = Interview.builder()
                .position(Position.FRONTEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.UI_FRAMEWORK))
                .csSubTopics(List.of())
                .durationMinutes(20)
                .build();

        interviewRepository.save(interview);
        entityManager.flush();
        entityManager.clear();

        Interview found = interviewRepository.findByIdWithElementCollections(interview.getId()).orElseThrow();
        entityManager.clear();

        assertThat(found.getInterviewTypes()).containsExactly(InterviewType.UI_FRAMEWORK);
        assertThat(found.getCsSubTopics()).isEmpty();
        assertThat(found.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
    }

    @Test
    @DisplayName("단일 interviewType으로 저장 시 findByIdWithElementCollections가 정확히 해당 타입만 반환한다")
    void findByIdWithElementCollections_singleType_returnsExactMatch() {
        // given
        Interview interview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .csSubTopics(List.of("자료구조"))
                .durationMinutes(30)
                .build();

        interviewRepository.save(interview);
        entityManager.flush();
        entityManager.clear();

        // when
        Interview found = interviewRepository.findByIdWithElementCollections(interview.getId()).orElseThrow();
        entityManager.clear();

        // then
        assertThat(found.getInterviewTypes())
                .hasSize(1)
                .containsExactly(InterviewType.CS_FUNDAMENTAL);
        assertThat(found.getCsSubTopics())
                .hasSize(1)
                .containsExactly("자료구조");
    }

    @Test
    @DisplayName("단일 interviewType으로 저장 시 findByPublicId가 정확히 해당 타입만 반환한다")
    void findByPublicId_singleType_returnsExactMatch() {
        // given
        Interview interview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .csSubTopics(List.of())
                .durationMinutes(30)
                .build();

        interviewRepository.save(interview);
        entityManager.flush();
        entityManager.clear();

        // when
        Interview found = interviewRepository.findByPublicId(interview.getPublicId()).orElseThrow();
        entityManager.clear();

        // then
        assertThat(found.getInterviewTypes())
                .hasSize(1)
                .containsExactly(InterviewType.CS_FUNDAMENTAL);
    }

    @Test
    @DisplayName("다른 면접의 ElementCollection이 영속성 컨텍스트에 먼저 로드돼도 단일 type 면접 조회는 오염되지 않는다 (H2 회귀 보호)")
    void findByIdWithElementCollections_persistenceContextPollutionScenario() {
        Interview polluter = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.BEHAVIORAL, InterviewType.LANGUAGE_FRAMEWORK))
                .durationMinutes(30)
                .build();

        Interview target = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();

        interviewRepository.save(polluter);
        interviewRepository.save(target);
        entityManager.flush();
        entityManager.clear();

        interviewRepository.findByIdWithElementCollections(polluter.getId()).orElseThrow();
        Interview foundTarget = interviewRepository.findByIdWithElementCollections(target.getId()).orElseThrow();
        entityManager.clear();

        Set<InterviewType> types = foundTarget.getInterviewTypes();
        assertThat(types)
                .hasSize(1)
                .containsExactly(InterviewType.CS_FUNDAMENTAL)
                .doesNotContain(InterviewType.BEHAVIORAL, InterviewType.LANGUAGE_FRAMEWORK);
    }

    @Test
    @DisplayName("findAllByUserId 목록 조회 시 각 면접의 interviewTypes가 저장값과 일치한다")
    void findAllByUserId_singleType_returnsExactTypes() {
        // given
        Interview csOnly = Interview.builder()
                .userId(2L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();

        interviewRepository.save(csOnly);
        entityManager.flush();
        entityManager.clear();

        // when
        Page<Interview> page = interviewRepository.findAllByUserId(2L, PageRequest.of(0, 10));

        // then
        assertThat(page.getContent()).hasSize(1);
        Interview found = page.getContent().get(0);
        assertThat(found.getInterviewTypes())
                .hasSize(1)
                .containsExactly(InterviewType.CS_FUNDAMENTAL);
    }

    @Test
    @DisplayName("createdAt이 강제로 동일한 면접 2개를 size=1로 두 페이지 조회 시 tie-break 정렬로 중복/누락 없이 2건이 반환된다")
    void findAllByUserId_sameCreatedAt_noDuplicateWithTieBreak() {
        Long userId = 3L;
        Interview first = Interview.builder()
                .userId(userId)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();

        Interview second = Interview.builder()
                .userId(userId)
                .position(Position.FRONTEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.UI_FRAMEWORK))
                .durationMinutes(20)
                .build();

        interviewRepository.save(first);
        interviewRepository.save(second);
        entityManager.flush();

        LocalDateTime fixed = LocalDateTime.of(2026, 4, 26, 10, 0, 0);
        ReflectionTestUtils.setField(first, "createdAt", fixed);
        ReflectionTestUtils.setField(second, "createdAt", fixed);
        entityManager.merge(first);
        entityManager.merge(second);
        entityManager.flush();
        entityManager.clear();

        Page<Interview> page0 = interviewRepository.findAllByUserId(userId, PageRequest.of(0, 1));
        Page<Interview> page1 = interviewRepository.findAllByUserId(userId, PageRequest.of(1, 1));

        assertThat(page0.getTotalElements()).isEqualTo(2);
        List<Long> allIds = new ArrayList<>();
        allIds.addAll(page0.getContent().stream().map(Interview::getId).toList());
        allIds.addAll(page1.getContent().stream().map(Interview::getId).toList());
        assertThat(allIds).hasSize(2).doesNotHaveDuplicates();
        assertThat(allIds).containsExactlyInAnyOrder(first.getId(), second.getId());
    }
}
