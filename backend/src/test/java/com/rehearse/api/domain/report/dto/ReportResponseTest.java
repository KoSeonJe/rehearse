package com.rehearse.api.domain.report.dto;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.report.entity.InterviewReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportResponseTest {

    @Test
    @DisplayName("ReportResponse.from()은 strengths/improvements를 독립 복사본으로 반환한다")
    void from_copiesCollections() {
        // given
        List<String> strengths = new ArrayList<>(List.of("논리적 사고", "기술적 깊이"));
        List<String> improvements = new ArrayList<>(List.of("구체적 예시 부족"));

        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();

        InterviewReport report = InterviewReport.builder()
                .interview(interview)
                .overallScore(85)
                .summary("전반적으로 우수한 면접")
                .strengths(strengths)
                .improvements(improvements)
                .feedbackCount(3)
                .build();

        // when
        ReportResponse response = ReportResponse.from(report);

        // then — 원본 변경이 응답에 영향을 주지 않음
        strengths.add("추가 항목");
        improvements.add("추가 항목");

        assertThat(response.getStrengths()).hasSize(2);
        assertThat(response.getImprovements()).hasSize(1);

        // List.copyOf()는 불변 리스트를 반환
        assertThatThrownBy(() -> response.getStrengths().add("불변 테스트"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> response.getImprovements().add("불변 테스트"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("ReportResponse.from()은 빈 컬렉션도 정상 처리한다")
    void from_handlesEmptyCollections() {
        // given
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();

        InterviewReport report = InterviewReport.builder()
                .interview(interview)
                .overallScore(50)
                .summary("요약")
                .strengths(List.of())
                .improvements(List.of())
                .feedbackCount(0)
                .build();

        // when
        ReportResponse response = ReportResponse.from(report);

        // then
        assertThat(response.getStrengths()).isEmpty();
        assertThat(response.getImprovements()).isEmpty();
    }
}
