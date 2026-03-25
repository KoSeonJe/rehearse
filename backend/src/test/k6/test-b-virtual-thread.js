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

// ── 테스트 B: VT 도입 전후 비교 ──
// constant-arrival-rate: 20 → 50 → 80 → 100 → 120 → 150 req/s (각 30초)
// PT 한계(66 req/s)를 넘어서는 구간 포함
export const options = {
  scenarios: {
    stage1: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 200,
      maxVUs: 500,
      startTime: '0s',
    },
    stage2: {
      executor: 'constant-arrival-rate',
      rate: 50,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 300,
      maxVUs: 800,
      startTime: '30s',
    },
    stage3: {
      executor: 'constant-arrival-rate',
      rate: 80,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 500,
      maxVUs: 1200,
      startTime: '60s',
    },
    stage4: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 600,
      maxVUs: 1500,
      startTime: '90s',
    },
    stage5: {
      executor: 'constant-arrival-rate',
      rate: 120,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 700,
      maxVUs: 1800,
      startTime: '120s',
    },
    stage6: {
      executor: 'constant-arrival-rate',
      rate: 150,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 800,
      maxVUs: 2000,
      startTime: '150s',
    },
  },
};

// ── 요청 ──
export default function () {
  // 시나리오별 오프셋 + 글로벌 iteration → 면접 1건당 1회 요청 (race condition 제거)
  const scenarioOffset = { stage1: 0, stage2: 5000, stage3: 10000, stage4: 15000, stage5: 20000, stage6: 25000 }[exec.scenario.name] || 0;
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
