-- SENIOR BEHAVIORAL 질문 Pool 시딩 (30문항: 협업6, 문제해결6, 성장6, 리더십6, 커뮤니케이션6)
-- referenceType: GUIDE (모범답변이 아닌 STAR 답변 방향 가이드)
-- V20 이후 스키마 기준 (question_order, evaluation_criteria, follow_up_strategy, quality_score 컬럼 없음)

-- ============================================================
-- 협업 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '조직 전체의 기술 협업 방식을 바꾼 경험이 있나요?', '조직 전체의 기술 협업 방식을 바꾼 경험이 있나요?', '협업',
 '[STAR 가이드]\nSituation: 여러 팀이 각자 다른 방식으로 작업해 통합/협업 비용이 높았던 조직 수준의 문제.\nTask: 조직 전반의 협업 방식을 표준화하거나 개선하는 대규모 변화 추진.\nAction: 현상 진단(설문, 인터뷰), 표준안 수립과 합의 도출, 파일럿 팀 운영, 단계적 확산, 피드백 루프 운영.\nResult: 조직 단위의 개선 지표(배포 빈도, 팀 간 의존성 감소, 개발자 만족도).\n\n핵심: 조직 변화는 "도구" 이전에 "문화". 강요가 아닌 "매력적인 표준"을 만드는 접근.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '글로벌 또는 분산 팀과 협업하며 시너지를 만든 경험을 이야기해주세요.', '글로벌 또는 분산 팀과 협업하며 시너지를 만든 경험을 이야기해주세요.', '협업',
 '[STAR 가이드]\nSituation: 시간대, 언어, 문화가 다른 글로벌 팀과 함께 프로젝트를 진행한 배경.\nTask: 물리적/문화적 거리에도 불구하고 팀 생산성과 품질을 유지하는 과제.\nAction: 문화적 차이 이해(직접/간접 소통 방식), 비동기 협업 프로세스 구축, 오버랩 시간대 최대 활용, 심리적 안전감 조성.\nResult: 글로벌 팀의 협업 성과와 이후 분산 팀 운영 원칙.\n\n핵심: 글로벌 협업은 "언어 문제"가 아닌 "신뢰와 명확성" 문제. 문화 지능(CQ) 언급 가능.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '조직에서 심리적 안전감을 만들기 위해 어떤 노력을 했나요?', '조직에서 심리적 안전감을 만들기 위해 어떤 노력을 했나요?', '협업',
 '[STAR 가이드]\nSituation: 팀원들이 의견을 자유롭게 표현하지 못하거나 실수를 숨기는 문화가 있던 상황.\nTask: 두려움 없이 의견을 내고 실수에서 배울 수 있는 환경 조성.\nAction: 리더로서 본인의 실수를 공개적으로 공유, 비난 없는 포스트모템 문화 도입, 잘못된 의견도 감사히 받는 행동.\nResult: 팀의 발언 빈도, 이슈 조기 발견율, 혁신 시도 횟수 등 측정 가능한 변화.\n\n핵심: 심리적 안전감은 "말로"가 아닌 "행동으로" 만들어짐. 리더의 취약성 표현(Vulnerability) 중요.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '여러 프로젝트 팀에 걸친 의존성 문제를 해결한 경험이 있나요?', '여러 프로젝트 팀에 걸친 의존성 문제를 해결한 경험이 있나요?', '협업',
 '[STAR 가이드]\nSituation: 여러 팀의 작업이 서로 얽혀 병목이 생기거나 충돌이 발생한 상황.\nTask: 팀 간 의존성을 관리하고 전체 흐름이 원활하게 진행되도록 하는 조율자 역할.\nAction: 의존성 맵 시각화, 크리티컬 패스 파악, 팀 간 인터페이스 계약 명확화, 주기적 의존성 리뷰 미팅 운영.\nResult: 의존성 충돌 감소, 릴리스 지연 단축, 팀 간 신뢰 향상.\n\n핵심: 복잡한 의존성을 단순화하는 아키텍처 사고와 팀 간 조율 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '조직에서 지식 공유 시스템이나 문화를 구축한 경험이 있나요?', '조직에서 지식 공유 시스템이나 문화를 구축한 경험이 있나요?', '협업',
 '[STAR 가이드]\nSituation: 개인에게 지식이 집중되어 버스 팩터가 높거나 온보딩 비용이 큰 조직 상황.\nTask: 지식을 조직의 자산으로 체계화하는 구조 설계.\nAction: 위키 체계 설계, ADR(Architecture Decision Record) 프로세스 도입, 기술 토크 정기화, 런치&런 세션 운영.\nResult: 온보딩 시간 단축, 버스 팩터 개선, 지식 베이스 활용률.\n\n핵심: "지식 = 권력"이 아닌 "지식 = 공유 자산" 문화로의 전환.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '엔지니어링 조직과 비즈니스 조직 간 신뢰를 구축한 경험이 있나요?', '엔지니어링 조직과 비즈니스 조직 간 신뢰를 구축한 경험이 있나요?', '협업',
 '[STAR 가이드]\nSituation: 엔지니어링 팀이 "비용 센터"로 인식되거나 비즈니스 팀과 신뢰가 낮은 상황.\nTask: 기술 조직의 비즈니스 기여를 가시화하고 파트너십 구축.\nAction: 기술 작업을 비즈니스 지표와 연결, 정기적 비즈니스 성과 공유, 비즈니스 팀 스프린트 리뷰 참여 유도, 빠른 실험 문화 도입.\nResult: 조직 간 신뢰 지표와 협업 패턴 변화.\n\n핵심: "우리는 다 다르다"가 아닌 "같은 목표를 다른 방식으로 추구"하는 인식 전환.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 문제해결 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '대규모 시스템 장애를 총괄하여 해결한 경험을 설명해주세요.', '대규모 시스템 장애를 총괄하여 해결한 경험을 설명해주세요.', '문제해결',
 '[STAR 가이드]\nSituation: 전사 서비스에 영향을 미친 대규모 장애의 배경과 규모(영향 사용자, 매출 손실, 지속 시간).\nTask: 기술 복구뿐 아니라 조직 전체의 대응을 조율하는 Incident Commander 역할.\nAction: 인시던트 레벨 선언, 역할 분담(조사/수정/커뮤니케이션), 30분 단위 상황 공유, 임시 조치 vs 영구 수정 병행, 포스트모템 주도.\nResult: MTTR, 포스트모템 액션 아이템 이행률, 동일 장애 재발 여부.\n\n핵심: 시니어의 장애 대응은 "혼자 고치는 것"이 아닌 "조직을 조율하고 커뮤니케이션하는 것".',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '조직의 기술 전략 방향을 수립하고 실행한 경험이 있나요?', '조직의 기술 전략 방향을 수립하고 실행한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 조직의 현재 기술 스택이나 아키텍처가 비즈니스 성장을 따라가지 못하는 상황.\nTask: 3~5년 기술 비전을 수립하고 현재와의 갭을 메우는 로드맵 설계.\nAction: 기술 부채 인벤토리, 시장/경쟁사 분석, 기술 트렌드 평가, C레벨 설득, 단계별 마이그레이션 플랜.\nResult: 전략 채택 여부와 실행 단계별 성과.\n\n핵심: 기술 전략은 "최신 기술 도입"이 아닌 "비즈니스 목표 달성을 위한 기술 투자 우선순위".',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '기존 모놀리식 시스템을 마이크로서비스로 전환한 경험이 있나요?', '기존 모놀리식 시스템을 마이크로서비스로 전환한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 모놀리스의 한계(배포 속도, 팀 자율성, 확장성)가 비즈니스에 영향을 미치기 시작한 배경.\nTask: 서비스 중단 없이 대규모 아키텍처를 전환하는 복잡한 과제.\nAction: 스트랭글러 피그 패턴 적용, 서비스 경계 설계(DDD 기반), 팀 토폴로지 변경, 관찰 가능성(Observability) 구축, 단계별 전환.\nResult: 전환 후 배포 빈도, 팀 자율성, 장애 격리 효과.\n\n핵심: "마이크로서비스 = 무조건 좋다"가 아님. 트레이드오프를 알고 선택한 근거 강조.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '복잡한 분산 시스템에서 발생한 간헐적 장애를 추적하고 해결한 경험이 있나요?', '복잡한 분산 시스템에서 발생한 간헐적 장애를 추적하고 해결한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 재현이 어렵고 특정 조건에서만 발생하는 분산 시스템의 장애 패턴.\nTask: 여러 서비스에 걸친 원인을 추적하여 근본 해결하는 과제.\nAction: 분산 트레이싱(Jaeger, Zipkin) 활용, 카오스 엔지니어링으로 재현 시도, 시계열 로그 상관 분석, 서비스 간 타임아웃/재시도 정책 검토.\nResult: 근본 원인 발견과 해결, 분산 시스템 관찰 가능성 개선.\n\n핵심: 분산 시스템 문제는 "어디서"가 아닌 "어떤 조합에서" 발생하는지 파악하는 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '기술 부채와 기능 개발 사이의 균형을 조직 수준에서 관리한 경험이 있나요?', '기술 부채와 기능 개발 사이의 균형을 조직 수준에서 관리한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: 기능 개발 압박이 심해 기술 부채가 누적되어 개발 속도가 저하되는 악순환.\nTask: 비즈니스 요구와 기술 건전성을 동시에 만족시키는 균형 메커니즘 설계.\nAction: 기술 부채 가시화(대시보드), "20% 부채 해소 시간" 제도화, 주요 기능마다 리팩토링 연계, 기술 부채 비용을 스프린트 추정에 포함.\nResult: 기술 부채 감소 추세와 개발 속도(사이클 타임) 변화.\n\n핵심: 조직 수준의 기술 부채 관리는 "허락 구하기"가 아닌 "측정하고 가시화하기"에서 시작.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '고가용성(HA) 또는 재해 복구(DR) 체계를 구축한 경험이 있나요?', '고가용성(HA) 또는 재해 복구(DR) 체계를 구축한 경험이 있나요?', '문제해결',
 '[STAR 가이드]\nSituation: SLA 요구사항(99.99% 가용성 등)과 현재 시스템 신뢰성 간 격차 발견.\nTask: 비용과 복잡도를 고려한 현실적인 HA/DR 전략 수립.\nAction: RTO/RPO 정의, 장애 시나리오 매핑(SPOF 분석), 멀티 리전 배포 또는 Active-Passive 구성, GameDay(장애 훈련) 운영.\nResult: 가용성 지표 달성 여부와 실제 장애 시 복구 시간.\n\n핵심: HA/DR은 "기술 구성"뿐 아니라 "정기 훈련"이 있어야 실효성이 있음.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 성장 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '시니어 엔지니어로서 조직에 기술 문화를 만들거나 변화시킨 경험이 있나요?', '시니어 엔지니어로서 조직에 기술 문화를 만들거나 변화시킨 경험이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 기술 문화가 약하거나 잘못된 관행이 퍼져 있던 조직에 합류하거나 문제를 인식한 배경.\nTask: 코드 품질, 테스트 문화, 배포 안정성 등 기술 문화를 변화시키는 장기 목표.\nAction: 현상 진단 → 비전 공유 → 얼리 어답터 확보 → 성공 사례 만들기 → 조직 전파. 규칙보다 "왜"를 먼저 설명.\nResult: 문화 변화의 정성/정량 지표와 소요 시간.\n\n핵심: 문화 변화는 "규정"이 아닌 "모델링". 시니어 스스로가 모범 사례가 되어야 함.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '기술 리더로 성장하면서 가장 힘들었던 점은 무엇이고 어떻게 극복했나요?', '기술 리더로 성장하면서 가장 힘들었던 점은 무엇이고 어떻게 극복했나요?', '성장',
 '[STAR 가이드]\nSituation: 개인 기여자에서 기술 리더로 전환하면서 맞닥뜨린 어려움(코딩 시간 감소, 모호한 영향력, 팀 성과 책임).\nTask: 역할 전환의 불안과 어려움을 극복하며 새 역할에서 효과적이 되는 과제.\nAction: 리더십 멘토 확보, 새 역할의 성공 기준 재정의(나의 코드 → 팀의 성과), 위임 연습, 정기적 자기 점검.\nResult: 리더로서 현재의 강점과 여전히 성장 중인 영역.\n\n핵심: 좋은 리더는 "이미 완성된 사람"이 아닌 "지속적으로 배우는 사람"임을 보여줄 것.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '기술 분야 이외의 영역(비즈니스, 조직 관리)을 배우고 성장한 경험이 있나요?', '기술 분야 이외의 영역(비즈니스, 조직 관리)을 배우고 성장한 경험이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 순수 기술 외에 비즈니스 전략, 재무, 조직 심리 등을 학습하게 된 계기.\nTask: 기술과 비기술 영역을 통합적으로 이해하여 더 넓은 영향력을 발휘하는 목표.\nAction: 비즈니스 지표 학습(LTV, CAC, 공헌이익), 경영진 회의 참관, 비즈니스 케이스 작성 연습, MBA 부분 학습.\nResult: 비기술 지식이 기술 결정에 영향을 미친 구체적 사례.\n\n핵심: 시니어 엔지니어의 성장은 "더 깊은 기술"이 아닌 "더 넓은 맥락 이해"로 확장.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '조직 내 다른 시니어 엔지니어와 기술 견해 차이를 극복하고 협력한 경험이 있나요?', '조직 내 다른 시니어 엔지니어와 기술 견해 차이를 극복하고 협력한 경험이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 동급 시니어와 기술 비전 또는 접근 방식에서 큰 차이가 있었던 상황.\nTask: 서로의 관점을 존중하면서도 조직에 최선인 결정을 내리는 과제.\nAction: 논쟁보다 실험(PoC)으로 검증, 각자의 전제 조건 명확화, 제3의 시니어 관점 요청, 결정 후 팀 정렬.\nResult: 합의에 이른 방식과 이후 관계 및 기술 방향.\n\n핵심: 자기 확신이 강할수록 "열린 마음"이 더 중요. 증거에 의한 설득 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '실패한 대형 프로젝트에서 조직이 배우게 한 경험이 있나요?', '실패한 대형 프로젝트에서 조직이 배우게 한 경험이 있나요?', '성장',
 '[STAR 가이드]\nSituation: 수개월 투자한 프로젝트가 중단되거나 큰 실패로 끝난 배경.\nTask: 실패를 비난 대신 조직 학습의 기회로 전환하는 과제.\nAction: 비난 없는 포스트모템(Blameless Post-Mortem) 주도, 시스템 관점의 원인 분석(개인 실수보다 프로세스/구조), 액션 아이템 도출과 책임자 지정, 공개적 공유.\nResult: 포스트모템 이후 조직 프로세스 변화와 유사 실패 감소.\n\n핵심: 실패를 숨기는 문화 vs 배우는 문화의 차이를 만드는 리더십.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '커리어 전반에 걸쳐 기술 외적으로 가장 크게 성장한 영역은 무엇인가요?', '커리어 전반에 걸쳐 기술 외적으로 가장 크게 성장한 영역은 무엇인가요?', '성장',
 '[STAR 가이드]\nSituation: 기술적 역량이 충분히 성숙한 이후 비기술 영역에서 성장이 필요했던 시점.\nTask: 더 큰 영향력을 위해 소통, 설득, 전략적 사고, 조직 관리 역량을 개발.\nAction: 구체적 성장 경험(임원 발표 기회, 크로스펑셔널 리드, 채용 인터뷰 참여, 외부 강연).\nResult: 기술 외 역량 성장이 기술 작업에 미친 긍정적 변화.\n\n핵심: "T자형 인재"에서 "π자형 인재"로의 성장. 기술 깊이 + 비즈니스 너비의 시너지.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 리더십 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '조직의 기술 비전을 수립하고 구성원들을 정렬시킨 경험이 있나요?', '조직의 기술 비전을 수립하고 구성원들을 정렬시킨 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 조직이 기술적 방향 없이 단기 요구에만 반응하던 상황.\nTask: 3~5년 기술 비전을 수립하고 전 구성원이 같은 방향을 향해 움직이게 하는 과제.\nAction: 비전 워크숍 진행, 현재-미래 갭 분석, 북극성 지표(North Star Metric) 정의, 타운홀 공유, OKR 연계.\nResult: 비전 수립 후 팀 정렬 지표(OKR 달성률, 자발적 기여 증가).\n\n핵심: 비전은 "선언"이 아닌 "대화". 구성원이 비전을 자신의 것으로 내면화하는 과정 강조.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '권한 위임(Delegation)을 실천하면서 얻은 교훈이 있나요?', '권한 위임(Delegation)을 실천하면서 얻은 교훈이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 모든 것을 본인이 결정하던 방식에서 위임으로 전환이 필요했던 성장 시점.\nTask: 팀원의 성장과 조직 확장 가능성을 위해 효과적으로 권한을 위임하는 과제.\nAction: 위임 대상 선정(업무 중요도 × 팀원 역량), 명확한 성공 기준과 경계 설정, 적절한 체크인(마이크로매니지먼트 금지), 실패 허용과 지원.\nResult: 위임 후 팀원의 성장과 본인이 더 전략적 작업에 집중한 효과.\n\n핵심: 위임은 "무책임"이 아닌 "팀 역량 개발". 컨트롤 욕구를 내려놓는 것이 시니어의 성숙.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '조직 구조 변경(팀 재편, M&A, 리오그)을 기술 관점에서 이끈 경험이 있나요?', '조직 구조 변경(팀 재편, M&A, 리오그)을 기술 관점에서 이끈 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 합병, 팀 분리/합병, 조직 재편 등 기술 조직에 구조적 변화가 생긴 배경.\nTask: 기술 통합 또는 분리 과정에서 연속성 유지와 새로운 팀 정렬을 이끄는 역할.\nAction: 기술 인벤토리 통합, 시스템 아키텍처 재설계, 인재 역량 재배치, 새 팀 문화 형성.\nResult: 구조 변경 후 기술 통합 완료 지표와 팀 생산성 회복 시점.\n\n핵심: 조직 변화에서 기술 리더는 "안정감 제공자". 불확실성 속에서 명확한 방향을 제시.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '다수의 팀 또는 대규모 프로젝트를 동시에 조율한 경험이 있나요?', '다수의 팀 또는 대규모 프로젝트를 동시에 조율한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 3개 이상의 팀이 연관된 대규모 프로그램을 조율해야 했던 배경.\nTask: 각 팀의 자율성을 보장하면서도 전체 목표를 향해 정렬시키는 조율자 역할.\nAction: 프로그램 수준 로드맵 수립, 팀 간 의존성 관리(RAID Log), 정기 스티어링 미팅, 에스컬레이션 기준 명확화.\nResult: 프로그램 완료 지표와 팀 자율성 유지 수준.\n\n핵심: 대규모 조율은 "통제"가 아닌 "정보 흐름 관리". 투명성이 핵심 도구.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '채용과 팀 구성 전략에 직접 참여한 경험이 있나요?', '채용과 팀 구성 전략에 직접 참여한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 팀 성장 또는 스킬 갭 해소를 위한 채용 전략을 수립하게 된 배경.\nTask: 단순 포지션 채용이 아닌 팀의 장기 역량과 문화를 고려한 인재 확보.\nAction: 역할 정의와 성공 프로파일 수립, 인터뷰 프로세스 설계, 다양성 고려, 채용 바(bar) 기준 정립, 온보딩 프로그램 설계.\nResult: 채용 효율(Time-to-fill, 90일 성과)과 팀 문화 변화.\n\n핵심: "빠른 채용"보다 "올바른 채용". 잘못된 채용의 비용은 실제로 매우 높음을 인지.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '조직 내 정치적 역학 관계를 다루며 기술적 목표를 달성한 경험이 있나요?', '조직 내 정치적 역학 관계를 다루며 기술적 목표를 달성한 경험이 있나요?', '리더십',
 '[STAR 가이드]\nSituation: 조직 내 다른 부서 또는 리더십과 기술 방향에서 마찰이 있었던 상황.\nTask: 정치적 마찰을 최소화하면서도 기술적으로 올바른 결정을 관철하는 과제.\nAction: 이해관계자 맵핑, 각자의 동기와 우려 파악, 공통 이익 찾기, 데이터 기반 논리 강화, 점진적 신뢰 구축.\nResult: 원하는 방향 달성 여부와 이해관계자 관계 변화.\n\n핵심: "정치"는 나쁜 것이 아님. 조직 현실을 인식하고 영리하게 항법하는 능력이 시니어에게 필수.',
 'GUIDE', TRUE, NOW());

-- ============================================================
-- 커뮤니케이션 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', 'C레벨 또는 이사회에 기술 전략을 발표하고 설득한 경험이 있나요?', 'C레벨 또는 이사회에 기술 전략을 발표하고 설득한 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 대규모 기술 투자 또는 전략 변경을 경영진에게 승인 받아야 했던 상황.\nTask: 기술 전문 지식 없는 C레벨이 이해하고 결정할 수 있도록 발표하는 과제.\nAction: 발표 구조 설계(문제 → 영향 → 해결책 → 투자 대비 효과), 핵심 메시지 3개 이내 압축, 리스크 시나리오와 완화 방안 준비.\nResult: 발표 결과(승인/조건부 승인/기각)와 이후 관계 변화.\n\n핵심: C레벨 커뮤니케이션은 "기술 정확성"보다 "결정을 도와주는 정보 제공".',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '기술 조직의 성과와 가치를 외부(경영진, 투자자, 고객)에 효과적으로 알린 경험이 있나요?', '기술 조직의 성과와 가치를 외부(경영진, 투자자, 고객)에 효과적으로 알린 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 기술 팀의 기여가 잘 인식되지 않거나 비즈니스 가치와 연결되지 않는 상황.\nTask: 기술 작업을 비즈니스 지표와 연결하여 가치를 가시화하는 과제.\nAction: 기술 지표(배포 빈도, 장애율)를 비즈니스 지표(수익 영향, 고객 만족도)와 연결, 정기 기술 뉴스레터, 테크 블로그 외부 공개.\nResult: 조직 내외부 인식 변화와 기술 투자 증가 여부.\n\n핵심: 기술 팀의 마케팅은 선택이 아닌 필수. 좋은 일은 알려야 인정받음.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '어려운 조직 변화(구조조정, 대규모 피벗)를 팀에 투명하게 전달한 경험이 있나요?', '어려운 조직 변화(구조조정, 대규모 피벗)를 팀에 투명하게 전달한 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 팀원들에게 부정적이거나 불안을 유발할 수 있는 조직 변화를 전달해야 했던 상황.\nTask: 변화의 이유와 영향을 솔직하게 전달하면서 팀의 신뢰와 사기를 유지하는 과제.\nAction: 알 수 있는 것과 아직 모르는 것을 명확히 구분, 질문에 솔직하게 답변, 팀원들의 우려를 경청, 할 수 있는 지원 약속.\nResult: 팀의 반응과 변화 후 이직률, 생산성 변화.\n\n핵심: 불확실한 상황일수록 "투명성"과 "솔직함"이 신뢰의 근거. 모르는 것을 "모른다"고 말하는 용기.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '기술 컨퍼런스나 외부 무대에서 발표하여 조직의 기술력을 알린 경험이 있나요?', '기술 컨퍼런스나 외부 무대에서 발표하여 조직의 기술력을 알린 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 외부 발표 기회를 갖게 된 배경(KCD, AWS Summit, 사내 기술 세미나 등).\nTask: 조직의 기술 사례를 외부 청중에게 가치 있게 전달하는 과제.\nAction: 청중 분석과 메시지 설계, 구체적 수치와 사례 중심 구성, 발표 연습, Q&A 준비.\nResult: 발표 반응(피드백, 문의), 채용/브랜딩/파트너십에 미친 영향.\n\n핵심: 외부 발표는 개인 브랜딩이자 조직 기술 브랜딩. 기술 커뮤니티에 기여하는 태도.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '서로 다른 이해관계를 가진 다수의 이해관계자를 한 방향으로 정렬시킨 경험이 있나요?', '서로 다른 이해관계를 가진 다수의 이해관계자를 한 방향으로 정렬시킨 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 각기 다른 부서/임원이 서로 다른 기술 방향을 요구하는 상황.\nTask: 모든 이해관계자가 납득할 수 있는 공통 방향을 도출하고 실행으로 연결하는 과제.\nAction: 이해관계자별 핵심 관심사 파악, 공통 목표(북극성) 프레이밍, 트레이드오프 투명화, 단계별 합의 빌딩.\nResult: 정렬에 성공한 방식과 그 이후 프로젝트 진행 속도.\n\n핵심: "모두를 행복하게"는 불가능. "모두가 납득할 수 있는 트레이드오프"를 제시하는 능력.',
 'GUIDE', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:BEHAVIORAL', '조직의 기술 역량을 외부 채용이 아닌 내부 육성으로 강화한 경험이 있나요?', '조직의 기술 역량을 외부 채용이 아닌 내부 육성으로 강화한 경험이 있나요?', '커뮤니케이션',
 '[STAR 가이드]\nSituation: 팀에 필요한 기술 역량이 부족하지만 채용보다 내부 육성이 더 적합했던 상황.\nTask: 기존 팀원을 성장시켜 필요한 역량을 내재화하는 장기 과제.\nAction: 개인별 성장 계획 수립, 스트레치 어사인먼트 배정, 학습 시간과 예산 지원, 성장 가시화(승진, 역할 확대).\nResult: 내부 육성 성공 사례와 팀 역량 향상 지표. 채용 비용 대비 효과.\n\n핵심: 내부 육성은 팀 충성도와 문화 연속성의 핵심. "만들기 vs 사기" 전략 선택 근거.',
 'GUIDE', TRUE, NOW());
