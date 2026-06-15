import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

// JwtTokenProvider 정합: secret 을 BASE64 디코딩 → HMAC-SHA256 raw 키, HS256 서명.
// 클레임: subject="1"(seed user id), role="USER" (필터가 "ROLE_" prefix 부착 → ROLE_USER).
const DEFAULT_SECRET =
  'bG9hZHRlc3RKd3RTZWNyZXRLZXlOb3RBUmVhbFNlY3JldE1pbmltdW0zMkNoYXJz';

function b64url(input) {
  return encoding
    .b64encode(input, 'std')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

// loadtest 전용 더미 secret 으로 HS256 JWT 1개 발급.
export function issueLoadTestJwt() {
  if (__ENV.JWT_TOKEN) {
    return __ENV.JWT_TOKEN;
  }

  const secret = __ENV.JWT_SECRET || DEFAULT_SECRET;
  const key = encoding.b64decode(secret); // raw HMAC 키 바이트 (ArrayBuffer)

  const nowSec = Math.floor(Date.now() / 1000);
  const header = { alg: 'HS256', typ: 'JWT' };
  const payload = {
    sub: '1',
    role: 'USER',
    iat: nowSec,
    exp: nowSec + 7 * 24 * 60 * 60,
  };

  const headerB64 = b64url(JSON.stringify(header));
  const payloadB64 = b64url(JSON.stringify(payload));
  const signingInput = `${headerB64}.${payloadB64}`;

  const sig = crypto.hmac('sha256', key, signingInput, 'binary'); // ArrayBuffer
  const sigB64 = b64url(sig);

  return `${signingInput}.${sigB64}`;
}

// AudioValidator 정합: audio/webm 매직바이트(EBML 0x1A45DFA3) 로 시작하는 더미 1KB webm.
// OpenAiAudioTurnAnalyzer 가 base64 인코딩 후 /v1/chat/completions(input_audio) 로 전송 → WireMock stub 응답.
export function dummyWebmBytes() {
  const size = 1024;
  const bytes = new Uint8Array(size);
  bytes[0] = 0x1a;
  bytes[1] = 0x45;
  bytes[2] = 0xdf;
  bytes[3] = 0xa3;
  for (let i = 4; i < size; i++) {
    bytes[i] = i % 256;
  }
  return bytes.buffer;
}
