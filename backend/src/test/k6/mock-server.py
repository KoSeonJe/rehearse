"""Claude + Whisper API Mock Server for load testing.

Usage: python3 mock-server.py [port]
Default port: 9090

Prints request count per second to stderr for RateLimiter verification.
"""
import json
import os
import sys
import time
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from socketserver import ThreadingMixIn

CLAUDE_DELAY = float(os.environ.get("MOCK_DELAY", "2"))  # seconds
WHISPER_DELAY = 1.0  # seconds

# ── 초당 요청 카운터 ──
request_count = 0
count_lock = threading.Lock()


def count_reporter():
    """1초 간격으로 수신 요청 수 출력."""
    global request_count
    while True:
        time.sleep(1)
        with count_lock:
            cnt = request_count
            request_count = 0
        if cnt > 0:
            print(f"[mock] {int(time.time())} req/s={cnt}", file=sys.stderr, flush=True)


CLAUDE_RESPONSE = json.dumps({
    "id": "msg_stub",
    "type": "message",
    "role": "assistant",
    "content": [
        {
            "type": "text",
            "text": json.dumps({
                "question": "그 함수의 시간복잡도를 어떻게 분석하셨나요?",
                "reason": "답변에서 제시한 알고리즘의 효율성 검증",
                "type": "DEEP_DIVE",
                "modelAnswer": "이진 탐색은 O(log n) 시간복잡도를 가집니다."
            })
        }
    ],
    "model": "claude-haiku-4-5-20251001",
    "stop_reason": "end_turn",
    "usage": {"input_tokens": 100, "output_tokens": 50}
})

WHISPER_RESPONSE = "안녕하세요. 저는 백엔드 개발자입니다. Spring Framework를 주로 사용하고 있습니다."


class MockHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        global request_count
        content_length = int(self.headers.get("Content-Length", 0))
        self.rfile.read(content_length)  # consume body

        with count_lock:
            request_count += 1

        if "/v1/messages" in self.path:
            time.sleep(CLAUDE_DELAY)
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(CLAUDE_RESPONSE.encode())

        elif "/v1/audio/transcriptions" in self.path:
            time.sleep(WHISPER_DELAY)
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(WHISPER_RESPONSE.encode())

        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        pass  # suppress per-request logs


class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 9090
    server = ThreadingHTTPServer(("0.0.0.0", port), MockHandler)
    print(f"Mock API server running on port {port}")
    print(f"  Claude: POST /v1/messages ({CLAUDE_DELAY}s delay)")
    print(f"  Whisper: POST /v1/audio/transcriptions ({WHISPER_DELAY}s delay)")
    print(f"  Request counter: printing req/s to stderr")

    # 카운터 리포터 데몬 시작
    reporter = threading.Thread(target=count_reporter, daemon=True)
    reporter.start()

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutdown.")
