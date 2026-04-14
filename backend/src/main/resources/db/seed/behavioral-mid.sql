-- MID BEHAVIORAL 질문 Pool 시딩 (30문항: 협업6, 문제해결6, 성장6, 리더십6, 커뮤니케이션6)
-- referenceType: GUIDE (모범답변이 아닌 STAR 답변 방향 가이드)
-- V20 이후 스키마 기준 (question_order, evaluation_criteria, follow_up_strategy, quality_score 컬럼 없음)

-- ============================================================
-- 협업 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '서로 다른 팀(백엔드/프론트엔드/디자인 등) 사이에서 발생한 갈등을 중재한 경험이 있나요?', '서로 다른 팀(백엔드/프론트엔드/디자인 등) 사이에서 발생한 갈등을 중재한 경험이 있나요?', '협업',
 '[STAR 가이드]\nSituation: 팀 간 책임 경계가 불명확하거나 의견 충돌이 생긴 구체적 상황.\nTask: 팀 간 갈등을 해소하고 프로젝트를 계속 진행시켜야 하는 중재자 역할.\nAction: 각 팀의 입장과 제약 조건을 개별 청취, 공통 목표 재확인, 책임 경계 명문화, 단계적 합의 도출.\nResult: 갈등 해소 후 협업 방식의 변화와 프로젝트 결과.\n\n핵심: 어느 한쪽 편이 아닌 "팀 전체의 이익"을 위한 중립적 중재 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '주니어 개발자를 멘토링하면서 어려웠던 점과 극복 방법을 이야기해주세요.', '주니어 개발자를 멘토링하면서 어려웠던 점과 극복 방법을 이야기해주세요.', '협업',
 '[STAR 가이드]\nSituation: 멘토링을 맡게 된 배경과 멘티의 현재 수준.\nTask: 멘티가 빠르게 성장하고 팀에 기여할 수 있도록 이끄는 역할.\nAction: 멘티의 학습 스타일과 목표 파악, 맞춤형 학습 계획 제시, 정기 1:1 미팅, 코드 리뷰를 통한 피드백.\nResult: 멘티의 성장 지표(첫 기여, 자립적 문제 해결)와 멘토링에서 본인이 배운 점.\n\n핵심: 멘토링은 "가르치는 것"이 아닌 "스스로 생각하게 만드는 것". 소크라테스식 질문법 언급 가능.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '외부 팀이나 벤더와 협업하면서 발생한 문제를 해결한 경험이 있나요?', '외부 팀이나 벤더와 협업하면서 발생한 문제를 해결한 경험이 있나요?', '협업',
 '[STAR 가이드]\nSituation: 외부 팀/벤더의 지연, 품질 문제, 소통 부재 등이 발생한 상황.\nTask: 내부 일정에 영향 없이 외부 의존성 문제를 해결해야 하는 과제.\nAction: 문제를 구체적으로 기록하여 에스컬레이션, 대안 플랜(내재화, 다른 벤더) 병행 준비, 계약/SLA 근거 제시.\nResult: 문제 해결 결과와 이후 외부 협업 프로세스 개선.\n\n핵심: 외부 의존성은 "통제 불가"가 아님. 리스크 관리와 대안 준비 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '여러 팀의 이해관계가 충돌하는 프로젝트에서 어떻게 합의를 이끌었나요?', '여러 팀의 이해관계가 충돌하는 프로젝트에서 어떻게 합의를 이끌었나요?', '협업',
 '[STAR 가이드]\nSituation: 팀마다 우선순위와 목표가 달라 합의점을 찾기 어려웠던 프로젝트.\nTask: 모든 팀이 납득할 수 있는 방향을 도출하고 진행시켜야 하는 상황.\nAction: 각 팀의 핵심 요구(must-have)와 양보 가능 부분(nice-to-have) 파악, 데이터 기반 우선순위 결정, 단계별 로드맵 제시.\nResult: 합의에 도달한 과정과 이후 프로젝트 진행 효율.\n\n핵심: 정치적 처리가 아닌 "데이터와 원칙"에 기반한 합의 도출.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '팀 문화나 업무 방식을 개선하기 위해 주도적으로 행동한 경험이 있나요?', '팀 문화나 업무 방식을 개선하기 위해 주도적으로 행동한 경험이 있나요?', '협업',
 '[STAR 가이드]\nSituation: 팀의 비효율적인 관행이나 부정적인 문화를 발견한 배경.\nTask: 개인의 불편이 아닌 팀 전체의 생산성과 분위기 개선이 목표.\nAction: 문제를 데이터로 정리하여 팀에 제안, 작은 실험으로 효과 입증, 점진적 도입.\nResult: 팀 문화 변화의 구체적 지표(회의 시간 단축, 배포 빈도 증가, 만족도 향상).\n\n핵심: 불평꾼이 아닌 "문제 해결사". 팀원들을 설득하고 함께 변화를 만든 경험.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '코드 리뷰 문화를 팀에 정착시키거나 개선한 경험이 있나요?', '코드 리뷰 문화를 팀에 정착시키거나 개선한 경험이 있나요?', '협업',
 '[STAR 가이드]\nSituation: 코드 리뷰가 형식적이거나 없는 팀에서 이를 개선하려 했던 배경.\nTask: 리뷰 문화를 팀에 자연스럽게 정착시키는 과제.\nAction: 리뷰 가이드라인 작성, 리뷰 체크리스트 도입, 긍정적 리뷰 문화 조성(칭찬 우선), 리뷰 시간 SLA 설정.\nResult: 리뷰 도입 후 코드 품질 향상, 버그 감소, 팀원 만족도 변화.\n\n핵심: 코드 리뷰는 "감시"가 아닌 "집단 지성". 심리적 안전감이 핵심.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 문제해결 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '프로덕션 장애를 대응한 경험을 단계별로 설명해주세요.', '프로덕션 장애를 대응한 경험을 단계별로 설명해주세요.', '문제해결',
 '[STAR 가이드]\nSituation: 어떤 서비스에서 어떤 규모의 장애가 발생했는지(영향 사용자 수, 매출 손실 등).\nTask: 최단 시간 내 서비스 복구와 근본 원인 해결.\nAction: 인시던트 선언 → 임시 조치(롤백, 트래픽 차단) → 원인 분석 → 영구 수정 → 포스트모템 작성. 커뮤니케이션 채널 관리.\nResult: 복구 시간(MTTR), 이후 동일 장애 재발 방지 지표.\n\n핵심: 장애 대응은 "기술 능력"만이 아닌 "커뮤니케이션과 리더십"도 포함됨을 보여줄 것.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '기술 부채가 누적된 시스템을 점진적으로 개선한 경험이 있나요?', '기술 부채가 누적된 시스템을 점진적으로 개선한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 기술 부채의 종류와 규모, 비즈니스에 미치는 영향.\nTask: 서비스 중단 없이 점진적으로 시스템을 개선해야 하는 과제.\nAction: 기술 부채 인벤토리 작성, 비용-효과 기반 우선순위 결정, 스트랭글러 패턴/피처 토글 활용, 마이그레이션 전략.\nResult: 개선 후 측정 가능한 효과(빌드 시간 단축, 배포 빈도 증가, 장애 감소).\n\n핵심: "전면 재작성"은 대부분 실패. "점진적 교체"의 전략과 실행력 어필.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '성능 병목을 찾아 시스템을 최적화한 경험을 구체적으로 설명해주세요.', '성능 병목을 찾아 시스템을 최적화한 경험을 구체적으로 설명해주세요.', '문제해결',
 '[STAR 가이드]\nSituation: 어떤 서비스에서 성능 문제가 발생했고 어떻게 인지했는지(사용자 불만, 모니터링 알림).\nTask: 성능 목표(응답 시간, TPS)와 현재 상태의 격차를 수치로 제시.\nAction: APM/프로파일링으로 병목 식별, 원인 분석(N+1 쿼리, 락 경합, 메모리 누수), 개선 → 측정 → 반복.\nResult: 개선 수치(예: P99 레이턴시 5초→300ms)와 적용한 최적화 패턴.\n\n핵심: "감으로 고쳤다"가 아닌 "측정 → 분석 → 개선 → 검증"의 엔지니어링 접근.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '여러 팀에 영향을 미치는 아키텍처 변경을 주도한 경험이 있나요?', '여러 팀에 영향을 미치는 아키텍처 변경을 주도한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 기존 아키텍처의 한계(확장성, 유지보수성)를 발견한 배경.\nTask: 여러 팀과 협의하며 호환성을 유지한 채 아키텍처를 변경하는 복잡한 과제.\nAction: RFC(Request for Comments) 문서 작성, 팀별 영향 범위 분석, 마이그레이션 플랜 단계화, 각 팀 담당자와 정기 싱크.\nResult: 변경 완료 후 아키텍처 개선 효과와 팀 간 협업 프로세스 변화.\n\n핵심: 기술 결정은 "설득과 합의"의 과정. ADR 작성 경험 언급 가능.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '데이터 마이그레이션이나 시스템 전환 중 발생한 문제를 해결한 경험이 있나요?', '데이터 마이그레이션이나 시스템 전환 중 발생한 문제를 해결한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 데이터 마이그레이션 또는 시스템 전환의 배경과 규모(데이터 크기, 영향 범위).\nTask: 무중단 또는 최소 중단으로 안전하게 전환해야 하는 과제.\nAction: 단계적 마이그레이션 전략(Blue-Green, 카나리 배포), 롤백 플랜 수립, 데이터 검증 자동화, 예상치 못한 문제 대응.\nResult: 전환 완료 지표와 발생한 문제 및 해결 과정.\n\n핵심: "잘 계획된 마이그레이션"보다 "계획이 틀렸을 때의 대응력"이 핵심.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '보안 취약점을 발견하고 대응한 경험이 있나요?', '보안 취약점을 발견하고 대응한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 어떤 경로로(코드 리뷰, 침투 테스트, 버그 바운티) 취약점을 발견했는지.\nTask: 취약점의 심각도 평가와 빠른 패치 적용.\nAction: 취약점 범위 파악 → 임시 완화 조치 → 근본 수정 → 검증 → 보안 검토 프로세스 강화.\nResult: 취약점 해결 결과와 이후 보안 강화 지표(SAST 도입, 코드 리뷰 보안 체크리스트 등).\n\n핵심: 보안 사고는 "기술 문제"이자 "신뢰 문제". 투명한 커뮤니케이션과 빠른 대응 강조.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 성장 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '주니어에서 미드레벨로 성장하면서 가장 크게 바뀐 점은 무엇인가요?', '주니어에서 미드레벨로 성장하면서 가장 크게 바뀐 점은 무엇인가요?', '성장',
 '[STAR 가이드]\nSituation: 주니어 시절의 본인과 현재를 비교할 수 있는 구체적 사례.\nTask: 성장의 변곡점이 된 경험 또는 인식의 전환.\nAction: 기술적 성장(코드 품질, 설계 능력)뿐 아니라 협업, 커뮤니케이션, 비즈니스 이해도의 변화.\nResult: 현재 스스로 평가하는 강점과 앞으로 채워야 할 영역.\n\n핵심: "기술 숙련도"만이 아닌 "오너십과 영향력 범위 확대"가 성장의 핵심.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '기술적으로 잘못된 결정을 내렸다가 수정한 경험이 있나요?', '기술적으로 잘못된 결정을 내렸다가 수정한 경험이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 어떤 기술 결정이 나중에 문제가 됐는지, 얼마나 시간이 지나서 인식했는지.\nTask: 잘못된 결정의 영향을 최소화하고 올바른 방향으로 전환.\nAction: 문제 인정 → 영향 범위 파악 → 수정 전략 수립 → 팀과 투명하게 공유 → 실행.\nResult: 수정 후 개선된 상황과 이후 기술 결정 프로세스의 변화.\n\n핵심: 실수 인정을 두려워하지 않는 용기 + 빠른 수정 능력. "사후 확증 편향" 없이 냉정하게 분석.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '도메인 지식이 부족한 상황에서 복잡한 비즈니스 요구사항을 이해하고 구현한 경험이 있나요?', '도메인 지식이 부족한 상황에서 복잡한 비즈니스 요구사항을 이해하고 구현한 경험이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 금융, 물류, 의료 등 생소한 도메인의 복잡한 비즈니스 규칙을 다루게 된 배경.\nTask: 도메인 전문가와 협력하여 요구사항을 정확히 구현해야 하는 과제.\nAction: 도메인 전문가 인터뷰 구조화, 유비쿼터스 언어 정의, 비즈니스 규칙을 테스트로 문서화, 잦은 검증 사이클.\nResult: 도메인 지식 습득 과정과 구현의 정확도, 이후 DDD 적용 경험.\n\n핵심: 기술 구현보다 "비즈니스 이해"가 선행되어야 함을 인식하는 성숙도.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '기술 트렌드를 학습하고 실무에 적용한 판단 기준이 있나요?', '기술 트렌드를 학습하고 실무에 적용한 판단 기준이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 새로운 기술(Kubernetes, GraphQL, Event Sourcing 등)을 접하고 도입 여부를 결정한 상황.\nTask: 기술의 유행이 아닌 팀과 서비스에 맞는 합리적 도입 판단.\nAction: 기술 검토 기준 수립(팀 역량, 마이그레이션 비용, 생태계 성숙도, 실제 해결 문제), PoC 진행, 도입/보류 결정의 근거 문서화.\nResult: 도입한 기술의 효과 또는 보류 결정의 근거와 결과.\n\n핵심: "신기술은 무조건 좋다"는 생각 탈피. "우리 문제를 해결하는가"가 판단 기준.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '팀 전체의 개발 생산성을 높이기 위해 기여한 경험이 있나요?', '팀 전체의 개발 생산성을 높이기 위해 기여한 경험이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 팀의 반복 작업, 수동 프로세스, 환경 셋업 비용 등 생산성 저해 요인 발견.\nTask: 팀 전체의 생산성을 높이는 인프라/도구/프로세스 개선.\nAction: 자동화 스크립트, CI/CD 파이프라인 개선, 공통 라이브러리 추출, 개발 환경 표준화.\nResult: 팀 생산성 향상 지표(배포 시간 단축, 환경 셋업 시간 감소, 반복 이슈 제거).\n\n핵심: "내 코드"가 아닌 "팀 전체의 효율"에 기여하는 플랫폼 엔지니어링 마인드.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '커리어에서 가장 어려운 결정을 내린 경험이 있나요?', '커리어에서 가장 어려운 결정을 내린 경험이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 단순한 기술 선택이 아닌 커리어 방향(이직, 팀 이동, 역할 변경)에 관한 결정의 배경.\nTask: 불확실성 속에서 장기적 성장을 위한 올바른 선택.\nAction: 의사결정 기준 수립(기술 성장 기회, 팀 문화, 비즈니스 도메인), 멘토와 상의, 리스크와 기회 분석.\nResult: 결정 후 결과와 그 선택에서 배운 점.\n\n핵심: 결정의 옳고 그름보다 "어떤 근거로 결정했는가"의 사고 과정이 중요.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 리더십 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '경영진이나 PM을 설득하여 기술적 결정을 관철한 경험이 있나요?', '경영진이나 PM을 설득하여 기술적 결정을 관철한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 기술적으로 중요하지만 비즈니스 관점에서 우선순위가 낮게 인식된 작업.\nTask: 비개발자 의사결정자를 설득하여 기술 투자를 승인받아야 하는 과제.\nAction: 기술 문제를 비즈니스 리스크/비용으로 번역, ROI 계산, 리스크 시나리오 제시, 단계적 접근 제안.\nResult: 설득 성공 여부와 결과. 실패했다면 그로부터 배운 설득 방법론.\n\n핵심: 기술 언어가 아닌 "비즈니스 언어"로 말할 수 있는 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '팀의 기술 방향성을 제시하거나 기술 로드맵을 수립한 경험이 있나요?', '팀의 기술 방향성을 제시하거나 기술 로드맵을 수립한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 팀의 기술 방향이 불명확하거나 중장기 전략이 없던 상황.\nTask: 팀의 비즈니스 목표와 기술 역량을 연결하는 로드맵 수립.\nAction: 현재 상태 진단(기술 스택, 역량, 부채), 목표 상태 정의, 단계별 이행 계획, 팀 합의 도출.\nResult: 로드맵 수립 후 팀의 방향 정렬과 실행 결과.\n\n핵심: 로드맵은 "계획표"가 아닌 "공유된 방향". 팀원들의 오너십을 이끌어낸 과정 강조.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '팀원의 성과가 기대에 미치지 못할 때 어떻게 대처했나요?', '팀원의 성과가 기대에 미치지 못할 때 어떻게 대처했나요?', '리더십',
 '[STAR 가이드]\nSituation: 팀원의 성과 저하를 인지한 배경(마감 미준수, 코드 품질 저하, 소극적 참여).\nTask: 팀원을 비난하지 않고 성과를 개선시켜야 하는 과제.\nAction: 1:1 면담으로 원인 파악(번아웃, 불명확한 기대치, 개인 사정), 명확한 기대치 재설정, 지원과 피드백 제공.\nResult: 팀원의 변화와 이후 관계 및 성과.\n\n핵심: "평가"가 아닌 "개선 지원". 원인 없는 성과 저하는 없음을 인식하는 공감 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '테크 리드 또는 리드 개발자로서 팀의 기술적 결정을 이끈 경험이 있나요?', '테크 리드 또는 리드 개발자로서 팀의 기술적 결정을 이끈 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 기술적 결정이 필요한 상황에서 리드 역할을 맡게 된 배경.\nTask: 팀원들의 의견을 수렴하되 최종 결정을 내려야 하는 책임.\nAction: RFC 프로세스 도입, 각 옵션의 트레이드오프 명확화, 팀 투표보다는 합리적 기준에 따른 결정, 결정 이후 팀 정렬.\nResult: 기술 결정의 결과와 팀 의사결정 문화 변화.\n\n핵심: 좋은 리드는 "모든 것을 결정"하지 않고 "팀이 좋은 결정을 내릴 수 있게" 함.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '스프린트나 프로젝트 일정이 위기에 처했을 때 팀을 이끈 경험이 있나요?', '스프린트나 프로젝트 일정이 위기에 처했을 때 팀을 이끈 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 일정 위기의 원인(예상 밖 복잡도, 팀원 이탈, 요구사항 변경).\nTask: 팀의 사기를 유지하면서 최선의 결과를 내야 하는 상황.\nAction: 현실적 상황 투명 공유, 우선순위 재조정, 팀원 강점 기반 역할 재배치, 작은 승리 만들기.\nResult: 최종 결과(일부 조정된 범위, 일정 내 핵심 기능 배포 등)와 팀의 회복력.\n\n핵심: 위기 상황에서 "낙관적 투명성"과 "현실적 계획"의 균형.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '조직 내 변화(새 프로세스, 도구 도입)를 이끌면서 저항을 극복한 경험이 있나요?', '조직 내 변화(새 프로세스, 도구 도입)를 이끌면서 저항을 극복한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 새 프로세스나 도구 도입에 대한 팀의 저항이 있었던 상황.\nTask: 변화의 필요성을 팀에 납득시키고 실제로 정착시키는 과제.\nAction: 저항의 근본 원인 파악(불편함, 학습 비용, 가치 의심), 얼리 어답터 확보, 성공 사례 만들기, 점진적 도입.\nResult: 변화 정착 지표와 팀 반응의 변화.\n\n핵심: 변화 관리는 "기술 도입"이 아닌 "사람 관리". 저항을 적이 아닌 피드백으로 받아들이는 태도.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 커뮤니케이션 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '이해관계자(PM, 비즈니스 팀)에게 기술적 제약을 설득력 있게 전달한 경험이 있나요?', '이해관계자(PM, 비즈니스 팀)에게 기술적 제약을 설득력 있게 전달한 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 기술적으로 불가능하거나 위험한 요구사항을 받은 상황.\nTask: "안 된다"가 아닌 "대안"을 제시하면서 이해관계자를 납득시키는 과제.\nAction: 제약의 원인을 비기술적 언어로 설명, 리스크와 비용 정량화, 대안 시나리오 3가지 제시, 의사결정 위임.\nResult: 이해관계자의 반응과 최종 결정, 신뢰 관계 변화.\n\n핵심: 기술자의 한계가 아닌 "함께 최선을 찾는 파트너"로 인식되는 커뮤니케이션.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '기술 부채나 아키텍처 개선의 중요성을 비개발 의사결정자에게 설명한 경험이 있나요?', '기술 부채나 아키텍처 개선의 중요성을 비개발 의사결정자에게 설명한 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 기술 부채가 비즈니스 속도에 영향을 미치지만 경영진이 중요성을 인식하지 못하는 상황.\nTask: 기술 내부 문제를 비즈니스 문제로 번역하여 투자 승인 받기.\nAction: 기술 부채를 "이자 비용"으로 비유, 장애 빈도/개발 속도 저하 데이터 제시, 개선 ROI 계산.\nResult: 승인 여부와 이후 프로세스 변화.\n\n핵심: 추상적인 기술 개념을 구체적인 비용과 리스크로 변환하는 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '팀 간 정보 사일로(silos)를 해소하기 위해 어떤 노력을 했나요?', '팀 간 정보 사일로(silos)를 해소하기 위해 어떤 노력을 했나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 팀들이 각자 정보를 쌓아두고 공유하지 않아 중복 작업이나 협업 실패가 생긴 상황.\nTask: 팀 간 지식 공유와 협업 채널을 만드는 과제.\nAction: 팀 간 정기 싱크 미팅 제안, 공통 Confluence/Notion 공간 구성, API 문서 공유 표준 수립.\nResult: 정보 공유 개선 후 중복 작업 감소, 협업 속도 향상.\n\n핵심: 사일로는 "사람 문제"가 아닌 "구조 문제". 구조를 바꾸는 접근 방식 강조.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '어려운 피드백(성과 평가, 부정적 리뷰)을 팀원에게 전달한 경험이 있나요?', '어려운 피드백(성과 평가, 부정적 리뷰)을 팀원에게 전달한 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 팀원에게 긍정적이지 않은 피드백을 전달해야 했던 배경.\nTask: 관계를 해치지 않으면서도 명확하고 건설적으로 전달하는 과제.\nAction: 적절한 타이밍과 장소 선택(1:1, 조용한 환경), 구체적 행동 기반 피드백, 개선 방향 함께 설정, 후속 지원.\nResult: 팀원의 반응과 이후 변화, 관계의 변화.\n\n핵심: 어려운 피드백을 회피하지 않는 용기 + 전달하는 기술. 라디컬 캔도(Radical Candor) 원칙 적용 가능.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '프로젝트 리스크를 사전에 파악하고 이해관계자에게 투명하게 공유한 경험이 있나요?', '프로젝트 리스크를 사전에 파악하고 이해관계자에게 투명하게 공유한 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 프로젝트 진행 중 일정, 기술, 인력 리스크를 조기에 발견한 상황.\nTask: 이해관계자에게 나쁜 소식을 미리 전달하고 대응 방안을 함께 논의하는 과제.\nAction: 리스크 명확화(발생 가능성 × 영향도), 대응 방안 3가지와 각각의 트레이드오프 제시, 의사결정 요청.\nResult: 이해관계자의 반응과 채택된 대응 방안, 실제 리스크 발생 여부.\n\n핵심: "나쁜 소식은 빨리"가 원칙. 문제 인식보다 "솔루션을 가져오는 프레이밍".',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:BEHAVIORAL', '크로스펑셔널 팀(기획/디자인/마케팅/개발)에서 개발 입장을 효과적으로 대표한 경험이 있나요?', '크로스펑셔널 팀(기획/디자인/마케팅/개발)에서 개발 입장을 효과적으로 대표한 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 개발 외 직군이 주도하는 회의나 프로젝트에서 기술 대표자로 참여한 상황.\nTask: 개발 관점의 제약과 가능성을 다른 직군에게 명확히 전달하는 역할.\nAction: 기술 용어 없이 비즈니스 임팩트 중심으로 설명, 빠른 프로토타입으로 가능성 증명, 불가능한 것보다 대안 제시.\nResult: 팀의 최종 결정과 개발 팀의 기여 방식에 미친 영향.\n\n핵심: 개발자는 "실행자"가 아닌 "전략적 파트너"로 참여하는 태도.',
 'GUIDE', TRUE, NOW());
