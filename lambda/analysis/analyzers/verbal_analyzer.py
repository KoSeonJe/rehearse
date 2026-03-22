from __future__ import annotations

import time

from openai import OpenAI, RateLimitError, AuthenticationError

from config import Config
from analyzers.json_utils import parse_llm_json
from analyzers.prompts import KOREAN_INSTRUCTION
from analyzers.verbal_prompt_factory import build_system_prompt, build_user_prompt

MAX_RETRIES = 3
RETRY_DELAY = 2

_SYSTEM_PROMPT = KOREAN_INSTRUCTION + """당신은 면접 언어 분석 전문가입니다.
면접자의 답변 텍스트를 분석하여 언어적 커뮤니케이션을 평가합니다.

평가 기준:
1. 답변 논리성 (verbal_score: 0-100)
   - STAR 기법 등 구조화된 답변인지
   - 질문에 대한 핵심 답변이 포함되었는지
   - 논리적 흐름이 자연스러운지
2. 필러워드 개수 (filler_word_count)
   - "음", "어", "그", "아", "뭐", "이제", "약간" 등 불필요한 습관어
3. 핵심 키워드 활용
4. 말투/어조 적절성

5. 말투 분석 (tone_label)
   - PROFESSIONAL: 격식체, 면접에 적합한 어조
   - CASUAL: 반말이나 구어체 섞임
   - HESITANT: 자신감 없는 어조, "~인 것 같아요", "아마~" 등
   - CONFIDENT: 단정적이고 확신 있는 어조
   - VERBOSE: 불필요하게 장황한 설명

반드시 아래 JSON 형식으로만 응답하세요:
{
  "verbal_score": <0-100>,
  "filler_word_count": <정수>,
  "tone_label": "<PROFESSIONAL|CASUAL|HESITANT|CONFIDENT|VERBOSE>",
  "tone_comment": "<한국어로 1-2문장의 말투 피드백>",
  "comment": "<한국어로 3-4문장의 언어 분석 피드백>"
}"""


def analyze_verbal(
    question_text: str,
    transcript: str,
    position: str | None = None,
    tech_stack: str | None = None,
    level: str | None = None,
    model_answer: str | None = None,
) -> dict:
    if not transcript or not transcript.strip():
        print("[Verbal] 전사 텍스트 없음 — 기본값 반환")
        return {
            "verbal_score": 0,
            "filler_word_count": 0,
            "comment": "답변 음성이 감지되지 않았습니다.",
        }

    client = OpenAI(api_key=Config.OPENAI_API_KEY)

    if position:
        system_prompt = build_system_prompt(position, tech_stack)
        user_prompt = build_user_prompt(
            position, tech_stack, level or "JUNIOR",
            question_text, transcript, model_answer,
        )
    else:
        system_prompt = _SYSTEM_PROMPT
        user_prompt = f"질문: {question_text}\n답변(STT): {transcript}"

    for attempt in range(MAX_RETRIES):
        try:
            response = client.chat.completions.create(
                model=Config.LLM_MODEL,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                max_tokens=500,
                temperature=0.3,
            )
            raw = response.choices[0].message.content.strip()
            result = parse_llm_json(raw)
            print(f"[Verbal] 분석 완료: score={result.get('verbal_score')}, fillers={result.get('filler_word_count')}")
            return result

        except AuthenticationError:
            raise

        except RateLimitError as e:
            wait = RETRY_DELAY * (2 ** attempt)
            print(f"[Verbal] 시도 {attempt + 1}/{MAX_RETRIES} RateLimitError — {wait}초 후 재시도")
            if attempt < MAX_RETRIES - 1:
                time.sleep(wait)
            else:
                raise

        except Exception as e:
            print(f"[Verbal] 시도 {attempt + 1}/{MAX_RETRIES} 실패: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_DELAY * (attempt + 1))
            else:
                raise
