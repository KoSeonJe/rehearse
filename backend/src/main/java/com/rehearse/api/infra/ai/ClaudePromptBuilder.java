package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ClaudePromptBuilder {

    private static final int MINUTES_PER_QUESTION = 3;
    private static final int MIN_QUESTION_COUNT = 2;
    private static final int MAX_QUESTION_COUNT = 24;
    private static final int SINGLE_TYPE_QUESTION_COUNT = 5;
    private static final int DOUBLE_TYPE_QUESTION_COUNT = 6;
    private static final int MULTI_TYPE_QUESTION_COUNT = 8;

    public static int calculateQuestionCount(Integer durationMinutes, int typeCount) {
        if (durationMinutes != null) {
            int count = (int) Math.round((double) durationMinutes / MINUTES_PER_QUESTION);
            return Math.max(MIN_QUESTION_COUNT, Math.min(count, MAX_QUESTION_COUNT));
        }
        return switch (typeCount) {
            case 1 -> SINGLE_TYPE_QUESTION_COUNT;
            case 2 -> DOUBLE_TYPE_QUESTION_COUNT;
            default -> MULTI_TYPE_QUESTION_COUNT;
        };
    }

    public String buildQuestionSystemPrompt() {
        return """
                당신은 한국 IT 기업의 시니어 개발자 면접관입니다.
                주어진 직무, 레벨, 면접 유형에 맞는 면접 질문을 생성해야 합니다.

                면접 유형별 출제 가이드:
                - CS_FUNDAMENTAL: CS 기초 (자료구조, 알고리즘, 운영체제, 네트워크, 데이터베이스)
                - BEHAVIORAL: STAR 기법 기반 경험 질문 (상황, 과제, 행동, 결과)
                - RESUME_BASED: 이력서/포트폴리오 기반 맞춤 질문
                - JAVA_SPRING: Java/Spring 프레임워크 심화 (JVM, Spring IoC/AOP, JPA, 트랜잭션)
                - SYSTEM_DESIGN: 시스템 아키텍처 설계, 스케일링, 트레이드오프 분석
                - FULLSTACK_JS: Node.js + React 풀스택, API 설계, DB 연동, 배포
                - REACT_COMPONENT: React 컴포넌트 설계, 상태 관리, 렌더링 최적화
                - BROWSER_PERFORMANCE: 브라우저 렌더링, 웹 성능 최적화, 번들 최적화
                - INFRA_CICD: 인프라 구성, CI/CD 파이프라인, 컨테이너 오케스트레이션
                - CLOUD: 클라우드 아키텍처 (AWS/GCP/Azure), 서버리스, IaC
                - DATA_PIPELINE: 데이터 수집/처리/적재 파이프라인, ETL/ELT, 스트리밍
                - SQL_MODELING: SQL 쿼리 최적화, 데이터 모델링, 정규화/반정규화

                CS 세부 주제가 지정된 경우 해당 주제에서만 출제하세요:
                - DATA_STRUCTURE: 자료구조와 알고리즘
                - OS: 운영체제 (프로세스, 스레드, 메모리, 스케줄링)
                - NETWORK: 네트워크 (TCP/IP, HTTP, DNS, 보안)
                - DATABASE: 데이터베이스 (인덱스, 트랜잭션, 정규화, 쿼리 최적화)

                질문 수 규칙:
                - 면접 시간이 설정된 경우: (면접 시간(분) / 3) 반올림 (최소 2개, 최대 24개)
                - 유형별로 균등 배분

                이력서가 제공된 경우 RESUME_BASED 유형의 질문은 이력서 내용을 기반으로 맞춤 생성하세요.

                레벨별 난이도:
                - JUNIOR: 기본 개념 이해도 확인, 실무 경험보다 학습 의지
                - MID: 실무 적용 능력, 문제 해결 경험, 기술적 깊이
                - SENIOR: 아키텍처 판단력, 리더십, 기술 의사결정 능력

                모범답변 생성 규칙:
                - 각 질문에 대한 모범답변(modelAnswer)을 반드시 포함하세요.
                - CS 카테고리(기술/CS) 질문: referenceType을 "MODEL_ANSWER"로, 정답이 있으므로 구체적 모범답변 제공
                - RESUME 카테고리(이력서 기반) 질문: referenceType을 "GUIDE"로, 정답이 없으므로 답변 방향 가이드 제공
                - questionCategory는 이력서/경험 기반이면 "RESUME", 기술/CS이면 "CS"로 지정

                반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.
                {
                  "questions": [
                    {
                      "content": "질문 내용",
                      "category": "세부 카테고리명",
                      "order": 1,
                      "evaluationCriteria": "이 질문에서 평가할 핵심 포인트",
                      "questionCategory": "RESUME 또는 CS",
                      "modelAnswer": "모범답변 또는 답변 가이드",
                      "referenceType": "MODEL_ANSWER 또는 GUIDE"
                    }
                  ]
                }
                """;
    }

    public String buildQuestionUserPrompt(Position position, String positionDetail,
                                           InterviewLevel level, List<InterviewType> interviewTypes,
                                           List<String> csSubTopics, String resumeText,
                                           Integer durationMinutes) {
        String positionKorean = switch (position) {
            case BACKEND -> "백엔드 개발자";
            case FRONTEND -> "프론트엔드 개발자";
            case DEVOPS -> "데브옵스 엔지니어";
            case DATA_ENGINEER -> "데이터 엔지니어";
            case FULLSTACK -> "풀스택 개발자";
        };

        String levelKorean = switch (level) {
            case JUNIOR -> "주니어";
            case MID -> "미드레벨";
            case SENIOR -> "시니어";
        };

        String typesKorean = interviewTypes.stream()
                .map(this::interviewTypeToKorean)
                .collect(Collectors.joining(", "));

        int questionCount = calculateQuestionCount(durationMinutes, interviewTypes.size());

        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("""
                직무: %s
                레벨: %s
                면접 유형: %s
                질문 수: %d개

                """, positionKorean, levelKorean, typesKorean, questionCount));

        if (csSubTopics != null && !csSubTopics.isEmpty() && interviewTypes.contains(InterviewType.CS_FUNDAMENTAL)) {
            String subTopicsKorean = csSubTopics.stream()
                    .map(this::csSubTopicToKorean)
                    .collect(Collectors.joining(", "));
            prompt.append(String.format("CS 세부 주제: %s\n\n", subTopicsKorean));
        }

        if (resumeText != null && !resumeText.isBlank()) {
            prompt.append(String.format("""
                    이력서/포트폴리오:
                    %s

                    """, resumeText));
        }

        prompt.append(String.format("세션 ID: %s\n", UUID.randomUUID()));
        prompt.append("이전 면접과 중복되지 않는 새로운 관점의 질문을 생성해주세요.\n");
        prompt.append("위 조건에 맞는 면접 질문과 각 질문별 평가 기준을 생성해주세요.\n");
        prompt.append("각 질문의 카테고리는 면접 유형의 세부 분야로 지정해주세요.\n");

        return prompt.toString();
    }

    private String interviewTypeToKorean(InterviewType type) {
        return switch (type) {
            case CS_FUNDAMENTAL -> "CS 기초";
            case BEHAVIORAL -> "Behavioral (인성/경험)";
            case RESUME_BASED -> "이력서/포트폴리오 기반";
            case JAVA_SPRING -> "Java/Spring 심화";
            case SYSTEM_DESIGN -> "시스템 설계";
            case FULLSTACK_JS -> "Node.js + React 풀스택";
            case REACT_COMPONENT -> "React/컴포넌트 설계";
            case BROWSER_PERFORMANCE -> "브라우저/웹 성능";
            case INFRA_CICD -> "인프라/CI-CD";
            case CLOUD -> "클라우드 아키텍처";
            case DATA_PIPELINE -> "데이터 파이프라인";
            case SQL_MODELING -> "SQL/데이터 모델링";
        };
    }

    private String csSubTopicToKorean(String subTopic) {
        return switch (subTopic) {
            case "DATA_STRUCTURE" -> "자료구조/알고리즘";
            case "OS" -> "운영체제";
            case "NETWORK" -> "네트워크";
            case "DATABASE" -> "데이터베이스";
            default -> subTopic;
        };
    }

    public String buildFollowUpSystemPrompt() {
        return """
                당신은 한국 IT 기업의 시니어 개발자 면접관입니다.
                면접자의 답변을 바탕으로 더 깊이 있는 후속 질문을 생성합니다.

                후속 질문 유형:
                - DEEP_DIVE: 답변의 특정 부분을 더 깊이 파고드는 질문
                - CLARIFICATION: 모호한 답변을 명확히 하기 위한 질문
                - CHALLENGE: 답변의 논리적 허점이나 대안을 탐색하는 질문
                - APPLICATION: 답변 내용을 다른 상황에 적용해보는 질문

                규칙:
                - 반드시 하나의 후속 질문만 생성하세요. 복합 질문은 금지합니다.
                - 이전 후속 대화가 제공된 경우, 이미 했던 질문과 중복되지 않는 새로운 관점의 질문을 생성하세요.
                - 매 라운드마다 다른 후속 질문 유형을 사용하여 다양한 각도에서 평가하세요.

                모범답변 생성 규칙:
                - 각 후속 질문에 대한 모범답변(modelAnswer)을 반드시 포함하세요.
                - 핵심 개념과 실무 적용 관점에서 2-4문장의 구체적인 답변 가이드를 제공하세요.

                반드시 아래 JSON 형식으로만 응답하세요:
                {
                  "question": "후속 질문 내용",
                  "reason": "이 질문을 하는 이유",
                  "type": "DEEP_DIVE|CLARIFICATION|CHALLENGE|APPLICATION",
                  "modelAnswer": "모범답변 또는 답변 가이드"
                }
                """;
    }

    public String buildFollowUpUserPrompt(String questionContent, String answerText,
                                           String nonVerbalSummary,
                                           List<FollowUpRequest.FollowUpExchange> previousExchanges) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("""
                원래 질문: %s
                면접자 답변: %s
                비언어적 관찰: %s
                """, questionContent, answerText,
                nonVerbalSummary != null ? nonVerbalSummary : "관찰 데이터 없음"));

        if (previousExchanges != null && !previousExchanges.isEmpty()) {
            prompt.append("\n이전 후속 대화:\n");
            for (int i = 0; i < previousExchanges.size(); i++) {
                FollowUpRequest.FollowUpExchange exchange = previousExchanges.get(i);
                prompt.append(String.format("[후속%d] Q: %s\n[후속%d] A: %s\n",
                        i + 1, exchange.getQuestion(), i + 1, exchange.getAnswer()));
            }
            prompt.append("\n위 대화를 바탕으로 새로운 후속 질문을 생성해주세요.\n");
            prompt.append("이전에 했던 질문과 중복되지 않는 새로운 관점의 질문이어야 합니다.\n");
        } else {
            prompt.append("\n위 답변에 대한 후속 질문을 생성해주세요.\n");
        }

        return prompt.toString();
    }

    public String buildReportSystemPrompt() {
        return """
                당신은 면접 코치입니다. 면접 피드백을 종합 분석하여 리포트를 생성합니다.

                반드시 아래 JSON 형식으로만 응답하세요:
                {
                  "overallScore": 75,
                  "summary": "종합 평가 요약 (2-3문장)",
                  "strengths": ["강점1", "강점2", "강점3"],
                  "improvements": ["개선점1", "개선점2", "개선점3"]
                }

                overallScore는 0-100 사이의 정수입니다.
                strengths와 improvements는 각각 최소 2개, 최대 5개 항목입니다.
                """;
    }

    public String buildReportUserPrompt(String feedbackSummary) {
        return String.format("""
                아래는 면접 피드백 데이터입니다. 종합 리포트를 생성해주세요.

                %s
                """, feedbackSummary);
    }

}
