# Task BE-08 — Flyway V{N} DROP 10컬럼 + entity / DTO / Response inner class cleanup

> **Phase**: 3 (Cleanup PR, BE only)
> **답하는 질문**: Phase 2a 머지 + dev 검증 후 자유서술 4 + 영속 raw 6 = 10컬럼 + 잔존 entity 필드 / DTO inner class 를 어떻게 안전히 일괄 폐기?

---

## 목적

Flyway V{N} 1건으로 `timestamp_feedback` 의 자유서술 4컬럼 (`nonverbal_comment` / `overall_comment` / `vocal_comment` / `attitude_comment`) + entity 영속 raw 6컬럼 (vocal 3: `speech_pace` / `tone_confidence_level` / `emotion_label` / vision 3: `eye_contact_level` / `posture_level` / `expression_label`) **총 10컬럼 DROP**. 필러 (`filler_word_count` / `filler_words`) = 예외 유지. `speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count` = entity 영속 부재 → DDL 대상 외 (Lambda 응답 부재만으로 처리 완결). 동시에 BE entity / `@Builder` / DTO / Response inner class (`DeliveryFeedback` / `NonverbalFeedback` / `VocalFeedback` / `CommentBlock`) + utility (`isCommentBlockEmpty` 등 BE 측) 잔존 참조 모두 정리.

## 에이전트

- **구현**: `backend` — Flyway DDL / entity 필드 삭제 / DTO 필드 / inner class 정리.
- **리뷰**: PR#3 머지 직전 `code-reviewer-backend`.

## 변경 파일

- `backend/src/main/resources/db/migration/V{N}__drop_timestamp_feedback_freetext_and_raw_columns.sql` — **신규**:
  ```sql
  -- Flyway 룰: DDL only (.claude/rules/conventions.md). DML 금지.
  -- V 번호 = 머지 직전 latest + 1.
  -- 컬럼명 = TimestampFeedback.java 의 @Column(name=...) 실측 (P0-A)
  ALTER TABLE timestamp_feedback
      DROP COLUMN nonverbal_comment,
      DROP COLUMN overall_comment,
      DROP COLUMN vocal_comment,
      DROP COLUMN attitude_comment,
      DROP COLUMN speech_pace,
      DROP COLUMN tone_confidence_level,
      DROP COLUMN emotion_label,
      DROP COLUMN eye_contact_level,
      DROP COLUMN posture_level,
      DROP COLUMN expression_label;
  ```
- `backend/src/main/java/com/rehearse/api/domain/feedback/entity/TimestampFeedback.java` (line 48-49, 51-52, 69-73, 75-102 추정)
  - 필드 제거: `nonverbalComment` / `overallComment` / `vocalComment` / `attitudeComment` (자유서술 4) + `speechPace` / `toneConfidenceLevel` / `emotionLabel` / `eyeContactLevel` / `postureLevel` / `expressionLabel` (raw 6)
  - `@Builder` 생성자 시그니처 동시 제거 (10 param)
  - 유지: `fillerWordCount` / `fillerWords` (필러 예외) + 기존 외 필드
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/SaveFeedbackRequest.java`
  - `TimestampFeedbackItem` 의 `CommentBlock 4종` (nonverbalComment / overallComment / vocalComment / attitudeComment) + raw 필드 제거 (구현 진입 직전 grep 으로 실제 필드 확인 — `speechPace` / `toneConfidenceLevel` 등)
  - `CommentBlock` inner class 참조 0 검증 후 삭제
  - `NonverbalScore` 잔존 (BE-05 재정의된 영역 키 분리 형태) — 변경 없음
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java`
  - inner class 삭제: `DeliveryFeedback` / `NonverbalFeedback` (4컬럼 read) / `VocalFeedback` / `CommentBlock`
  - `from(...)` 정적 팩토리에서 자유서술 4컬럼 + raw 6컬럼 read 코드 제거
  - 잔존: `NonverbalRubricFeedback` (BE-07 신설) + `TechnicalFeedback` + `TechnicalDimensionFeedback` + top-level `fillerWordCount` / `fillerWords`
  - 신규 응답 필드 `delivery` 자체 제거
- `backend/src/test/java/.../TimestampFeedbackRepositoryTest.java` — Flyway smoke + 컬럼 부재 assert (10컬럼) + 필러 컬럼 존재 assert
- `backend/src/test/java/.../*` — 자유서술 4 / raw 6 참조 테스트 삭제 또는 갱신

## 핵심 로직 / 변경 요약

```
[Pre]  DB: timestamp_feedback = 자유서술 4컬럼 + entity 영속 raw 6컬럼 잔존 (NULL 보장)
       entity: 10 필드 + @Builder param 잔존
       DTO: SaveFeedbackRequest.TimestampFeedbackItem 의 CommentBlock 4 + raw 필드 잔존
       Response: DeliveryFeedback / NonverbalFeedback (read) / VocalFeedback / CommentBlock inner class 잔존
       응답 JSON: technicalFeedback + nonverbalFeedback + delivery (vocal jet null)

[Post] DB: 10컬럼 DROP. 필러 2 유지.
       entity: 10 필드 + @Builder param 제거. 필러 유지.
       DTO: CommentBlock 4 + raw 필드 제거. CommentBlock inner class 삭제.
       Response: DeliveryFeedback / NonverbalFeedback (read) / VocalFeedback / CommentBlock 삭제.
                  from() 정적 팩토리에서 read 코드 제거.
       응답 JSON: technicalFeedback + nonverbalFeedback + top-level fillerWordCount / fillerWords.
                  delivery 키 부재.
```

## 의존

- **선결 조건 (게이트)**:
  - Phase 2a / 2b 머지 후 dev 신규 인터뷰 ≥ 3회 (각 ≥ 5 turn) → 10컬럼 100% NULL DB 확인
  - `grep -r "nonverbal_comment\\|nonverbalComment\\|vocal_comment\\|vocalComment\\|attitude_comment\\|attitudeComment\\|overall_comment\\|overallComment" backend/src frontend/src lambda` = 본 phase 변경 대상 외 0 매치
  - `grep -r "speechPace\\|toneConfidenceLevel\\|emotionLabel\\|speedVariance\\|eyeContactLevel\\|postureLevel\\|expressionLabel\\|gazeOnCameraRatio\\|postureUnstableCount" backend/src frontend/src` = 본 phase 변경 대상 외 0 매치
  - 사용자 명시 승인 (Phase 2a + 2b 검증 게이트 통과)
- **컬럼명 실측 검증 (P0-A)**: DDL 작성 직전 entity `@Column(name=...)` 또는 기본명 (필드명 → snake_case 변환) 으로 실측. 본 spec 실측 = 자유서술 4 + raw 6 (`speech_pace` / `tone_confidence_level` / `emotion_label` / `eye_contact_level` / `posture_level` / `expression_label`).

## 테스트 케이스

- [ ] **`TimestampFeedbackRepositoryTest.flyway_v_n_drops_10_columns`** (Repository, Testcontainers + Flyway):
  - Flyway V{N} 적용 후 `timestamp_feedback` 컬럼 목록 조회
  - 자유서술 4컬럼 부재 assert
  - raw 측정치 6컬럼 부재 assert
  - **필러 컬럼 (`filler_word_count` / `filler_words`) 존재 assert** (예외 유지)
- [ ] **회귀 — entity / DTO 컴파일**:
  - `./gradlew compileJava` green (10 필드 삭제 후 잔존 참조 0)
- [ ] **회귀 — 응답 DTO 시그니처**:
  - `TimestampFeedbackResponse` JSON serialization 시 `delivery` 키 부재 + `nonverbalFeedback` 키 존재 + top-level `fillerWordCount` 존재 assert
- [ ] **회귀 — grep**:
  - `grep -r "CommentBlock\\|DeliveryFeedback" backend/src` = 0
  - `grep -r "speechPace\\|toneConfidenceLevel\\|emotionLabel\\|eyeContactLevel\\|postureLevel\\|expressionLabel" backend/src` = 0
- [ ] **Migration smoke**:
  - `./gradlew bootRun --args='--spring.profiles.active=local'` 부팅 시 V{N} 적용 + 정상 기동
- [ ] **회귀**: `./gradlew test` 전체 green + `./gradlew build` green

## 완료 기준

- [ ] 변경 파일 commit (논리 단위: Flyway DDL / entity 필드 / DTO Request / DTO Response 분리 권장)
- [ ] PR#3 회귀 green
- [ ] **`code-reviewer-backend` 실행** (PR#3 머지 직전, MANDATORY)
- [ ] Phase 3 검증 게이트: Migration smoke + dev Flyway 적용 + 사용자 명시 승인

## 커밋 메시지

```
chore(BE): timestamp_feedback 자유서술 4 + raw 6 컬럼 DROP (Flyway V{N})
chore(BE): TimestampFeedback entity 10 필드 + @Builder param 제거
chore(BE): SaveFeedbackRequest CommentBlock / raw 필드 + CommentBlock inner class 삭제
chore(BE): TimestampFeedbackResponse Delivery / Nonverbal (read) / Vocal / CommentBlock 삭제
```

## 비고

- DDL 역마이그레이션 (`ADD COLUMN`) = 컬럼 구조만 복원, 과거 데이터 손실 (product-spec 비스코프 A — backfill 미수행 정합).
- `speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count` = entity 영속 부재 → DDL 대상 외 (Lambda 응답 부재만으로 처리 완결).
- 필러 (`filler_word_count` / `filler_words`) = 사용자 명시 예외 유지. 본 Task DDL 대상 외.
- Lambda 변경 0 (Phase 2a 에서 이미 산출 중단 + 자유서술 4종 / raw 9종 페이로드 키 부재).
- FE 변경 0 (Phase 2b 에서 이미 표시 제거 — FE-05 에서 type cleanup 동시).
- 보안 (A09): Flyway DDL 자체는 본문 없음 (구조만). 정합 차단 표면 부재.
