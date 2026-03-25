import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Rate, Trend, Counter } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// ── 커스텀 메트릭 ──
const errorRate = new Rate('error_rate');
const followUpDuration = new Trend('followup_duration', true);
const successCount = new Counter('success_count');
const failCount = new Counter('fail_count');

// ── 환경변수 ──
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MAX_INTERVIEW_ID = parseInt(__ENV.MAX_INTERVIEW_ID || '50000');

// ── 테스트 A: TX 분리 전후 비교 ──
// constant-arrival-rate: 10 → 20 → 30 req/s (각 30초)
// pool 10 + 3초 점유 = 최대 3 req/s이므로 10만 넘어도 차이 극명
export const options = {
  scenarios: {
    stage1: {
      executor: 'constant-arrival-rate',
      rate: 10,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 100,
      maxVUs: 500,
      startTime: '0s',
    },
    stage2: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 200,
      maxVUs: 800,
      startTime: '30s',
    },
    stage3: {
      executor: 'constant-arrival-rate',
      rate: 30,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 300,
      maxVUs: 1000,
      startTime: '60s',
    },
  },
};

// ── 요청 ──
export default function () {
  // 시나리오별 오프셋 + 글로벌 iteration → 면접 1건당 1회 요청 (race condition 제거)
  const scenarioOffset = { stage1: 0, stage2: 10000, stage3: 20000 }[exec.scenario.name] || 0;
  const interviewId = (exec.scenario.iterationInInstance + scenarioOffset) % MAX_INTERVIEW_ID + 1;
  const questionSetId = interviewId;

  const boundary = '----k6boundary' + Math.random().toString(36).substr(2);
  const requestJson = JSON.stringify({
    questionSetId: questionSetId,
    questionContent: '정렬 알고리즘의 종류와 각각의 시간복잡도를 설명해주세요.',
    answerText: '퀵정렬은 평균 O(n log n)이고 최악은 O(n^2)입니다. 병합정렬은 항상 O(n log n)이며 안정 정렬입니다.',
    previousExchanges: [],
  });

  const body =
    `--${boundary}\r\n` +
    `Content-Disposition: form-data; name="request"\r\n` +
    `Content-Type: application/json\r\n\r\n` +
    `${requestJson}\r\n` +
    `--${boundary}--\r\n`;

  const params = {
    headers: {
      'Content-Type': `multipart/form-data; boundary=${boundary}`,
    },
    timeout: '120s',
  };

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/v1/interviews/${interviewId}/follow-up`,
    body,
    params,
  );
  const elapsed = Date.now() - start;

  followUpDuration.add(elapsed);

  const is2xx = res.status >= 200 && res.status < 300;
  const is400 = res.status === 400;

  check(res, {
    'status is 2xx': () => is2xx,
  });

  if (is2xx) {
    successCount.add(1);
  } else if (!is400) {
    failCount.add(1);
  }

  // 인프라 에러만 카운트 (400 비즈니스 에러 제외)
  errorRate.add(!is2xx && !is400);

  if (res.status >= 500) {
    console.error(`[${res.status}] interview=${interviewId} ${res.body?.substring(0, 200)}`);
  }
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
