import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// ── 커스텀 메트릭 ──
const errorRate = new Rate('error_rate');
const rateLimited = new Counter('rate_limited_count');
const followUpDuration = new Trend('followup_duration', true);

// ── 환경변수 ──
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MAX_INTERVIEW_ID = parseInt(__ENV.MAX_INTERVIEW_ID || '5000');

// ── 공통 ramp-up 시나리오: 한계점 탐색 ──
export const options = {
  scenarios: {
    rampup: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 10 },
        { duration: '30s', target: 10 },    // 10 VUs 유지
        { duration: '15s', target: 50 },
        { duration: '30s', target: 50 },    // 50 VUs 유지
        { duration: '15s', target: 100 },
        { duration: '30s', target: 100 },   // 100 VUs 유지
        { duration: '15s', target: 200 },
        { duration: '30s', target: 200 },   // 200 VUs 유지
        { duration: '15s', target: 300 },
        { duration: '30s', target: 300 },   // 300 VUs 유지
        { duration: '15s', target: 500 },
        { duration: '30s', target: 500 },   // 500 VUs 유지
        { duration: '15s', target: 0 },     // 쿨다운
      ],
    },
  },
  // threshold는 설정하지 않음 — 병목 발견이 목적이므로 에러를 허용
};

// ── 요청 ──
export default function () {
  // VU+iteration 기반 유니크 interview 할당 (후속질문 2개 제한 고려)
  const interviewId = ((__VU - 1) * 30 + Math.floor(__ITER / 2)) % MAX_INTERVIEW_ID + 1;
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
  const is429 = res.status === 429;
  const is400 = res.status === 400; // 비즈니스 로직 (후속질문 제한 등)

  check(res, {
    'status is 2xx': () => is2xx,
  });

  // 인프라 에러만 카운트 (400 비즈니스 에러, 429 rate limit 제외)
  errorRate.add(!is2xx && !is429 && !is400);

  if (is429) {
    rateLimited.add(1);
  }

  if (res.status >= 500) {
    console.error(`[${res.status}] ${res.body?.substring(0, 200)}`);
  }

  sleep(1);
}

// ── 종료 시 요약 ──
export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
