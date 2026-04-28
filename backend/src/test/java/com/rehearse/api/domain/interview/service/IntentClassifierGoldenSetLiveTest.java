package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.IntentResult;
import com.rehearse.api.domain.interview.entity.IntentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 골든셋 LIVE 테스트 — LIVE_TEST=true 환경변수가 있어야 실행됨.
 * Phase A 게이트 검증용. 실제 AI 호출이 발생하므로 CI에서는 실행하지 않는다.
 *
 * 실행 방법:
 *   LIVE_TEST=true ./gradlew test --tests "IntentClassifierGoldenSetLiveTest"
 *
 * LLM 출력은 비결정적이므로 개별 케이스 hard-assert 대신 집계 정확도 게이트로 검증한다.
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@EnabledIfEnvironmentVariable(named = "LIVE_TEST", matches = "true")
class IntentClassifierGoldenSetLiveTest {

    @Autowired
    private IntentClassifier intentClassifier;

    private static final double ACCURACY_THRESHOLD = 0.90;

    record GoldenCase(String mainQuestion, String answerText, IntentType expectedIntent) {}

    private static List<GoldenCase> goldenSet() {
        return List.of(
            // ANSWER × 10
            new GoldenCase(
                "JVM의 가비지 컬렉션 동작 방식을 설명해주세요.",
                "JVM은 힙 메모리를 Young Generation과 Old Generation으로 나누고 Minor GC와 Major GC가 각각 동작합니다.",
                IntentType.ANSWER
            ),
            new GoldenCase(
                "REST API에서 멱등성이란 무엇인가요?",
                "같은 요청을 여러 번 해도 결과가 동일한 성질입니다. GET, PUT, DELETE가 멱등하고 POST는 아닙니다.",
                IntentType.ANSWER
            ),
            new GoldenCase(
                "데이터베이스 인덱스를 사용하는 이유를 설명해주세요.",
                "정확하진 않을 수 있는데, 데이터를 정렬된 구조로 유지해서 검색 속도를 높이는 것 같습니다.",
                IntentType.ANSWER
            ),
            new GoldenCase(
                "TCP와 UDP의 차이점은 무엇인가요?",
                "TCP는 연결 지향적이고 신뢰성을 보장하며 UDP는 비연결 방식으로 속도가 빠르지만 손실이 있을 수 있습니다.",
                IntentType.ANSWER
            ),
            new GoldenCase(
                "Spring의 DI와 IoC에 대해 설명해주세요.",
                "IoC는 제어의 역전으로 객체 생성을 프레임워크가 담당하고, DI는 그 방법 중 하나로 의존성을 주입하는 것입니다.",
                IntentType.ANSWER
            ),
            new GoldenCase(
                "SQL에서 JOIN의 종류를 설명해주세요.",
                "INNER JOIN은 교집합, LEFT JOIN은 왼쪽 테이블 전체, RIGHT JOIN은 오른쪽 전체, FULL OUTER는 합집합입니다.",
                IntentType.ANSWER
            ),
            new GoldenCase(
                "해시 충돌 해결 방법에는 무엇이 있나요?",
                "체이닝과 오픈 어드레싱 방식이 있습니다. Java의 HashMap은 체이닝을 사용하는 것으로 알고 있습니다.",
                IntentType.ANSWER
            ),
            new GoldenCase(
                "동기와 비동기의 차이를 설명해주세요.",
                "동기는 작업이 끝날 때까지 기다리고 비동기는 작업 완료를 기다리지 않고 다음 작업을 진행합니다.",
                IntentType.ANSWER
            ),
            new GoldenCase(
                "OOP의 4대 원칙을 설명해주세요.",
                "캡슐화, 상속, 다형성, 추상화 네 가지입니다. 각각 데이터 은닉, 재사용성, 타입 유연성, 복잡성 감소를 위한 것입니다.",
                IntentType.ANSWER
            ),
            new GoldenCase(
                "Redis를 캐시로 사용하는 이유는 무엇인가요?",
                "인메모리 저장소라서 디스크 IO 없이 빠르게 읽을 수 있고, TTL 설정으로 자동 만료가 가능해서 세션이나 캐시에 많이 씁니다.",
                IntentType.ANSWER
            ),

            // CLARIFY_REQUEST × 5
            new GoldenCase(
                "서비스 디스커버리에 대해 설명해주세요.",
                "클라이언트 사이드와 서버 사이드 중 어떤 방식으로 설명하면 될까요?",
                IntentType.CLARIFY_REQUEST
            ),
            new GoldenCase(
                "트랜잭션 격리 수준에 대해 설명해주세요.",
                "죄송한데 트랜잭션 격리 수준이 정확히 어떤 맥락에서 나온 건지 다시 한번 설명해 주실 수 있을까요?",
                IntentType.CLARIFY_REQUEST
            ),
            new GoldenCase(
                "마이크로서비스 아키텍처의 장단점을 설명해주세요.",
                "어떤 규모의 시스템을 기준으로 설명하면 될까요? 스타트업 수준인지 대기업 수준인지에 따라 다를 것 같아서요.",
                IntentType.CLARIFY_REQUEST
            ),
            new GoldenCase(
                "CQRS 패턴에 대해 설명해주세요.",
                "CQRS가 어떤 약자인지 먼저 설명해 주실 수 있나요? 처음 들어보는 용어라서요.",
                IntentType.CLARIFY_REQUEST
            ),
            new GoldenCase(
                "이벤트 소싱 패턴을 설명해주세요.",
                "이벤트 소싱이 이벤트 드리븐 아키텍처와 같은 개념인가요? 아니면 다른 건가요?",
                IntentType.CLARIFY_REQUEST
            ),

            // GIVE_UP × 5
            new GoldenCase(
                "B-Tree와 B+Tree의 차이점을 설명해주세요.",
                "솔직히 이 부분은 잘 모르겠어요. 다음 질문으로 넘어가도 될까요?",
                IntentType.GIVE_UP
            ),
            new GoldenCase(
                "Raft 합의 알고리즘에 대해 설명해주세요.",
                "모르겠습니다. 공부하지 못한 부분이에요.",
                IntentType.GIVE_UP
            ),
            new GoldenCase(
                "JVM 바이트코드 최적화 방식을 설명해주세요.",
                "이건 너무 깊은 내용이라 잘 모르겠어요. 패스할게요.",
                IntentType.GIVE_UP
            ),
            new GoldenCase(
                "CAP 정리에 대해 설명해주세요.",
                "전혀 모르겠습니다. 건너뛰겠습니다.",
                IntentType.GIVE_UP
            ),
            new GoldenCase(
                "Lock-free 알고리즘의 동작 원리를 설명해주세요.",
                "잘 모르겠어요, 죄송합니다. 다음으로 넘어가주세요.",
                IntentType.GIVE_UP
            ),

            // OFF_TOPIC × 5
            new GoldenCase(
                "HashMap의 해시 충돌 해결 방법을 설명해주세요.",
                "시간이 얼마나 남았어요? 면접이 몇 시에 끝나는지 알고 싶어서요.",
                IntentType.OFF_TOPIC
            ),
            new GoldenCase(
                "인덱스를 사용하면 성능이 향상되는 이유를 설명해주세요.",
                "저 오늘 점심을 못 먹어서 좀 배가 고픈데, 면접 끝나고 근처에 맛있는 식당이 있을까요?",
                IntentType.OFF_TOPIC
            ),
            new GoldenCase(
                "Spring Security의 동작 방식을 설명해주세요.",
                "요즘 날씨가 많이 덥죠? 저는 여름이 정말 힘들더라고요.",
                IntentType.OFF_TOPIC
            ),
            new GoldenCase(
                "데이터베이스 정규화 1NF~3NF를 설명해주세요.",
                "화장실 다녀와도 될까요?",
                IntentType.OFF_TOPIC
            ),
            new GoldenCase(
                "멀티스레드 환경에서 동시성 문제를 어떻게 해결하나요?",
                "프론트엔드 개발자들은 이런 걸 신경 안 써도 되는데 저는 왜 백엔드를 선택했을까요.",
                IntentType.OFF_TOPIC
            )
        );
    }

    @Test
    @DisplayName("골든셋 25케이스 — 90% 이상 정확도 달성")
    void goldenSet_achievesAccuracyTarget() {
        List<GoldenCase> cases = goldenSet();
        int correct = 0;

        for (GoldenCase tc : cases) {
            IntentResult result = intentClassifier.classify(tc.mainQuestion(), tc.answerText(), null);
            if (result.type() == tc.expectedIntent()) {
                correct++;
            } else {
                System.out.printf("[MISS] expected=%s actual=%s | Q: %s | A: %s%n",
                        tc.expectedIntent(), result.type(), tc.mainQuestion(), tc.answerText());
            }
        }

        double accuracy = (double) correct / cases.size();
        System.out.printf("골든셋 정확도: %d/%d = %.1f%%%n", correct, cases.size(), accuracy * 100);

        assertThat(accuracy)
                .as("골든셋 정확도가 %.0f%% 미만입니다 (%d/%d 통과)",
                        ACCURACY_THRESHOLD * 100, correct, cases.size())
                .isGreaterThanOrEqualTo(ACCURACY_THRESHOLD);
    }
}
