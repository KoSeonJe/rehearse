-- JUNIOR BEHAVIORAL 질문 Pool 시딩 (30문항: 협업6, 문제해결6, 성장6, 리더십6, 커뮤니케이션6)
-- referenceType: GUIDE (모범답변이 아닌 STAR 답변 방향 가이드)
-- V20 이후 스키마 기준 (question_order, evaluation_criteria, follow_up_strategy, quality_score 컬럼 없음)

-- ============================================================
-- 협업 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀 프로젝트에서 기술적 의견 충돌이 있었던 경험과 해결 과정을 설명해주세요.', '팀 프로젝트에서 기술적 의견 충돌이 있었던 경험과 해결 과정을 설명해주세요.', '협업',
 '[STAR 가이드]\nSituation: 어떤 기술적 선택지(REST vs GraphQL, 라이브러리 선택 등)를 두고 의견이 갈렸는지 구체적으로 제시.\nTask: 팀이 합의에 도달해야 했던 이유와 제약 조건 언급.\nAction: 각자의 주장을 문서화, 프로토타입/벤치마크로 객관적 비교, 상대 의견의 장점 인정.\nResult: 최종 결정 과정과 결과, 이 경험에서 배운 협업 원칙.\n\n핵심: "내 의견이 맞았다"가 아니라 "팀이 더 나은 결정을 내렸다"에 초점. 양보한 경험도 긍정적으로 풀어낼 것.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '코드 리뷰에서 본인의 코드에 대해 강한 피드백을 받은 경험이 있나요? 어떻게 대처했나요?', '코드 리뷰에서 본인의 코드에 대해 강한 피드백을 받은 경험이 있나요? 어떻게 대처했나요?', '협업',
 '[STAR 가이드]\nSituation: 어떤 코드에 대해 어떤 피드백을 받았는지 구체적으로(설계 전면 수정 요청, 성능 이슈 지적 등).\nTask: 피드백을 수용할지 반론할지 판단해야 했던 상황.\nAction: 즉각 방어하지 않고 근거를 먼저 이해, 모르는 부분 추가 질문, 수정 후 재리뷰 요청.\nResult: 코드 품질 개선 결과와 이후 변화한 코드 작성 습관.\n\n핵심: 구체적으로 무엇을 배웠는지가 중요. 피드백을 성장 기회로 수용하는 태도를 보여줄 것.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '처음 만나는 팀원과 프로젝트를 시작하면서 신뢰를 쌓은 경험을 이야기해주세요.', '처음 만나는 팀원과 프로젝트를 시작하면서 신뢰를 쌓은 경험을 이야기해주세요.', '협업',
 '[STAR 가이드]\nSituation: 이전에 같이 일해본 적 없는 팀원과 프로젝트를 함께 시작한 배경.\nTask: 짧은 시간 안에 팀워크를 만들고 성과를 내야 했던 상황.\nAction: 역할과 기대치를 명확히 정의, 작은 성공 경험 쌓기, 1:1 대화로 작업 방식 파악, 투명한 진행 상황 공유.\nResult: 협업 효율이 높아진 시점과 프로젝트 결과.\n\n핵심: 신뢰는 결과가 아닌 "예측 가능성"에서 온다. 약속을 지키는 것이 신뢰의 기반임을 강조.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '다른 직군(기획자, 디자이너 등)과 협업하면서 가장 어려웠던 점과 극복 방법은 무엇인가요?', '다른 직군(기획자, 디자이너 등)과 협업하면서 가장 어려웠던 점과 극복 방법은 무엇인가요?', '협업',
 '[STAR 가이드]\nSituation: 비개발 직군과 용어/관점 차이로 소통이 어려웠던 상황.\nTask: 서로의 요구사항을 정확히 이해하고 기술적 제약을 전달해야 하는 과제.\nAction: 기술 용어를 쉽게 풀어 설명, 시각적 자료(목업, 다이어그램) 활용, 정기 싱크 미팅 제안.\nResult: 협업 프로세스 개선 결과와 이후 적용하게 된 원칙.\n\n핵심: "그쪽이 이해를 못 해서"가 아닌 "내가 더 잘 전달하려고 노력했다"에 초점.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀원이 본인의 의견에 반대했을 때, 어떻게 대응하고 해결했나요?', '팀원이 본인의 의견에 반대했을 때, 어떻게 대응하고 해결했나요?', '협업',
 '[STAR 가이드]\nSituation: 본인이 제안한 방식이나 결정에 팀원이 명확히 반대한 상황.\nTask: 갈등 없이 팀의 방향을 결정해야 하는 과제.\nAction: 상대 의견을 경청하고 논리를 파악, 본인 의견에 대한 근거 정리, 제3의 방안 탐색 또는 데이터로 결정.\nResult: 도달한 합의와 팀 분위기 변화, 이후 의사결정 프로세스 개선.\n\n핵심: "내가 이겼다/졌다"보다 "팀이 더 나은 결정을 했다"는 관점으로 마무리.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '원격(비대면) 환경에서 팀원과 효과적으로 소통했던 경험을 설명해주세요.', '원격(비대면) 환경에서 팀원과 효과적으로 소통했던 경험을 설명해주세요.', '협업',
 '[STAR 가이드]\nSituation: 팀원들과 물리적으로 떨어져 있거나 시간대가 다른 환경에서 협업.\nTask: 대면 소통 없이도 프로젝트를 원활하게 진행해야 하는 과제.\nAction: 비동기 원칙(맥락 포함 메시지, 문서화 우선, 명확한 기한), 도구 활용(Slack, Notion, GitHub Issues), 정기 싱크 미팅과 비동기 업데이트 균형.\nResult: 프로젝트 성과와 비동기 협업에서 정립한 커뮤니케이션 원칙.\n\n핵심: 문서화 습관과 맥락 전달 능력을 구체적 사례로 보여줄 것.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 문제해결 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '예상치 못한 기술적 문제가 발생했을 때 어떻게 대응했나요?', '예상치 못한 기술적 문제가 발생했을 때 어떻게 대응했나요?', '문제해결',
 '[STAR 가이드]\nSituation: 어떤 서비스에서 어떤 증상의 문제가 발생했는지. 영향 범위 언급.\nTask: 빠른 원인 파악과 수정이 필요했던 긴박한 상황.\nAction: 로그 분석 → 원인 추정 → 가설 검증 → 수정 → 배포의 체계적 과정. 임시 조치와 근본 원인 해결을 구분.\nResult: 해결까지 걸린 시간, 재발 방지를 위한 조치, 배운 점.\n\n핵심: 디버깅의 체계적 접근법과 재발 방지 노력을 보여줄 것.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '오랫동안 원인을 찾지 못했던 어려운 버그를 결국 수정한 경험이 있나요?', '오랫동안 원인을 찾지 못했던 어려운 버그를 결국 수정한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 버그 증상과 재현 조건, 얼마나 오래 원인을 찾지 못했는지.\nTask: 정확한 원인 파악과 사이드이펙트 없는 수정이 목표.\nAction: 가설 기반 디버깅(로그, 디버거, 재현 스크립트), 막혔을 때 동료 도움 요청 또는 관점 전환, 코드 이분 탐색(bisect).\nResult: 발견한 근본 원인과 수정 방법, 이후 유사 버그 예방책.\n\n핵심: 포기하지 않는 끈기와 체계적 접근이 핵심. "어떤 방식으로 범위를 좁혀갔는지" 구체적으로.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '프로젝트 일정이 촉박해졌을 때 어떻게 대처했나요?', '프로젝트 일정이 촉박해졌을 때 어떻게 대처했나요?', '문제해결',
 '[STAR 가이드]\nSituation: 예상보다 일정이 지연되거나 범위가 늘어난 배경.\nTask: 주어진 시간 내에 최대한의 가치를 전달해야 하는 과제.\nAction: Must/Should/Could/Won''t로 기능 분류, 이해관계자와 MVP 범위 재정의, 기술 부채는 문서화하여 후속으로 미룸, 핵심 기능 테스트 커버리지 유지.\nResult: 일정 내 배포 성공 여부, 이후 기술 부채 해소, 시간 관리 원칙.\n\n핵심: "다 해내겠다"가 아닌 "안 할 것을 결정하는 능력". 트레이드오프를 인지하고 소통하는 태도.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '기술적 선택(라이브러리, 프레임워크 등)을 잘못했다고 느낀 경험이 있나요? 어떻게 극복했나요?', '기술적 선택(라이브러리, 프레임워크 등)을 잘못했다고 느낀 경험이 있나요? 어떻게 극복했나요?', '문제해결',
 '[STAR 가이드]\nSituation: 어떤 기술을 선택했고, 어떤 시점에 문제가 드러났는지.\nTask: 이미 진행된 상황에서 방향 전환 또는 보완이 필요한 과제.\nAction: 피해 범위 파악, 리팩토링 vs 교체 vs 우회 방안 비교, 팀과 상황 공유 후 최선안 선택.\nResult: 결과적으로 어떻게 해결했는지, 다음번에 기술 선택 시 달라진 기준.\n\n핵심: 실수를 인정하는 용기 + 빠른 피벗 능력. 실수보다 "수습 과정"이 중요.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '문서가 거의 없는 레거시 코드를 분석하고 이해한 경험이 있나요?', '문서가 거의 없는 레거시 코드를 분석하고 이해한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 담당자도 없고 문서도 없는 레거시 코드를 파악해야 했던 상황.\nTask: 코드를 이해하고 수정 또는 확장해야 하는 과제.\nAction: 진입점(엔트리포인트)부터 역방향 탐색, 테스트 코드 작성으로 동작 검증, 이해한 내용 즉시 문서화, 도메인 전문가 인터뷰.\nResult: 기능 파악 후 성공적으로 수정/추가한 결과와 남긴 문서의 효과.\n\n핵심: "읽은 것"보다 "이해를 검증한 방법(테스트 작성, 문서화)"을 구체적으로.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '요구사항이 중간에 크게 변경된 상황에서 어떻게 대응했나요?', '요구사항이 중간에 크게 변경된 상황에서 어떻게 대응했나요?', '문제해결',
 '[STAR 가이드]\nSituation: 기획이 확정되지 않았거나 중간에 크게 변경된 상황.\nTask: 불확실성 속에서도 개발을 진행하고 일정을 맞춰야 하는 과제.\nAction: 인터페이스 분리/모듈화로 변경에 유연한 설계, 핵심 요구사항과 변동 가능 영역 구분, 기획자와 자주 싱크하며 우선순위 재조정.\nResult: 최종 결과물의 품질과 변경에 유연하게 대응할 수 있었던 설계 효과.\n\n핵심: 불만보다 적응력. "변경 비용을 어떻게 최소화했는가"에 초점.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 성장 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '새로운 언어나 프레임워크를 처음 학습해서 실무에 적용한 경험을 이야기해주세요.', '새로운 언어나 프레임워크를 처음 학습해서 실무에 적용한 경험을 이야기해주세요.', '성장',
 '[STAR 가이드]\nSituation: 프로젝트 요구사항으로 전혀 모르는 기술을 다루게 된 배경.\nTask: 제한된 시간 안에 학습하고 실제 구현까지 완료해야 하는 과제.\nAction: 학습 전략(공식 문서 우선 → 핵심 개념 파악 → 예제 구현 → 프로젝트 적용), 멘토/커뮤니티 활용, 팀 공유.\nResult: 성공적으로 적용한 결과와 본인만의 학습 방법론.\n\n핵심: "빠르게 배웠다"는 결과보다 "효율적으로 학습하는 체계"가 있음을 보여줄 것.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '본인의 기술적 약점을 인식하고 개선한 경험을 설명해주세요.', '본인의 기술적 약점을 인식하고 개선한 경험을 설명해주세요.', '성장',
 '[STAR 가이드]\nSituation: 기술적 한계를 체감한 구체적 상황(동시성 문제 이해 부족, DB 최적화 역량 부족 등).\nTask: 해당 역량을 반드시 채워야 했던 이유와 목표.\nAction: 학습 계획 수립(서적, 강의, 사이드 프로젝트), 꾸준한 학습, 실제 업무에 적용.\nResult: 역량 향상의 구체적 증거(문제 해결, 코드 품질 개선)와 지속하고 있는 학습 습관.\n\n핵심: 부족함을 솔직히 인정하면서도 능동적으로 극복하는 사람임을 보여줄 것.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '프로젝트 회고에서 본인이 가장 크게 배운 점은 무엇인가요?', '프로젝트 회고에서 본인이 가장 크게 배운 점은 무엇인가요?', '성장',
 '[STAR 가이드]\nSituation: 어떤 프로젝트를 마무리하고 회고를 진행한 배경.\nTask: 팀 또는 개인 차원에서 다음 프로젝트를 더 잘하기 위한 교훈 도출.\nAction: KPT(Keep/Problem/Try) 또는 5Why 방법으로 회고, 문제의 근본 원인 탐색, 실행 가능한 액션 아이템 도출.\nResult: 이후 프로젝트에서 실제로 적용된 변화와 그 효과.\n\n핵심: 회고를 "형식적 절차"가 아닌 "실질적 개선 도구"로 활용한 경험을 강조.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '학습 중 슬럼프나 장벽을 경험했을 때 어떻게 극복했나요?', '학습 중 슬럼프나 장벽을 경험했을 때 어떻게 극복했나요?', '성장',
 '[STAR 가이드]\nSituation: 공부가 막히거나 동기를 잃은 구체적 시점과 원인.\nTask: 다시 학습 흐름을 되찾고 목표를 달성해야 했던 상황.\nAction: 슬럼프 원인 분석(번아웃, 방향 상실, 난이도), 해결 전략(학습 목표 세분화, 스터디 그룹 참여, 작은 성공 경험 쌓기).\nResult: 슬럼프를 극복한 후 달성한 성과와 이후 동기 유지 전략.\n\n핵심: 어려움을 숨기지 않되 "극복한 방법"에 집중. 자기 인식과 회복 탄력성을 보여줄 것.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '기술 블로그 작성이나 발표 등 지식 공유 활동을 한 경험이 있나요?', '기술 블로그 작성이나 발표 등 지식 공유 활동을 한 경험이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 기술 블로그, 사내 위키, 스터디 발표 등을 시작한 계기.\nTask: 어떤 주제를 어떤 대상에게 전달해야 했는지.\nAction: 주제 선정 기준, 구조화 방법, 어려운 개념을 쉽게 설명하기 위한 노력, 피드백 반영.\nResult: 팀 온보딩 시간 단축, 블로그 반응, 본인 이해 심화 등 구체적 효과.\n\n핵심: "가르치면서 배운다"는 태도. 지식을 독점하지 않고 팀의 수준을 함께 끌어올리는 마인드.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '사이드 프로젝트나 오픈소스 기여 등 자기 주도 학습 경험에 대해 설명해주세요.', '사이드 프로젝트나 오픈소스 기여 등 자기 주도 학습 경험에 대해 설명해주세요.', '성장',
 '[STAR 가이드]\nSituation: 어떤 동기로 시작했는지(업무 부족함, 호기심, 기술 트렌드 등).\nTask: 달성하고자 했던 목표(기술 습득, 서비스 런칭, 오픈소스 기여 등).\nAction: 기획→설계→구현→배포 과정, 시간 관리(주말/퇴근 후 루틴), 기술적 도전과 해결.\nResult: 프로젝트 결과(사용자 수, GitHub 스타 등)와 성장한 역량.\n\n핵심: 완성도보다 "왜 시작했고, 무엇을 배웠는가"의 자기주도성.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 리더십 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀 내에서 아무도 맡지 않으려 하는 역할을 자원한 경험이 있나요?', '팀 내에서 아무도 맡지 않으려 하는 역할을 자원한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 팀에 필요하지만 아무도 자원하지 않은 역할(레거시 담당, 문서화, 배포 관리 등).\nTask: 해당 역할을 맡아 팀에 기여해야 했던 상황.\nAction: 역할의 중요성 인식, 주도적으로 자원, 체계적으로 역할 수행, 팀에 공유.\nResult: 팀 전체에 미친 긍정적 영향과 본인의 성장.\n\n핵심: "책임감 있는 팀원"의 모습. 티나지 않는 기여도 가치 있음을 강조.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀 내에서 지식을 공유하거나 스터디를 주도한 경험이 있나요?', '팀 내에서 지식을 공유하거나 스터디를 주도한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 팀에서 특정 기술/도메인 이해가 부족하거나 개인적으로 쌓은 지식을 나누고 싶었던 배경.\nTask: 팀원들의 역량을 함께 높이거나 특정 문제를 해결하기 위한 학습 공유.\nAction: 스터디/세미나 기획, 커리큘럼 설계, 발표 자료 준비, 팀원 참여 유도.\nResult: 팀 전반의 기술 수준 향상, 이후 코드 품질이나 논의 수준 변화.\n\n핵심: 개인 성장이 아닌 "팀 성장"을 이끈 경험.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀의 개발 프로세스나 컨벤션을 개선하자고 제안한 경험이 있나요?', '팀의 개발 프로세스나 컨벤션을 개선하자고 제안한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 기존 프로세스나 컨벤션에서 비효율이나 문제를 발견한 배경.\nTask: 변경에 대한 팀의 동의를 얻고 실제로 도입해야 하는 과제.\nAction: 문제를 데이터로 정리하여 제안, 팀의 의견 수렴, 파일럿 적용 후 효과 측정.\nResult: 도입 결과(코드 리뷰 시간 단축, 배포 안정성 향상 등)와 이후 프로세스 정착.\n\n핵심: 개인 불편이 아닌 "팀 전체의 이익"으로 제안하는 태도.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '신규 팀원이나 후배의 온보딩을 도운 경험이 있나요?', '신규 팀원이나 후배의 온보딩을 도운 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 새로운 팀원 합류 또는 후배 멘토링 배경.\nTask: 빠르게 적응하고 기여할 수 있도록 돕는 것이 목표.\nAction: 온보딩 문서 정비, 프로젝트 구조/컨벤션 설명, 초기 이슈 배정과 페어 프로그래밍, 정기 1:1 체크인.\nResult: 온보딩 기간 단축, 첫 기여까지의 시간, 멘토링에서 본인이 배운 점.\n\n핵심: 가르치면서 본인도 성장했다는 상호 학습의 가치.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀이 방향을 잃거나 진행이 정체됐을 때 주도적으로 해결한 경험이 있나요?', '팀이 방향을 잃거나 진행이 정체됐을 때 주도적으로 해결한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 팀이 방향을 잡지 못하거나 진행이 정체된 상황.\nTask: 누군가가 주도적으로 정리하고 이끌어야 했던 필요성.\nAction: 문제 분석 후 해결 방향 제안, 역할 분담과 일정 관리 주도, 팀원 의견 수렴하며 합의 도출.\nResult: 프로젝트 성과와 팀 역학 변화, 배운 리더십 원칙.\n\n핵심: "내가 다 했다"가 아닌 "팀이 움직일 수 있도록 촉진했다"는 서번트 리더십.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '기술 부채를 팀에 알리고 개선을 이끈 경험이 있나요?', '기술 부채를 팀에 알리고 개선을 이끈 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 레거시 코드, 테스트 부재, 아키텍처 문제 등 기술 부채를 인지한 배경.\nTask: 기능 개발 일정과 기술 부채 해소 사이의 균형이 필요한 상황.\nAction: 비즈니스 영향(장애 빈도, 개발 속도 저하)을 정량화하여 설득, 점진적 개선 계획 수립.\nResult: 개선 후 효과(배포 시간 단축, 버그 감소, 생산성 향상)와 지속적 품질 관리 프로세스 도입.\n\n핵심: "기술 부채 = 비즈니스 리스크"로 번역하여 비개발자도 납득할 수 있게 설득.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 커뮤니케이션 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '복잡한 기술적 내용을 비개발자에게 설명해야 했던 경험이 있나요?', '복잡한 기술적 내용을 비개발자에게 설명해야 했던 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 기획자/PM/경영진에게 기술적 상황(장애 원인, 구현 불가 사유 등)을 설명해야 했던 상황.\nTask: 상대방이 정확히 이해하고 올바른 의사결정을 할 수 있도록 전달.\nAction: 비유와 시각 자료 활용, 기술 용어를 비즈니스 임팩트로 변환, 핵심 3가지 이내로 정리, 질문 유도.\nResult: 상대방이 이해하고 적절한 의사결정을 내린 결과.\n\n핵심: "쉽게 설명하는 능력 = 깊이 이해하고 있다는 증거".',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '코드 리뷰 의견에 동의하지 않을 때 어떻게 이의를 제기했나요?', '코드 리뷰 의견에 동의하지 않을 때 어떻게 이의를 제기했나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 코드 리뷰에서 리뷰어의 의견이 잘못되었거나 본인의 방식이 더 낫다고 판단한 상황.\nTask: 감정 상하지 않게 본인의 입장을 논리적으로 전달해야 하는 과제.\nAction: 즉각 반박 대신 리뷰어 의도 파악, 본인 방식의 근거(성능 수치, 가독성)를 명확히 제시, 더 나은 방안 함께 탐색.\nResult: 건설적 토론을 통해 도달한 결론과 그 과정에서 배운 점.\n\n핵심: 반론도 "배움의 과정"으로 프레이밍. 존중하면서도 자기 의견을 낼 수 있는 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '새로운 아이디어를 팀에 제안하고 설득한 경험을 이야기해주세요.', '새로운 아이디어를 팀에 제안하고 설득한 경험을 이야기해주세요.', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 본인이 더 나은 방법이라고 생각하는 아이디어를 발견한 배경.\nTask: 팀의 동의를 얻어 실제로 도입해야 하는 과제.\nAction: 아이디어의 가치를 데이터와 근거로 정리, 예상 반론에 대한 답 준비, 소규모 파일럿 제안.\nResult: 팀의 반응(수용/기각)과 도입 후 효과. 기각됐다면 그 과정에서 배운 점.\n\n핵심: 설득에 실패했더라도 "어떻게 논리적으로 제안했는가"가 중요.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '기술 문서(API 문서, 설계 문서 등)를 작성하고 팀에 도움이 된 경험이 있나요?', '기술 문서(에이피아이 문서, 설계 문서 등)를 작성하고 팀에 도움이 된 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 문서 부재로 팀이 어려움을 겪거나 본인이 문서 작성을 맡게 된 배경.\nTask: 팀원들이 쉽게 이해하고 활용할 수 있는 문서 작성.\nAction: 대상 독자 정의, 구조 설계(목차, 예시 코드, 다이어그램), 리뷰 후 개선.\nResult: 문서 활용 효과(온보딩 시간 단축, 문의 감소 등)와 지속 업데이트 방법.\n\n핵심: 문서는 "한 번 쓰고 끝"이 아닌 "살아있는 자산". 최신 상태 유지 방법 언급.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '회의를 진행하거나 팀 논의를 이끈 경험이 있나요?', '회의를 진행하거나 팀 논의를 이끈 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 회의가 주제를 벗어나거나 결론 없이 끝나던 상황, 또는 직접 회의를 진행한 배경.\nTask: 제한된 시간 안에 명확한 결론과 액션 아이템을 도출해야 하는 과제.\nAction: 회의 전 아젠다 공유, 시간 관리, 의견 수렴 후 결론 명시, 회의록 작성 및 공유.\nResult: 회의 효율 개선 결과와 팀의 반응.\n\n핵심: 퍼실리테이션 능력 = 남의 의견도 끌어내고 결론도 이끄는 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:BEHAVIORAL', '팀원에게 건설적인 피드백을 전달해야 했던 경험이 있나요?', '팀원에게 건설적인 피드백을 전달해야 했던 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 팀원의 코드나 행동에서 개선이 필요하다고 느꼈던 구체적 상황.\nTask: 상대방이 방어적이 되지 않도록 하면서도 명확하게 전달해야 하는 과제.\nAction: 구체적 행동 기반 피드백(판단이 아닌 관찰), 긍정적 의도 인정, 개선 방향 함께 제시.\nResult: 상대방의 반응과 이후 변화, 관계에 미친 영향.\n\n핵심: 피드백은 "공격"이 아닌 "선물". SBI 모델(Situation-Behavior-Impact) 활용 언급 가능.',
 'GUIDE', TRUE, NOW());
