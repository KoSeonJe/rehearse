package com.rehearse.api.domain.interview.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewTrack - 진행차단진단 로그 라벨 매핑")
class InterviewTrackTest {

    @Test
    @DisplayName("RESUME 트랙은 RESUME 라벨로 출력된다")
    void resume_logLabel() {
        assertThat(InterviewTrack.RESUME.logLabel()).isEqualTo("RESUME");
    }

    @Test
    @DisplayName("CS 트랙은 STANDARD 라벨로 출력된다")
    void cs_logLabel() {
        assertThat(InterviewTrack.CS.logLabel()).isEqualTo("STANDARD");
    }

    @Test
    @DisplayName("LANGUAGE 트랙은 STANDARD 라벨로 출력된다 (CS 동일 그룹)")
    void language_logLabel() {
        assertThat(InterviewTrack.LANGUAGE.logLabel()).isEqualTo("STANDARD");
    }
}
