-- JUNIOR BEHAVIORAL 질문 Pool 시딩 (20문항: 협업4, 문제해결5, 성장4, 리더십3, 커뮤니케이션4)
-- referenceType: GUIDE (모범답변이 아닌 답변 방향 가이드)

-- ============================================================
-- 협업 (4문항)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀 프로젝트에서 기술적 의견 충돌이 있었던 경험과 해결 과정을 설명해주세요.', '협업', 1,
 '감정이 아닌 근거 중심 접근, 상대 의견 존중, 팀 결정에 집중하는 태도',
 '[STAR 가이드]\nSituation: 어떤 기술적 선택지(REST vs GraphQL, 라이브러리 선택 등)를 두고 의견이 갈렸는지 구체적으로 제시.\nTask: 팀이 합의에 도달해야 했던 이유와 제약 조건 언급.\nAction: 각자의 주장을 문서화, 프로토타입/벤치마크로 객관적 비교, 상대 의견의 장점 인정.\nResult: 최종 결정 과정과 결과, 이 경험에서 배운 협업 원칙.\n\n핵심: "내 의견이 맞았다"가 아니라 "팀이 더 나은 결정을 내렸다"에 초점. 양보한 경험도 긍정적으로 풀어낼 것.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀원의 작업 속도가 느려 프로젝트 일정에 영향을 준 경험이 있나요? 어떻게 대처했나요?', '협업', 2,
 '비난이 아닌 지원 태도, 작업 재분배/페어 프로그래밍, 리스크 투명 공유',
 '[STAR 가이드]\nSituation: 특정 파트의 진행이 지연된 상황. 개인 비난 톤은 절대 피함.\nTask: 전체 일정을 맞추기 위해 팀 차원의 조율 필요.\nAction: 1:1로 어려움 확인, 작업 재분배 또는 페어 프로그래밍, 일정 리스크를 PM/리더에게 투명 공유.\nResult: "팀 전체가 함께 문제를 해결한 과정"과 이후 작업 분배 방식 개선.\n\n핵심: "도와줬다"보다 "함께 해결했다"는 표현이 좋음.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '코드 리뷰에서 본인의 코드에 대해 강한 피드백을 받은 경험이 있나요?', '협업', 3,
 '피드백을 성장 기회로 수용하는 태도, 방어 대신 이해 우선, 구체적 학습 내용',
 '[STAR 가이드]\nSituation: 어떤 코드에 대해 어떤 피드백을 받았는지 구체적으로(설계 전면 수정 요청, 성능 이슈 지적 등).\nTask: 피드백을 수용할지 반론할지 판단해야 했던 상황.\nAction: 즉각 방어하지 않고 근거를 먼저 이해, 모르는 부분 추가 질문, 수정 후 재리뷰 요청.\nResult: 코드 품질 개선 결과와 이후 변화한 코드 작성 습관.\n\n핵심: 구체적으로 무엇을 배웠는지가 중요.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '다른 직군(기획자, 디자이너 등)과 협업하면서 어려웠던 점과 극복 방법은?', '협업', 4,
 '용어/관점 차이 인식, 시각적 자료 활용, 내가 더 잘 전달하려는 노력',
 '[STAR 가이드]\nSituation: 비개발 직군과 용어/관점 차이로 소통이 어려웠던 상황.\nTask: 서로의 요구사항을 정확히 이해하고 기술적 제약을 전달해야 하는 과제.\nAction: 기술 용어를 쉽게 풀어 설명, 시각적 자료(목업, 다이어그램) 활용, 정기 싱크 미팅 제안.\nResult: 협업 프로세스 개선 결과와 이후 적용하게 된 원칙.\n\n핵심: "그쪽이 이해를 못 해서"가 아닌 "내가 더 잘 전달하려고 노력했다"에 초점.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

-- ============================================================
-- 문제해결 (5문항)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '프로덕션(또는 프로젝트)에서 긴급한 버그를 해결한 경험을 설명해주세요.', '문제해결', 5,
 '체계적 디버깅 과정(로그→가설→검증→수정), 임시 조치와 근본 해결 구분, 재발 방지',
 '[STAR 가이드]\nSituation: 어떤 서비스에서 어떤 증상의 버그가 발생했는지. 영향 범위(사용자 수, 장애 시간) 언급.\nTask: 빠른 원인 파악과 수정이 필요했던 긴박한 상황.\nAction: 로그 분석 → 원인 추정 → 가설 검증 → 수정 → 배포의 체계적 과정. 핫픽스와 근본 원인 해결을 구분.\nResult: 해결까지 걸린 시간, 재발 방지를 위한 모니터링/테스트 추가, 포스트모템 경험.\n\n핵심: 디버깅의 체계적 접근법과 재발 방지 노력을 보여줄 것.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '성능 이슈를 발견하고 개선한 경험이 있나요?', '문제해결', 6,
 '측정→분석→개선→검증 엔지니어링 접근, 구체적 수치 제시',
 '[STAR 가이드]\nSituation: 어떤 기능/API에서 성능 문제가 발생했는지, 어떻게 인지했는지.\nTask: 성능 목표(응답 시간, TPS)와 현재 상태의 격차를 수치로 제시.\nAction: 프로파일링 도구로 병목 분석, 구체적 개선(쿼리 최적화, 캐싱, 인덱스, 비동기 처리), 개선 전후 비교 측정.\nResult: 개선 수치(예: 3초→200ms)와 성능 최적화 원칙.\n\n핵심: "감으로 고쳤다"가 아닌 "측정 → 분석 → 개선 → 검증"의 접근.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '기술적으로 어떤 선택을 해야 할지 판단이 어려웠던 경험이 있나요?', '문제해결', 7,
 '합리적 근거 기반 결정, 판단 기준 수립 과정, PoC/팀 논의',
 '[STAR 가이드]\nSituation: 여러 기술적 선택지가 있고 각각 장단점이 명확했던 상황(새 기술 vs 검증된 기술, 직접 구현 vs 라이브러리).\nTask: 제한된 시간/자원 안에서 최선의 판단이 필요.\nAction: 판단 기준 수립(팀 역량, 학습 곡선, 유지보수성), 시니어와 논의, 필요 시 PoC 진행.\nResult: 최종 선택과 결과, 돌아보았을 때의 솔직한 평가.\n\n핵심: 완벽한 선택보다 "합리적 근거로 결정하고 책임지는 태도".',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '처음 접하는 기술이나 도메인을 빠르게 학습해서 적용한 경험이 있나요?', '문제해결', 8,
 '효율적 학습 전략, 공식 문서 우선, 학습→적용 과정, 팀 공유',
 '[STAR 가이드]\nSituation: 프로젝트 요구사항으로 전혀 모르는 기술/도메인을 다루게 된 배경.\nTask: 제한된 시간 안에 학습하고 실제 구현까지 완료해야 하는 과제.\nAction: 학습 전략(공식 문서 우선 → 핵심 개념 파악 → 예제 구현 → 프로젝트 적용), 멘토/커뮤니티 활용, 팀 공유.\nResult: 성공적으로 적용한 결과와 본인만의 학습 방법론.\n\n핵심: "빠르게 배웠다"는 결과보다 "효율적으로 학습하는 체계"가 있음을 보여줄 것.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '요구사항이 불명확하거나 자주 변경되는 상황에서 어떻게 대처했나요?', '문제해결', 9,
 '변경에 유연한 설계, 핵심/변동 영역 구분, 기획자와 싱크, 적응력',
 '[STAR 가이드]\nSituation: 기획이 확정되지 않았거나 중간에 크게 변경된 상황.\nTask: 불확실성 속에서도 개발을 진행하고 일정을 맞춰야 하는 과제.\nAction: 인터페이스 분리/모듈화로 변경에 유연한 설계, 핵심 요구사항과 변동 가능 영역 구분, 기획자와 자주 싱크하며 우선순위 재조정.\nResult: 최종 결과물의 품질과 변경에 유연하게 대응할 수 있었던 설계 효과.\n\n핵심: 불만보다 적응력. "변경 비용을 어떻게 최소화했는가"에 초점.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

-- ============================================================
-- 성장 (4문항)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '본인의 기술적 부족함을 인지하고 극복한 경험을 설명해주세요.', '성장', 10,
 '솔직한 한계 인정, 능동적 학습 계획, 실무 적용, 지속적 학습 습관',
 '[STAR 가이드]\nSituation: 기술적 한계를 체감한 구체적 상황(동시성 문제 이해 부족, DB 최적화 역량 부족 등).\nTask: 해당 역량을 반드시 채워야 했던 이유와 목표.\nAction: 학습 계획 수립(서적, 강의, 사이드 프로젝트), 꾸준한 학습, 실제 업무에 적용.\nResult: 역량 향상의 구체적 증거(문제 해결, 코드 품질 개선)와 지속하고 있는 학습 습관.\n\n핵심: 부족함을 솔직히 인정하면서도 능동적으로 극복하는 사람임을 보여줄 것.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '실패한 프로젝트나 기능이 있나요? 그 경험에서 무엇을 배웠나요?', '성장', 11,
 '실패를 솔직히 인정, 원인 분석(남 탓 아닌 본인 기여), 구체적 교훈과 개선',
 '[STAR 가이드]\nSituation: 기대한 결과를 달성하지 못한 프로젝트/기능의 배경을 솔직하게.\nTask: 당시 목표와 실제 결과의 격차를 구체적으로 제시.\nAction: 실패 원인 분석(기술적, 프로세스, 소통). 남 탓이 아닌 본인 기여 부분에 집중.\nResult: 구체적 교훈과 이후 동일한 실수를 방지하기 위해 적용한 개선 사항.\n\n핵심: 실패 자체보다 "실패로부터 배우고 개선하는 능력"이 핵심. 변명이 아닌 회고.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '사이드 프로젝트나 자기 주도 학습 경험에 대해 설명해주세요.', '성장', 12,
 '구체적 동기, 기획→설계→구현→배포 과정, 시간 관리, 자기주도성',
 '[STAR 가이드]\nSituation: 어떤 동기로 시작했는지(업무 부족함, 호기심, 기술 트렌드 등).\nTask: 달성하고자 했던 목표(기술 습득, 서비스 런칭, 오픈소스 기여 등).\nAction: 기획→설계→구현→배포 과정, 시간 관리(주말/퇴근 후 루틴), 기술적 도전과 해결.\nResult: 프로젝트 결과(사용자 수, GitHub 스타 등)와 성장한 역량.\n\n핵심: 완성도보다 "왜 시작했고, 무엇을 배웠는가"의 자기주도성.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '기술 블로그 작성이나 지식 공유 경험이 있나요?', '성장', 13,
 '지식 공유 동기, 어려운 개념을 쉽게 설명하는 노력, 팀 수준 향상 기여',
 '[STAR 가이드]\nSituation: 기술 블로그, 사내 위키, 스터디 발표 등을 시작한 계기.\nTask: 어떤 주제를 어떤 대상에게 전달해야 했는지.\nAction: 주제 선정 기준, 구조화 방법, 어려운 개념을 쉽게 설명하기 위한 노력, 피드백 반영.\nResult: 팀 온보딩 시간 단축, 블로그 반응, 본인 이해 심화 등 구체적 효과.\n\n핵심: "가르치면서 배운다"는 태도. 지식을 독점하지 않고 팀의 수준을 함께 끌어올리는 마인드.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

-- ============================================================
-- 리더십 (3문항)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀 프로젝트에서 주도적으로 방향을 이끈 경험이 있나요?', '리더십', 14,
 '문제 분석→방향 제안→역할 분담→합의 도출, 서번트 리더십',
 '[STAR 가이드]\nSituation: 팀이 방향을 잡지 못하거나 진행이 정체된 상황.\nTask: 누군가가 주도적으로 정리하고 이끌어야 했던 필요성.\nAction: 문제 분석 후 해결 방향 제안, 역할 분담과 일정 관리 주도, 팀원 의견 수렴하며 합의 도출.\nResult: 프로젝트 성과와 팀 역학 변화, 배운 리더십 원칙.\n\n핵심: "내가 다 했다"가 아닌 "팀이 움직일 수 있도록 촉진했다"는 서번트 리더십.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '신규 팀원(또는 후배)의 온보딩을 도운 경험이 있나요?', '리더십', 15,
 '온보딩 문서 정비, 페어 프로그래밍, 1:1 체크인, 상호 학습',
 '[STAR 가이드]\nSituation: 새로운 팀원 합류 또는 후배 멘토링 배경.\nTask: 빠르게 적응하고 기여할 수 있도록 돕는 것이 목표.\nAction: 온보딩 문서 정비, 프로젝트 구조/컨벤션 설명, 초기 이슈 배정과 페어 프로그래밍, 정기 1:1 체크인.\nResult: 온보딩 기간 단축, 첫 기여까지의 시간, 멘토링에서 본인이 배운 점.\n\n핵심: 가르치면서 본인도 성장했다는 상호 학습의 가치.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '기술 부채를 팀에 설득하여 개선한 경험이 있나요?', '리더십', 16,
 '기술 부채의 비즈니스 영향 정량화, 점진적 개선 계획, 비개발자 설득',
 '[STAR 가이드]\nSituation: 레거시 코드, 테스트 부재, 아키텍처 문제 등 기술 부채를 인지한 배경.\nTask: 기능 개발 일정과 기술 부채 해소 사이의 균형이 필요한 상황.\nAction: 비즈니스 영향(장애 빈도, 개발 속도 저하)을 정량화하여 설득, 점진적 개선 계획 수립, 리스크 명확히 제시.\nResult: 개선 후 효과(배포 시간 단축, 버그 감소, 생산성 향상)와 지속적 품질 관리 프로세스 도입.\n\n핵심: "기술 부채 = 비즈니스 리스크"로 번역하여 비개발자도 납득할 수 있게 설득.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

-- ============================================================
-- 커뮤니케이션 (4문항)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '복잡한 기술적 내용을 비개발자에게 설명한 경험이 있나요?', '커뮤니케이션', 17,
 '비유/시각 자료 활용, 기술 용어→비즈니스 임팩트 변환, 이해도 확인',
 '[STAR 가이드]\nSituation: 기획자/PM/경영진에게 기술적 상황(장애 원인, 구현 불가 사유 등)을 설명해야 했던 상황.\nTask: 상대방이 정확히 이해하고 올바른 의사결정을 할 수 있도록 전달.\nAction: 비유와 시각 자료 활용, 기술 용어를 비즈니스 임팩트로 변환, 핵심 3가지 이내로 정리, 질문 유도.\nResult: 상대방이 이해하고 적절한 의사결정을 내린 결과.\n\n핵심: "쉽게 설명하는 능력 = 깊이 이해하고 있다는 증거".',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '잘못된 방향으로 진행되고 있다고 느꼈을 때, 이를 팀에 어떻게 전달했나요?', '커뮤니케이션', 18,
 '근거 기반 문제 정리, 대안 함께 제시, 적절한 타이밍과 방식, 건설적 피드백',
 '[STAR 가이드]\nSituation: 프로젝트가 기술적/방향적으로 잘못되고 있다고 판단한 상황.\nTask: 팀 분위기나 상급자 결정에 반대되더라도 의견을 표현해야 했던 과제.\nAction: 개인적 불만이 아닌 근거 기반 정리, 대안을 함께 제시, 적절한 타이밍과 방식(1:1, 회고 미팅) 선택.\nResult: 팀의 반응과 최종 결정(수용/기각 모두 OK), 건설적 피드백 원칙.\n\n핵심: 반대 의견을 제시하는 용기 + 존중하는 방식. 결과와 무관하게 의견을 낸 것 자체가 가치.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '원격 근무나 비동기 환경에서 효과적으로 협업한 경험이 있나요?', '커뮤니케이션', 19,
 '비동기 커뮤니케이션 원칙, 문서화 습관, 도구 활용, 맥락 전달 능력',
 '[STAR 가이드]\nSituation: 팀원들과 물리적으로 떨어져 있거나 시간대가 다른 환경.\nTask: 대면 소통 없이도 프로젝트를 원활하게 진행해야 하는 과제.\nAction: 비동기 원칙(맥락 포함 메시지, 문서화 우선, 명확한 기한), 도구 활용(Slack, Notion, GitHub Issues), 정기 싱크 미팅과 비동기 업데이트 균형.\nResult: 프로젝트 성과와 비동기 협업에서 정립한 커뮤니케이션 원칙.\n\n핵심: "문서화 습관"과 "맥락 전달 능력"을 구체적 사례로 보여줄 것.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '마감 기한이 촉박한 상황에서 우선순위를 조정한 경험을 설명해주세요.', '커뮤니케이션', 20,
 'MoSCoW 우선순위 분류, 이해관계자와 합의, MVP 범위 재정의, 트레이드오프 인식',
 '[STAR 가이드]\nSituation: 남은 시간 대비 해야 할 작업이 과도했던 상황.\nTask: 주어진 시간 내에 최대한의 가치를 전달해야 하는 과제.\nAction: Must/Should/Could/Won''t로 기능 분류, 이해관계자와 MVP 범위 재정의, 기술 부채는 문서화하여 후속으로 미룸, 핵심 기능 테스트 커버리지 유지.\nResult: 일정 내 배포 성공 여부, 이후 기술 부채 해소, 시간 관리/우선순위 원칙.\n\n핵심: "다 해내겠다"가 아닌 "안 할 것을 결정하는 능력". 트레이드오프를 인지하고 소통하는 태도.',
 'GUIDE', 'REALTIME', 1.00, TRUE, NOW());
