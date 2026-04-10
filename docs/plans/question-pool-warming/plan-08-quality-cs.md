# Phase 1: CS Fundamental 전수 검증

## Why

CS 기초 질문은 position-agnostic으로 모든 사용자가 받는 질문.
답변 품질이 면접 준비에 직접 영향. 모든 답변이 "면접 베스트 답변"인지 검토.

## 대상

| 세션 | 파일 | 문항수 | 카테고리 |
|------|------|--------|---------|
| 1-A | `cs-fundamental-junior.sql` | 118 | 자료구조/OS/네트워크/DB |
| 1-B | `cs-fundamental-mid.sql` | 120 | 자료구조/OS/네트워크/DB |
| 1-C | `cs-fundamental-senior.sql` | 120 | 자료구조/OS/네트워크/DB |

## 검토 기준

- 질문에 대해 충분히 답변했는가?
- 기술적으로 정확한가?
- 핵심을 빠뜨리지 않았는가?
- 괜찮은 답변은 건드리지 않음

## Agent 할당

- **Implement**: `executor` — 부족한 답변만 수정
- **Review**: `code-reviewer` — 기술 정확성

## 검증

- [ ] 기술적 오류 0건
- [ ] INSERT 수 불변 (118/120/120)
- [ ] SQL 문법 유효
