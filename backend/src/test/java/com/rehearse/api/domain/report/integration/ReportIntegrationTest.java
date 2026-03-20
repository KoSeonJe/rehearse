package com.rehearse.api.domain.report.integration;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.report.entity.InterviewReport;
import com.rehearse.api.domain.report.repository.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    @Test
    @DisplayName("GET /api/v1/interviews/{id}/report — ElementCollection LAZY 로딩 직렬화 성공")
    void getReport_serializesElementCollections() throws Exception {
        // given — 별도 트랜잭션에서 데이터 저장 후 세션 종료
        Long interviewId = txTemplate.execute(status -> {
            Interview interview = Interview.builder()
                    .position(Position.BACKEND)
                    .level(InterviewLevel.JUNIOR)
                    .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                    .durationMinutes(30)
                    .build();
            interviewRepository.save(interview);

            InterviewReport report = InterviewReport.builder()
                    .interview(interview)
                    .overallScore(85)
                    .summary("전반적으로 우수한 면접")
                    .strengths(List.of("논리적 사고", "기술적 깊이"))
                    .improvements(List.of("구체적 예시 부족"))
                    .feedbackCount(3)
                    .build();
            reportRepository.save(report);

            return interview.getId();
        });

        // when & then — 세션이 닫힌 상태에서 API 호출 (프로덕션 환경 재현)
        mockMvc.perform(get("/api/v1/interviews/" + interviewId + "/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.strengths").isArray())
                .andExpect(jsonPath("$.data.strengths.length()").value(2))
                .andExpect(jsonPath("$.data.strengths[0]").value("논리적 사고"))
                .andExpect(jsonPath("$.data.strengths[1]").value("기술적 깊이"))
                .andExpect(jsonPath("$.data.improvements.length()").value(1))
                .andExpect(jsonPath("$.data.improvements[0]").value("구체적 예시 부족"));
    }
}
