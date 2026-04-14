-- DevOps AWS/Kubernetes 시드 데이터 (180문항: INFRA_CICD 90 + CLOUD 90)

-- ============================================================
-- INFRA_CICD — JUNIOR (30문항)
-- cache_key: DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'CI/CD의 개념과 각 단계를 설명해주세요.', '씨아이 씨디의 개념과 각 단계를 설명해주세요.', 'CICD',
 'CI(Continuous Integration)는 코드 변경을 자주 병합하고 자동 빌드/테스트하는 것입니다. CD는 Continuous Delivery(수동 배포 승인)와 Continuous Deployment(자동 배포)로 나뉩니다. 빌드 → 테스트 → 스테이징 → 프로덕션 순으로 파이프라인이 구성되며, 빠른 피드백과 안정적 배포를 목표로 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'GitHub Actions의 기본 구조를 설명해주세요.', 'GitHub Actions의 기본 구조를 설명해주세요.', 'CICD',
 'Workflow(.github/workflows/*.yml)는 Event(push, PR)에 의해 트리거됩니다. Job은 Runner(ubuntu-latest 등)에서 실행되며, Step은 Action(uses:) 또는 Shell 명령(run:)으로 구성됩니다. 여러 Job은 needs로 의존 관계를 설정하고, secrets으로 민감 정보를 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Docker 이미지와 컨테이너의 차이를 설명해주세요.', '도커 이미지와 컨테이너의 차이를 설명해주세요.', 'DOCKER',
 '이미지는 애플리케이션과 의존성을 포함한 읽기 전용 템플릿으로, 레이어로 구성됩니다. 컨테이너는 이미지의 실행 인스턴스로, 읽기/쓰기 레이어가 추가됩니다. 하나의 이미지로 여러 컨테이너를 생성할 수 있으며, 컨테이너는 격리된 프로세스로 호스트 OS 커널을 공유합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Dockerfile의 주요 명령어를 설명해주세요.', 'Dockerfile의 주요 명령어를 설명해주세요.', 'DOCKER',
 'FROM은 베이스 이미지, COPY/ADD는 파일 복사, RUN은 빌드 시 명령 실행, CMD/ENTRYPOINT는 컨테이너 시작 시 실행할 명령입니다. WORKDIR은 작업 디렉토리, EXPOSE는 포트 문서화, ENV는 환경변수 설정입니다. 각 명령이 새 레이어를 생성하므로 RUN을 &&로 연결하여 레이어를 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'docker-compose의 용도와 기본 사용법을 설명해주세요.', '도커-compose의 용도와 기본 사용법을 설명해주세요.', 'DOCKER',
 'docker-compose는 여러 컨테이너를 YAML 파일로 정의하고 한 번에 실행/관리하는 도구입니다. services로 컨테이너를 정의하고, volumes으로 데이터를 영속화하며, networks로 컨테이너 간 통신을 설정합니다. 개발 환경에서 앱+DB+Redis를 한 번에 실행하는 데 주로 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', '환경 변수 관리 방법과 베스트 프랙티스를 설명해주세요.', '환경 변수 관리 방법과 베스트 프랙티스를 설명해주세요.', 'CICD',
 '.env 파일은 로컬 개발용으로 .gitignore에 추가합니다. CI/CD에서는 GitHub Secrets, AWS SSM Parameter Store, HashiCorp Vault로 관리합니다. 환경별(dev/staging/prod) 설정을 분리하고, 민감 정보는 절대 코드에 하드코딩하지 않습니다. 12-Factor App 원칙을 따릅니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Blue-Green, Rolling, Canary 배포 전략의 차이를 설명해주세요.', 'Blue-Green, Rolling, Canary 배포 전략의 차이를 설명해주세요.', 'DEPLOYMENT',
 'Blue-Green은 두 환경을 유지하고 트래픽을 한 번에 전환하여 빠른 롤백이 가능하지만 리소스가 2배 필요합니다. Rolling은 인스턴스를 순차적으로 교체하여 리소스 효율적이지만 롤백이 느립니다. Canary는 소수 트래픽으로 먼저 검증 후 점진 확대하여 가장 안전합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Docker 볼륨(Volume)의 종류와 차이를 설명해주세요.', '도커 볼륨(Volume)의 종류와 차이를 설명해주세요.', 'DOCKER',
 'Named Volume(docker volume create)은 Docker가 관리하며 데이터 영속성에 적합합니다. Bind Mount(-v /host:/container)는 호스트 디렉토리를 직접 마운트하여 개발 시 코드 실시간 반영에 유용합니다. tmpfs Mount는 메모리에 저장되어 임시 데이터에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Docker 네트워크의 종류를 설명해주세요.', '도커 네트워크의 종류를 설명해주세요.', 'DOCKER',
 'bridge(기본, 같은 호스트 컨테이너 통신), host(호스트 네트워크 직접 사용, 포트 매핑 불필요), none(네트워크 없음), overlay(다중 호스트 컨테이너 통신, Swarm/K8s)가 있습니다. 같은 네트워크의 컨테이너는 컨테이너 이름으로 DNS 조회가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Git branching 전략(Git Flow, GitHub Flow, Trunk-Based)을 비교해주세요.', 'Git branching 전략(Git Flow, GitHub Flow, Trunk-Based)을 비교해주세요.', 'CICD',
 'Git Flow는 develop/release/hotfix 브랜치로 엄격한 릴리스 관리에 적합하지만 복잡합니다. GitHub Flow는 main + feature 브랜치로 간단하며 PR 기반 코드 리뷰를 중시합니다. Trunk-Based는 main에 직접 커밋하며 Feature Flag로 배포를 제어하여 CI/CD에 최적화됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Docker 이미지 크기를 줄이는 방법을 설명해주세요.', '도커 이미지 크기를 줄이는 방법을 설명해주세요.', 'DOCKER',
 'Alpine 기반 이미지(~5MB vs Ubuntu ~70MB)를 사용하고, 멀티스테이지 빌드로 빌드 도구를 최종 이미지에서 제거합니다. .dockerignore로 불필요한 파일을 제외하고, RUN 명령을 &&로 연결하여 레이어를 줄입니다. apt-get clean && rm -rf /var/lib/apt/lists/*로 캐시를 정리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Kubernetes의 기본 개념과 구성 요소를 설명해주세요.', '쿠버네티스의 기본 개념과 구성 요소를 설명해주세요.', 'KUBERNETES',
 'Kubernetes는 컨테이너 오케스트레이션 플랫폼으로, 배포/스케일링/관리를 자동화합니다. Control Plane(API Server, etcd, Scheduler, Controller Manager)과 Worker Node(kubelet, kube-proxy, Container Runtime)로 구성됩니다. Pod가 최소 배포 단위이며 하나 이상의 컨테이너를 포함합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Kubernetes Pod란 무엇인가요?', '쿠버네티스 Pod란 무엇인가요?', 'KUBERNETES',
 'Pod는 K8s의 최소 배포 단위로, 하나 이상의 컨테이너를 포함합니다. 같은 Pod의 컨테이너는 네트워크(localhost), 스토리지(Volume)를 공유합니다. 일반적으로 하나의 Pod에 하나의 앱 컨테이너를 배치하고, 사이드카 패턴으로 로깅/프록시 컨테이너를 추가합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Linux의 기본 명령어(프로세스, 네트워크, 파일)를 설명해주세요.', 'Linux의 기본 명령어(프로세스, 네트워크, 파일)를 설명해주세요.', 'CICD',
 'ps/top/htop(프로세스), netstat/ss(네트워크 연결), curl/wget(HTTP 요청), grep/awk/sed(텍스트 처리), chmod/chown(권한), systemctl(서비스 관리), journalctl(로그 확인)이 핵심 명령어입니다. 서버 문제 디버깅과 자동화 스크립트 작성에 필수적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Infrastructure as Code(IaC)의 개념과 장점을 설명해주세요.', 'Infrastructure as Code(IaC)의 개념과 장점을 설명해주세요.', 'CICD',
 'IaC는 인프라를 코드로 정의하고 버전 관리하는 방식입니다. Terraform, CloudFormation, Pulumi가 대표적입니다. 재현성(동일 환경 반복 생성), 버전 관리(변경 추적), 자동화(CI/CD 통합), 문서화(코드 자체가 문서), 코드 리뷰(인프라 변경도 PR)가 장점입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Container Registry(ECR, Docker Hub)의 역할을 설명해주세요.', 'Container Registry(ECR, 도커 Hub)의 역할을 설명해주세요.', 'DOCKER',
 'Container Registry는 Docker 이미지를 저장하고 배포하는 저장소입니다. Docker Hub는 공개 이미지 호스팅, ECR은 AWS 통합 프라이빗 레지스트리입니다. 이미지 태그로 버전을 관리하고, docker push/pull로 업로드/다운로드합니다. 취약점 스캔 기능도 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Nginx와 Apache의 차이를 설명해주세요.', '엔진엑스와 Apache의 차이를 설명해주세요.', 'DEPLOYMENT',
 'Nginx는 이벤트 기반(비동기) 아키텍처로 높은 동시 연결 처리에 유리하며 리버스 프록시, 로드 밸런서로 많이 사용됩니다. Apache는 프로세스/스레드 기반으로 모듈(.htaccess)이 풍부하지만 동시 연결이 많을 때 메모리 사용이 높습니다. 현재 Nginx가 웹 서버 시장 점유율 1위입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'SSH의 개념과 키 기반 인증을 설명해주세요.', 'SSH의 개념과 키 기반 인증을 설명해주세요.', 'DEPLOYMENT',
 'SSH(Secure Shell)는 암호화된 원격 접속 프로토콜(포트 22)입니다. 키 기반 인증은 공개키를 서버에 등록하고, 개인키로 인증하여 패스워드보다 안전합니다. ssh-keygen으로 키 쌍을 생성하고, ~/.ssh/authorized_keys에 공개키를 등록합니다. ssh-agent로 키를 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'CI 파이프라인에서 캐싱의 중요성을 설명해주세요.', '씨아이 파이프라인에서 캐싱의 중요성을 설명해주세요.', 'CICD',
 'CI 캐싱은 빌드 시간을 크게 줄입니다. 의존성 캐시(npm cache, gradle cache), Docker 레이어 캐시, 빌드 결과 캐시를 활용합니다. GitHub Actions는 actions/cache로 캐싱하고, 키 기반으로 캐시 히트/미스를 관리합니다. 캐시 무효화 전략(lock 파일 해시)도 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', '컨테이너와 가상머신(VM)의 차이를 설명해주세요.', '컨테이너와 가상머신(VM)의 차이를 설명해주세요.', 'DOCKER',
 'VM은 하이퍼바이저 위에 게스트 OS를 포함하여 무겁고(GB 단위) 시작이 느립니다(분). 컨테이너는 호스트 OS 커널을 공유하여 경량(MB 단위)이고 시작이 빠릅니다(초). 컨테이너는 프로세스 격리 수준이며, VM은 완전한 OS 격리입니다. 보안이 중요한 멀티테넌트에는 VM이 더 안전합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Reverse Proxy의 개념과 활용을 설명해주세요.', 'Reverse Proxy의 개념과 활용을 설명해주세요.', 'DEPLOYMENT',
 'Reverse Proxy는 클라이언트 요청을 받아 백엔드 서버로 전달하는 중간 서버입니다. SSL 종료, 로드 밸런싱, 캐싱, 압축, 보안(DDoS 방어, IP 차단)을 처리합니다. Nginx, HAProxy, Envoy가 대표적이며, 백엔드 서버를 외부에 노출하지 않아 보안이 강화됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', '로그 관리의 중요성과 기본 도구를 설명해주세요.', '로그 관리의 중요성과 기본 도구를 설명해주세요.', 'CICD',
 '로그는 문제 진단, 보안 감사, 성능 모니터링에 필수적입니다. 구조화된 로그(JSON)가 파싱에 유리하고, 로그 레벨(DEBUG/INFO/WARN/ERROR)로 분류합니다. ELK Stack(Elasticsearch+Logstash+Kibana), CloudWatch Logs, Datadog이 대표적인 로그 관리 도구입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Health Check의 종류와 구현 방법을 설명해주세요.', 'Health Check의 종류와 구현 방법을 설명해주세요.', 'DEPLOYMENT',
 'Liveness Probe는 컨테이너 생존 확인(실패 시 재시작), Readiness Probe는 트래픽 수신 준비 확인(실패 시 서비스에서 제외), Startup Probe는 초기화 완료 확인입니다. HTTP GET(/health), TCP 소켓, 명령 실행 방식으로 구현합니다. 적절한 initialDelay와 period 설정이 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'YAML 파일의 특징과 JSON 대비 장점을 설명해주세요.', 'YAML 파일의 특징과 제이슨 대비 장점을 설명해주세요.', 'CICD',
 'YAML은 들여쓰기로 계층을 표현하며 가독성이 좋습니다. 주석(#)을 지원하고, 문자열에 따옴표가 선택적입니다. 앵커(&)/별칭(*)으로 중복을 줄일 수 있습니다. K8s 매니페스트, docker-compose, GitHub Actions 등 인프라 설정에 널리 사용됩니다. JSON은 프로그래밍 언어 간 데이터 교환에 더 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Git Hook의 종류와 CI에서의 활용을 설명해주세요.', 'Git 훅의 종류와 씨아이에서의 활용을 설명해주세요.', 'CICD',
 'pre-commit(커밋 전 린트/포맷 검사), commit-msg(커밋 메시지 규칙 검증), pre-push(푸시 전 테스트 실행)가 클라이언트 훅입니다. Husky + lint-staged로 변경 파일만 검사하여 빠른 피드백을 제공합니다. 서버 훅(pre-receive)으로 브랜치 보호 정책을 강제할 수도 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Docker의 멀티스테이지 빌드를 설명해주세요.', '도커의 멀티스테이지 빌드를 설명해주세요.', 'DOCKER',
 '멀티스테이지 빌드는 하나의 Dockerfile에서 여러 FROM을 사용하여 빌드와 실행을 분리합니다. 첫 번째 스테이지에서 빌드하고, 두 번째 스테이지에서 빌드 결과만 복사합니다. 빌드 도구(Maven, npm)가 최종 이미지에 포함되지 않아 이미지 크기가 크게 줄어듭니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- INFRA_CICD — MID (30문항)
-- cache_key: DEVOPS:MID:AWS_K8S:INFRA_CICD
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'ECS, EKS, Fargate의 차이와 선택 기준을 설명해주세요.', '이씨에스, 이케이에스, Fargate의 차이와 선택 기준을 설명해주세요.', 'KUBERNETES',
 'ECS는 AWS 네이티브 컨테이너 오케스트레이터로 간단하고 AWS 서비스 통합이 좋습니다. EKS는 관리형 Kubernetes로 K8s 생태계를 활용하며 멀티클라우드 이식성이 있습니다. Fargate는 서버리스 컨테이너로 인프라 관리가 불필요하지만 비용이 높습니다. 소규모는 Fargate, 대규모는 EKS가 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s Deployment, ReplicaSet, StatefulSet의 차이를 설명해주세요.', '쿠버네티스 Deployment, ReplicaSet, StatefulSet의 차이를 설명해주세요.', 'KUBERNETES',
 'Deployment는 무상태 앱의 선언적 배포를 관리하며 롤링 업데이트/롤백을 지원합니다. ReplicaSet은 Pod 복제본 수를 유지하며 Deployment가 내부적으로 관리합니다. StatefulSet은 상태가 있는 앱(DB, 캐시)용으로 안정적인 네트워크 ID와 영속 볼륨을 보장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s Service의 종류(ClusterIP, NodePort, LoadBalancer)를 비교해주세요.', '쿠버네티스 Service의 종류(ClusterIP, NodePort, LoadBalancer)를 비교해주세요.', 'KUBERNETES',
 'ClusterIP는 클러스터 내부 통신용(기본값)입니다. NodePort는 각 노드의 특정 포트(30000-32767)로 외부 접근을 허용합니다. LoadBalancer는 클라우드 로드 밸런서를 생성하여 외부 트래픽을 분산합니다. Ingress는 L7 라우팅(URL/호스트 기반)으로 여러 서비스에 대한 단일 진입점을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'ConfigMap과 Secret의 차이와 사용법을 설명해주세요.', 'ConfigMap과 Secret의 차이와 사용법을 설명해주세요.', 'KUBERNETES',
 'ConfigMap은 비밀이 아닌 설정 데이터를 key-value로 저장하고, 환경변수나 볼륨으로 Pod에 주입합니다. Secret은 Base64 인코딩된 민감 데이터(비밀번호, 토큰)를 저장하며, 메모리에만 마운트됩니다. 실무에서는 Sealed Secrets나 External Secrets Operator로 암호화를 강화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'HPA(Horizontal Pod Autoscaler)와 VPA의 차이를 설명해주세요.', 'HPA(Horizontal Pod Autoscaler)와 VPA의 차이를 설명해주세요.', 'KUBERNETES',
 'HPA는 CPU/메모리 사용률이나 커스텀 메트릭 기반으로 Pod 수를 자동 조절합니다. VPA는 Pod의 CPU/메모리 요청/제한을 자동 조절합니다. HPA는 무상태 앱에 적합하고, VPA는 스케일 아웃이 어려운 단일 Pod에 적합합니다. 둘을 동시에 같은 메트릭(CPU)으로 사용하면 충돌할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Helm Chart의 개념과 장점을 설명해주세요.', 'Helm Chart의 개념과 장점을 설명해주세요.', 'KUBERNETES',
 'Helm은 K8s 패키지 매니저로, Chart(템플릿화된 매니페스트 묶음)를 통해 복잡한 앱을 한 번에 배포합니다. values.yaml로 환경별 설정을 오버라이드하고, helm upgrade/rollback으로 버전을 관리합니다. 공식 차트 저장소(ArtifactHub)에서 검증된 차트를 사용할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s Ingress Controller의 역할과 구성을 설명해주세요.', '쿠버네티스 Ingress Controller의 역할과 구성을 설명해주세요.', 'KUBERNETES',
 'Ingress Controller(Nginx Ingress, ALB Ingress)는 Ingress 리소스의 규칙을 실행하여 외부 트래픽을 서비스로 라우팅합니다. 호스트/경로 기반 라우팅, TLS 종료, Rate Limiting을 설정합니다. annotations으로 세부 설정을 지정하고, 하나의 로드 밸런서로 여러 서비스에 접근할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Terraform의 핵심 개념(state, plan, apply)을 설명해주세요.', '테라폼의 핵심 개념(state, plan, apply)을 설명해주세요.', 'CICD',
 'Terraform은 HCL로 인프라를 선언적으로 정의합니다. State 파일은 현재 인프라 상태를 추적하며, 원격 백엔드(S3)에 저장합니다. plan은 변경 사항을 미리 보여주고, apply는 실제 적용합니다. 모듈로 재사용 가능한 인프라 블록을 만들고, workspace로 환경을 분리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s의 리소스 요청(Request)과 제한(Limit)의 차이를 설명해주세요.', '쿠버네티스의 리소스 요청(Request)과 제한(Limit)의 차이를 설명해주세요.', 'KUBERNETES',
 'Request는 Pod 스케줄링 시 보장되는 최소 리소스이고, Limit은 사용 가능한 최대 리소스입니다. CPU Limit 초과 시 스로틀링되고, Memory Limit 초과 시 OOMKilled됩니다. QoS Class는 Guaranteed(Request=Limit), Burstable(Request<Limit), BestEffort(미설정)로 분류됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s의 롤링 업데이트와 롤백 과정을 설명해주세요.', '쿠버네티스의 롤링 업데이트와 롤백 과정을 설명해주세요.', 'KUBERNETES',
 'Deployment의 strategy: RollingUpdate로 maxSurge(초과 Pod 수)와 maxUnavailable(최소 가용 Pod)을 설정합니다. 새 ReplicaSet의 Pod를 생성하면서 이전 ReplicaSet의 Pod를 줄여갑니다. kubectl rollout undo로 이전 버전으로 롤백하며, revision으로 특정 버전을 지정할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Docker 보안 best practice를 설명해주세요.', '도커 보안 best practice를 설명해주세요.', 'DOCKER',
 'root가 아닌 사용자로 실행(USER nonroot), 읽기 전용 파일시스템(--read-only), 최소 베이스 이미지(distroless), 이미지 취약점 스캔(Trivy, Snyk), 비밀 정보를 이미지에 포함하지 않기, 특권 모드(--privileged) 금지, 서명된 이미지 사용이 핵심 프랙티스입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s PersistentVolume과 PersistentVolumeClaim의 관계를 설명해주세요.', '쿠버네티스 PersistentVolume과 PersistentVolumeClaim의 관계를 설명해주세요.', 'KUBERNETES',
 'PV는 관리자가 프로비저닝한 스토리지 리소스(EBS, NFS)이고, PVC는 사용자가 요청하는 스토리지입니다. PVC가 PV에 바인딩되며, StorageClass로 동적 프로비저닝(요청 시 자동 생성)을 설정합니다. accessModes(ReadWriteOnce, ReadOnlyMany, ReadWriteMany)와 용량을 지정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', '모니터링 시스템(Prometheus + Grafana)의 구조를 설명해주세요.', '모니터링 시스템(Prometheus + Grafana)의 구조를 설명해주세요.', 'CICD',
 'Prometheus는 Pull 방식으로 메트릭을 수집하며, PromQL로 쿼리합니다. Exporter(node-exporter, JMX)가 메트릭을 노출하고, AlertManager가 알림을 전송합니다. Grafana는 대시보드로 시각화합니다. K8s에서는 kube-state-metrics, cAdvisor로 클러스터 메트릭을 수집합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s DaemonSet과 Job/CronJob의 차이를 설명해주세요.', '쿠버네티스 DaemonSet과 Job/CronJob의 차이를 설명해주세요.', 'KUBERNETES',
 'DaemonSet은 모든(또는 특정) 노드에 하나의 Pod를 실행합니다. 로그 수집(Fluentd), 모니터링(node-exporter), 네트워크 플러그인에 사용됩니다. Job은 일회성 작업을 완료까지 실행하고, CronJob은 스케줄 기반으로 주기적 작업(DB 백업, 리포트)을 실행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'CI/CD 파이프라인에서 테스트 전략을 설명해주세요.', '씨아이 씨디 파이프라인에서 테스트 전략을 설명해주세요.', 'CICD',
 'Unit Test(빠른 피드백, PR마다 실행), Integration Test(서비스 간 연동), E2E Test(전체 흐름, 비용 높음)을 피라미드 형태로 구성합니다. PR에서는 Unit + 핵심 Integration, merge 시 전체 Integration + E2E를 실행합니다. 테스트 병렬화, 캐싱, 선택적 실행으로 파이프라인 속도를 최적화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Docker 빌드 캐시 최적화 전략을 설명해주세요.', '도커 빌드 캐시 최적화 전략을 설명해주세요.', 'DOCKER',
 'Dockerfile 명령 순서를 변경 빈도가 낮은 것부터 배치합니다(COPY package.json → npm install → COPY src). BuildKit의 --mount=type=cache로 패키지 매니저 캐시를 영속화합니다. CI에서는 --cache-from으로 이전 빌드의 레이어를 재사용하고, 레지스트리 캐시(cache-to=type=registry)도 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- INFRA_CICD — SENIOR (30문항)
-- cache_key: DEVOPS:SENIOR:AWS_K8S:INFRA_CICD
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'GitOps(ArgoCD)의 원칙과 아키텍처를 설명해주세요.', 'GitOps(ArgoCD)의 원칙과 아키텍처를 설명해주세요.', 'GITOPS',
 'GitOps는 Git을 Single Source of Truth로 하여, 선언적 설정과 자동 동기화로 인프라를 관리합니다. ArgoCD는 Git 저장소의 매니페스트와 클러스터 상태를 지속적으로 비교하고 차이가 있으면 동기화합니다. App of Apps 패턴으로 여러 앱을 계층적으로 관리하고, PR 기반 승인 워크플로를 구성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'Service Mesh(Istio)의 아키텍처와 핵심 기능을 설명해주세요.', 'Service Mesh(Istio)의 아키텍처와 핵심 기능을 설명해주세요.', 'SERVICE_MESH',
 'Istio는 사이드카(Envoy) 프록시를 각 Pod에 자동 주입하여 트래픽을 제어합니다. 트래픽 관리(VirtualService, DestinationRule), 보안(mTLS 자동화, AuthorizationPolicy), 관측성(분산 추적, 메트릭, 로그)을 서비스 코드 변경 없이 제공합니다. istiod가 Control Plane으로 정책을 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s Network Policy의 개념과 활용을 설명해주세요.', '쿠버네티스 Network Policy의 개념과 활용을 설명해주세요.', 'KUBERNETES',
 'Network Policy는 Pod 간 트래픽을 L3/L4에서 제어하는 방화벽 규칙입니다. 기본 정책(Default Deny All)을 설정하고 필요한 통신만 허용하는 화이트리스트 방식이 보안에 유리합니다. podSelector, namespaceSelector, ipBlock으로 대상을 지정합니다. CNI 플러그인(Calico, Cilium)이 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s의 리소스 할당과 QoS Class를 설명해주세요.', '쿠버네티스의 리소스 할당과 QoS Class를 설명해주세요.', 'KUBERNETES',
 'Guaranteed(모든 컨테이너의 Request=Limit)는 OOM 우선순위가 가장 낮아 안정적입니다. Burstable(Request < Limit)은 필요시 추가 리소스를 사용합니다. BestEffort(Request/Limit 미설정)는 메모리 부족 시 가장 먼저 종료됩니다. 프로덕션 워크로드는 Guaranteed나 Burstable을 사용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', '멀티 클러스터 Kubernetes 관리 전략을 설명해주세요.', '멀티 클러스터 쿠버네티스 관리 전략을 설명해주세요.', 'KUBERNETES',
 '멀티 클러스터는 DR, 지역별 배포, 환경 분리에 필요합니다. Fleet(Rancher), Anthos(GCP), ArgoCD ApplicationSet으로 여러 클러스터에 일관되게 배포합니다. Federation v2로 리소스를 동기화하고, 글로벌 로드 밸런서(GSLB)로 트래픽을 분산합니다. 클러스터 간 서비스 디스커버리도 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', '카나리 배포를 자동화하는 방법을 설명해주세요.', '카나리 배포를 자동화하는 방법을 설명해주세요.', 'GITOPS',
 'Flagger나 Argo Rollouts로 카나리 배포를 자동화합니다. 트래픽을 점진적으로 이동(5%→25%→50%→100%)하며, Prometheus 메트릭(에러율, 지연)을 분석하여 자동 승격/롤백합니다. Istio의 VirtualService weight 조절이나 Nginx Ingress canary annotations으로 트래픽을 분할합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s Operator 패턴의 개념과 활용을 설명해주세요.', '쿠버네티스 Operator 패턴의 개념과 활용을 설명해주세요.', 'KUBERNETES',
 'Operator는 Custom Resource Definition(CRD)과 컨트롤러로 복잡한 앱의 운영을 자동화합니다. Reconciliation 루프로 원하는 상태와 현재 상태를 지속적으로 동기화합니다. PostgreSQL Operator, Prometheus Operator 등이 DB 클러스터 관리, 백업, 페일오버를 자동화합니다. Operator SDK/Kubebuilder로 개발합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'Chaos Engineering의 원칙과 K8s에서의 실천을 설명해주세요.', 'Chaos Engineering의 원칙과 쿠버네티스에서의 실천을 설명해주세요.', 'KUBERNETES',
 'Chaos Engineering은 프로덕션에서 장애를 의도적으로 주입하여 시스템 회복력을 검증합니다. 정상 상태 정의 → 가설 수립 → 실험(Pod 종료, 네트워크 지연, CPU 스트레스) → 결과 분석 순서입니다. Chaos Mesh, Litmus가 K8s 네이티브 도구이며, Game Day로 팀 대응 역량도 훈련합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s 보안 강화(Pod Security, RBAC, OPA)를 설명해주세요.', '쿠버네티스 보안 강화(Pod Security, RBAC, OPA)를 설명해주세요.', 'KUBERNETES',
 'Pod Security Standards(Privileged/Baseline/Restricted)로 Pod 보안 수준을 강제합니다. RBAC(Role/ClusterRole + RoleBinding)으로 API 접근을 제어하고, ServiceAccount별 최소 권한을 부여합니다. OPA/Gatekeeper로 정책(이미지 소스 제한, 리소스 제한 필수)을 코드화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', '분산 추적(Distributed Tracing)의 원리와 구현을 설명해주세요.', '분산 추적(Distributed Tracing)의 원리와 구현을 설명해주세요.', 'SERVICE_MESH',
 '분산 추적은 요청이 여러 서비스를 거치는 경로를 추적합니다. Trace(전체 요청), Span(개별 작업), Context Propagation(트레이스 ID 전파)이 핵심 개념입니다. OpenTelemetry가 표준이며, Jaeger/Zipkin으로 시각화합니다. 서비스 메시에서는 사이드카가 자동으로 헤더를 전파합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- CLOUD — JUNIOR (30문항)
-- cache_key: DEVOPS:JUNIOR:AWS_K8S:CLOUD
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'EC2 인스턴스의 개념과 인스턴스 유형을 설명해주세요.', '이씨투 인스턴스의 개념과 인스턴스 유형을 설명해주세요.', 'AWS_COMPUTE',
 'EC2는 AWS의 가상 서버 서비스입니다. 범용(t3, m5), 컴퓨팅 최적화(c5), 메모리 최적화(r5), 스토리지 최적화(i3), GPU(p3) 유형이 있습니다. On-Demand(시간당 과금), Reserved(1-3년 약정 할인), Spot(여유 용량 할인, 중단 가능)으로 비용을 최적화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'S3의 개념과 스토리지 클래스를 설명해주세요.', '에스쓰리의 개념과 스토리지 클래스를 설명해주세요.', 'AWS_STORAGE',
 'S3는 객체 스토리지로 무제한 용량, 99.999999999%(11 9s) 내구성을 제공합니다. Standard(자주 접근), IA(비빈번 접근, 저비용), Glacier(아카이브, 분~시간 복원), Intelligent-Tiering(자동 분류) 클래스가 있습니다. 수명 주기 정책으로 자동 클래스 전환하여 비용을 절감합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'RDS의 개념과 장점을 설명해주세요.', '알디에스의 개념과 장점을 설명해주세요.', 'AWS_STORAGE',
 'RDS는 관리형 관계형 데이터베이스로 MySQL, PostgreSQL, Oracle, SQL Server, Aurora를 지원합니다. 자동 백업, 패치, 스냅샷, Multi-AZ(고가용성), Read Replica(읽기 확장)를 제공합니다. 인프라 관리 부담을 줄여 애플리케이션 개발에 집중할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'Lambda의 개념과 사용 사례를 설명해주세요.', '람다의 개념과 사용 사례를 설명해주세요.', 'AWS_COMPUTE',
 'Lambda는 서버 관리 없이 코드를 실행하는 서버리스 컴퓨팅 서비스입니다. 이벤트(API Gateway, S3, SQS, EventBridge) 기반으로 트리거되며, 실행 시간(ms)과 메모리 기준으로 과금됩니다. 이미지 처리, API 백엔드, 데이터 변환, 스케줄 작업에 적합하며, 최대 15분까지 실행 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'VPC(Virtual Private Cloud)의 개념을 설명해주세요.', '브이피씨(Virtual Private Cloud)의 개념을 설명해주세요.', 'AWS_NETWORKING',
 'VPC는 AWS 클라우드 내 논리적으로 격리된 가상 네트워크입니다. CIDR 블록으로 IP 범위를 정의하고, 서브넷(Public/Private)으로 네트워크를 분리합니다. Internet Gateway로 인터넷 접속을, NAT Gateway로 프라이빗 서브넷의 아웃바운드 접속을 허용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'IAM(Identity and Access Management)의 핵심 개념을 설명해주세요.', 'IAM(Identity and Access Management)의 핵심 개념을 설명해주세요.', 'AWS_NETWORKING',
 'IAM은 AWS 리소스 접근을 제어합니다. User(사람), Group(사용자 묶음), Role(서비스/외부 계정), Policy(JSON 권한 문서)로 구성됩니다. 최소 권한 원칙(필요한 권한만 부여)을 따르고, MFA를 활성화합니다. EC2에는 IAM Role을, 사용자에게는 IAM User + Policy를 부여합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'CloudFront의 개념과 장점을 설명해주세요.', '클라우드프론트의 개념과 장점을 설명해주세요.', 'AWS_NETWORKING',
 'CloudFront는 AWS의 CDN으로, 전 세계 엣지 로케이션에서 콘텐츠를 캐싱하여 지연을 줄입니다. S3 정적 파일, ALB 동적 콘텐츠를 오리진으로 설정합니다. SSL/TLS 무료 제공, DDoS 방어(Shield 통합), Lambda@Edge로 엣지 컴퓨팅이 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS 리전(Region)과 가용 영역(AZ)의 차이를 설명해주세요.', '에이더블유에스 리전(Region)과 가용 영역(AZ)의 차이를 설명해주세요.', 'AWS_COMPUTE',
 '리전은 지리적으로 분리된 데이터 센터 클러스터(ap-northeast-2 = 서울)입니다. 가용 영역(AZ)은 리전 내 독립된 데이터 센터(2a, 2b, 2c)로, 전력/네트워크가 분리되어 단일 장애점을 방지합니다. Multi-AZ 배포로 고가용성을 확보하고, 리전 선택은 지연/규정/비용을 고려합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', '보안 그룹(Security Group)과 NACL의 차이를 설명해주세요.', '보안 그룹(Security Group)과 NACL의 차이를 설명해주세요.', 'AWS_NETWORKING',
 '보안 그룹은 인스턴스 수준의 상태 저장(Stateful) 방화벽으로, 인바운드 허용 시 아웃바운드 자동 허용됩니다. NACL은 서브넷 수준의 상태 비저장(Stateless) 방화벽으로 인/아웃바운드를 각각 설정해야 합니다. 보안 그룹은 Allow만, NACL은 Allow/Deny 모두 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'Route 53의 역할과 라우팅 정책을 설명해주세요.', 'Route 53의 역할과 라우팅 정책을 설명해주세요.', 'AWS_NETWORKING',
 'Route 53은 AWS의 관리형 DNS 서비스입니다. 도메인 등록, DNS 라우팅, 헬스 체크를 제공합니다. Simple(단일 레코드), Weighted(가중치 분산), Latency(최저 지연), Failover(장애 조치), Geolocation(지역 기반) 라우팅 정책을 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'EBS(Elastic Block Store)의 볼륨 유형을 설명해주세요.', 'EBS(Elastic Block Store)의 볼륨 유형을 설명해주세요.', 'AWS_STORAGE',
 'gp3(범용 SSD, 대부분 워크로드), io2(고성능 SSD, DB), st1(처리량 최적화 HDD, 빅데이터), sc1(콜드 HDD, 아카이브)가 있습니다. EBS는 EC2에 네트워크로 연결되는 블록 스토리지로, 스냅샷으로 백업하고 AZ 간 복사할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS의 Shared Responsibility Model을 설명해주세요.', '에이더블유에스의 Shared Responsibility Model을 설명해주세요.', 'AWS_COMPUTE',
 'AWS는 클라우드 "의" 보안(하드웨어, 네트워크, 물리적 시설)을 책임지고, 고객은 클라우드 "안"의 보안(OS 패치, 네트워크 설정, IAM, 데이터 암호화, 앱 보안)을 책임집니다. 관리형 서비스(RDS, Lambda)는 AWS의 책임 범위가 더 넓어집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'SQS(Simple Queue Service)의 개념과 사용 사례를 설명해주세요.', '에스큐에스(Simple Queue Service)의 개념과 사용 사례를 설명해주세요.', 'AWS_COMPUTE',
 'SQS는 완전관리형 메시지 큐 서비스입니다. Standard Queue(순서 보장 X, 무제한 처리량)와 FIFO Queue(순서 보장, 초당 300건)가 있습니다. 서비스 간 비동기 통신, 작업 큐, 버퍼링에 사용됩니다. Dead Letter Queue(DLQ)로 처리 실패 메시지를 별도 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS CLI와 SDK의 차이와 사용법을 설명해주세요.', '에이더블유에스 CLI와 SDK의 차이와 사용법을 설명해주세요.', 'AWS_COMPUTE',
 'AWS CLI는 터미널에서 AWS 서비스를 명령어로 관리하는 도구(aws s3 ls, aws ec2 describe-instances)입니다. SDK는 프로그래밍 언어별 라이브러리(boto3, AWS SDK for Java)로 애플리케이션에서 AWS를 호출합니다. 인증은 ~/.aws/credentials 또는 IAM Role로 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'Elastic IP와 Public IP의 차이를 설명해주세요.', 'Elastic 아이피와 Public 아이피의 차이를 설명해주세요.', 'AWS_NETWORKING',
 'Public IP는 인스턴스 시작 시 자동 할당되며, 중지/재시작 시 변경됩니다. Elastic IP는 고정 공인 IP로 계정에 할당되어 인스턴스에 연결/해제가 자유롭습니다. 미사용 Elastic IP에는 비용이 발생합니다. DNS에 고정 IP를 등록하거나 장애 시 다른 인스턴스로 빠르게 전환할 때 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- CLOUD — MID (30문항)
-- cache_key: DEVOPS:MID:AWS_K8S:CLOUD
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'ALB와 NLB의 차이와 선택 기준을 설명해주세요.', 'ALB와 NLB의 차이와 선택 기준을 설명해주세요.', 'AWS_NETWORKING',
 'ALB(Application Load Balancer)는 L7에서 HTTP/HTTPS 기반 라우팅(경로, 호스트, 헤더)을 제공합니다. NLB(Network Load Balancer)는 L4에서 TCP/UDP를 처리하며 초저지연, 고정 IP, 수백만 RPS를 지원합니다. 웹 앱은 ALB, 게임/IoT/gRPC는 NLB가 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'CloudWatch와 CloudTrail의 차이를 설명해주세요.', 'CloudWatch와 CloudTrail의 차이를 설명해주세요.', 'AWS_NETWORKING',
 'CloudWatch는 모니터링 서비스로 메트릭(CPU, 네트워크), 로그, 알람을 관리합니다. CloudTrail은 감사 서비스로 AWS API 호출(누가, 언제, 무엇을)을 기록합니다. CloudWatch는 운영 모니터링, CloudTrail은 보안 감사와 컴플라이언스에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'Auto Scaling의 정책 유형을 비교해주세요.', 'Auto Scaling의 정책 유형을 비교해주세요.', 'AWS_COMPUTE',
 'Target Tracking(CPU 60% 유지)은 간단하고 권장됩니다. Step Scaling(CPU 70% → +2, 80% → +4)은 세밀한 제어가 가능합니다. Scheduled(매일 9시 확장)은 예측 가능한 패턴에 적합합니다. Predictive Scaling은 ML 기반으로 트래픽을 예측하여 사전 확장합니다. Cooldown 기간으로 빈번한 스케일링을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'Lambda 콜드 스타트의 원인과 해결 방법을 설명해주세요.', '람다 콜드 스타트의 원인과 해결 방법을 설명해주세요.', 'AWS_SERVERLESS',
 '콜드 스타트는 새 실행 환경 생성 시 발생하며, 런타임 초기화 + 코드 로드 + 핸들러 초기화가 지연 원인입니다. Provisioned Concurrency로 미리 환경을 준비하거나, SnapStart(Java)로 스냅샷 기반 빠른 복원이 가능합니다. 패키지 크기 최소화, 의존성 경량화, ARM64(Graviton) 사용도 도움됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'IAM Role과 Policy의 관계와 best practice를 설명해주세요.', 'IAM Role과 Policy의 관계와 best practice를 설명해주세요.', 'AWS_NETWORKING',
 'Policy는 JSON으로 권한을 정의(Effect, Action, Resource, Condition)하고, Role에 연결합니다. AWS 관리형 정책과 커스텀 정책이 있으며, 인라인 정책보다 관리형 정책이 권장됩니다. 최소 권한 원칙, Access Analyzer로 미사용 권한 탐지, SCP(조직 수준 제한)를 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'ElastiCache(Redis vs Memcached)의 선택 기준을 설명해주세요.', 'ElastiCache(레디스 vs Memcached)의 선택 기준을 설명해주세요.', 'AWS_DATABASE',
 'Redis는 다양한 데이터 구조, 영속성, 복제, Pub/Sub, Lua 스크립트를 지원하여 대부분의 경우 권장됩니다. Memcached는 멀티스레드, 단순 key-value 캐싱에 적합하고 메모리 효율이 좋습니다. 세션 저장, 리더보드, 실시간 분석에는 Redis, 단순 DB 쿼리 캐싱에는 Memcached를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'SQS, SNS, Kinesis의 차이와 사용 사례를 설명해주세요.', '에스큐에스, SNS, Kinesis의 차이와 사용 사례를 설명해주세요.', 'AWS_SERVERLESS',
 'SQS는 메시지 큐(1:1 소비, Pull 방식)로 작업 분산에 적합합니다. SNS는 Pub/Sub(1:N 팬아웃, Push 방식)으로 알림, 이벤트 전파에 사용합니다. Kinesis는 실시간 스트리밍(대용량 순서 보장)으로 로그/이벤트 스트림에 적합합니다. SNS + SQS 팬아웃 패턴도 자주 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'RDS Proxy의 역할과 장점을 설명해주세요.', '알디에스 Proxy의 역할과 장점을 설명해주세요.', 'AWS_DATABASE',
 'RDS Proxy는 DB 연결을 풀링하여 Lambda 같은 서버리스 환경에서 커넥션 폭증을 방지합니다. IAM 인증으로 DB 자격 증명을 안전하게 관리하고, 페일오버 시 연결을 자동 전환하여 다운타임을 줄입니다. Aurora MySQL/PostgreSQL, RDS MySQL/PostgreSQL을 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS의 데이터 암호화(at rest, in transit)를 설명해주세요.', '에이더블유에스의 데이터 암호화(at rest, in transit)를 설명해주세요.', 'AWS_NETWORKING',
 'At Rest(저장 시 암호화)는 KMS 관리 키로 S3(SSE-S3/SSE-KMS), EBS, RDS를 암호화합니다. In Transit(전송 시 암호화)는 TLS/SSL로 통신을 보호합니다. CMK(Customer Managed Key)로 키 순환을 자동화하고, Envelope Encryption으로 대규모 데이터를 효율적으로 암호화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS VPC Peering과 Transit Gateway의 차이를 설명해주세요.', '에이더블유에스 브이피씨 Peering과 Transit Gateway의 차이를 설명해주세요.', 'AWS_NETWORKING',
 'VPC Peering은 두 VPC를 1:1로 연결하며, 전이적 라우팅이 불가능합니다. N개 VPC 연결 시 N(N-1)/2개 피어링이 필요합니다. Transit Gateway는 중앙 허브로 여러 VPC, VPN, Direct Connect를 연결하여 라우팅을 단순화합니다. 10개 이상 VPC 연결 시 Transit Gateway가 관리에 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'DynamoDB의 특징과 설계 원칙을 설명해주세요.', '다이나모디비의 특징과 설계 원칙을 설명해주세요.', 'AWS_DATABASE',
 'DynamoDB는 완전관리형 NoSQL(Key-Value/Document)로 밀리초 지연, 무제한 확장을 제공합니다. Partition Key(필수)와 Sort Key(선택)로 데이터를 분산합니다. 단일 테이블 설계(Single Table Design)로 관련 엔티티를 하나의 테이블에 저장하여 쿼리를 최소화합니다. On-Demand와 Provisioned 용량 모드가 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS Step Functions의 개념과 사용 사례를 설명해주세요.', '에이더블유에스 Step Functions의 개념과 사용 사례를 설명해주세요.', 'AWS_SERVERLESS',
 'Step Functions는 서버리스 워크플로 오케스트레이터로, Lambda 등 AWS 서비스를 시각적으로 연결합니다. Standard(최대 1년, 감사)와 Express(최대 5분, 고빈도) 유형이 있습니다. 주문 처리, 데이터 파이프라인, ML 학습 파이프라인에 사용됩니다. 에러 처리, 재시도, 병렬 실행을 선언적으로 정의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'Aurora와 일반 RDS의 차이를 설명해주세요.', 'Aurora와 일반 알디에스의 차이를 설명해주세요.', 'AWS_DATABASE',
 'Aurora는 AWS가 클라우드 네이티브로 재설계한 DB로, MySQL/PostgreSQL 호환입니다. 3개 AZ에 6개 복제본으로 자동 복제하여 내구성이 높고, 읽기 확장(최대 15 Read Replica)이 용이합니다. 일반 RDS 대비 최대 5배(MySQL), 3배(PostgreSQL) 빠르며, Serverless v2로 자동 스케일링도 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS WAF의 개념과 설정 방법을 설명해주세요.', '에이더블유에스 WAF의 개념과 설정 방법을 설명해주세요.', 'AWS_NETWORKING',
 'WAF(Web Application Firewall)는 HTTP 트래픽을 필터링하여 XSS, SQL Injection, DDoS를 방어합니다. CloudFront, ALB, API Gateway에 연결합니다. 관리형 규칙(AWS Managed Rules), IP 기반 규칙, Rate-based 규칙, 커스텀 규칙을 조합합니다. OWASP Top 10 대응에 필수적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'API Gateway의 개념과 Lambda 통합을 설명해주세요.', '에이피아이 Gateway의 개념과 람다 통합을 설명해주세요.', 'AWS_SERVERLESS',
 'API Gateway는 REST/HTTP/WebSocket API를 관리하는 완전관리형 서비스입니다. Lambda Proxy 통합으로 요청을 Lambda에 전달하고, 스테이지(dev/prod) 관리, 인증(Cognito, JWT), 스로틀링, 캐싱을 제공합니다. HTTP API가 REST API보다 빠르고 저렴하며, 대부분의 사용 사례에 충분합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- CLOUD — SENIOR (30문항)
-- cache_key: DEVOPS:SENIOR:AWS_K8S:CLOUD
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', '멀티 리전 DR(Disaster Recovery) 전략을 설명해주세요.', '멀티 리전 디알(Disaster Recovery) 전략을 설명해주세요.', 'AWS_ARCHITECTURE',
 'Backup & Restore(RPO 시간, RTO 시간, 최저비용), Pilot Light(핵심 인프라만 유지), Warm Standby(축소 운영), Multi-site Active-Active(완전 이중화, 최저 RTO/RPO) 4단계가 있습니다. Route 53 Failover, Aurora Global Database, S3 Cross-Region Replication으로 구현하며, 비용과 RTO/RPO 요구사항에 따라 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'EventBridge의 이벤트 아키텍처와 활용을 설명해주세요.', 'EventBridge의 이벤트 아키텍처와 활용을 설명해주세요.', 'AWS_SERVERLESS',
 'EventBridge는 서버리스 이벤트 버스로, AWS 서비스/SaaS/커스텀 이벤트를 중앙에서 라우팅합니다. 규칙(Rule)으로 이벤트 패턴을 매칭하고 대상(Lambda, SQS, Step Functions)으로 전달합니다. Schema Registry로 이벤트 스키마를 관리하고, Archive/Replay로 이벤트를 재생합니다. 이벤트 기반 아키텍처의 핵심입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', '서버리스 아키텍처 설계 원칙을 설명해주세요.', '서버리스 아키텍처 설계 원칙을 설명해주세요.', 'AWS_SERVERLESS',
 '단일 책임 함수(작게, 빠르게), 이벤트 기반 설계, 비동기 처리(SQS, SNS), Idempotency(중복 실행 안전), 상태 비저장(DynamoDB/S3에 상태 저장)이 핵심 원칙입니다. 콜드 스타트 최소화, 동시성 제어(Reserved Concurrency), Power Tuning으로 메모리/비용 최적화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS 비용 최적화 전략을 설명해주세요.', '에이더블유에스 비용 최적화 전략을 설명해주세요.', 'AWS_ARCHITECTURE',
 'Right-sizing(인스턴스 크기 최적화), Savings Plans/Reserved Instances(1-3년 약정 할인), Spot Instances(비핵심 워크로드), 자동 스케일링(수요 기반)이 핵심입니다. Cost Explorer/Budget으로 비용을 모니터링하고, Graviton(ARM) 인스턴스는 x86 대비 40% 저렴합니다. S3 Intelligent-Tiering, 미사용 리소스 정리도 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'Well-Architected Framework의 6가지 축을 설명해주세요.', 'Well-Architected Framework의 6가지 축을 설명해주세요.', 'AWS_ARCHITECTURE',
 '운영 우수성(자동화, IaC, 모니터링), 보안(IAM, 암호화, 침입 탐지), 신뢰성(Multi-AZ, Auto Scaling, 백업), 성능 효율성(적절한 리소스 선택, 캐싱), 비용 최적화(Reserved, Spot, Right-sizing), 지속 가능성(에너지 효율, 워크로드 최적화) 6가지 축으로 아키텍처를 평가합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS Organizations와 Landing Zone 구성을 설명해주세요.', '에이더블유에스 Organizations와 Landing Zone 구성을 설명해주세요.', 'AWS_ARCHITECTURE',
 'Organizations는 여러 AWS 계정을 중앙 관리하며, OU(조직 단위)로 그룹화합니다. SCP(Service Control Policy)로 계정별 권한을 제한합니다. Landing Zone(Control Tower)은 보안/거버넌스 규칙이 적용된 멀티 계정 환경을 자동 구성합니다. 환경별(dev/staging/prod), 팀별로 계정을 분리하는 것이 best practice입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 컴플라이언스(GDPR, SOC2)를 충족하는 방법을 설명해주세요.', '에이더블유에스에서 컴플라이언스(GDPR, SOC2)를 충족하는 방법을 설명해주세요.', 'AWS_SECURITY',
 'AWS Config로 리소스 설정을 규칙 기반으로 감사하고, CloudTrail로 API 호출을 기록합니다. GuardDuty로 위협을 탐지하고, Inspector로 취약점을 스캔합니다. AWS Artifact에서 SOC, PCI DSS 보고서를 확인합니다. 데이터 암호화(KMS), 접근 제어(IAM), 로깅(CloudTrail+CloudWatch)이 기본 요구사항입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS Direct Connect와 VPN의 차이를 설명해주세요.', '에이더블유에스 Direct Connect와 VPN의 차이를 설명해주세요.', 'AWS_NETWORKING',
 'VPN은 인터넷 위에 IPSec 터널로 연결하여 빠른 설정이 가능하지만 대역폭이 제한되고 지연이 불안정합니다. Direct Connect는 전용 회선으로 안정적인 대역폭(1-100Gbps)과 낮은 지연을 제공하지만, 구축에 수주가 걸리고 비용이 높습니다. 하이브리드 클라우드에서는 Direct Connect + VPN 백업을 조합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 마이크로서비스 간 통신 패턴을 설명해주세요.', '에이더블유에스에서 마이크로서비스 간 통신 패턴을 설명해주세요.', 'AWS_ARCHITECTURE',
 '동기 통신은 ALB/API Gateway + Service Discovery(Cloud Map)로 REST/gRPC를 사용합니다. 비동기 통신은 SQS(큐), SNS(팬아웃), EventBridge(이벤트 라우팅)로 서비스를 느슨하게 결합합니다. App Mesh(서비스 메시)로 트래픽 제어, mTLS, 분산 추적을 앱 코드 변경 없이 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'CloudFormation의 고급 기능(Nested Stack, Custom Resource)을 설명해주세요.', 'CloudFormation의 고급 기능(Nested Stack, Custom Resource)을 설명해주세요.', 'AWS_ARCHITECTURE',
 'Nested Stack은 템플릿을 모듈화하여 재사용하고, 스택 참조(Outputs/Imports)로 리소스를 공유합니다. Custom Resource는 Lambda를 호출하여 CloudFormation이 네이티브 지원하지 않는 리소스를 관리합니다. StackSets으로 여러 계정/리전에 일괄 배포하고, Drift Detection으로 수동 변경을 탐지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 데이터 레이크 아키텍처를 설계하는 방법을 설명해주세요.', '에이더블유에스에서 데이터 레이크 아키텍처를 설계하는 방법을 설명해주세요.', 'AWS_ARCHITECTURE',
 'S3를 중심으로 Raw/Curated/Analytics 계층을 구성합니다. Glue로 ETL/스키마 카탈로그를 관리하고, Athena로 S3 데이터를 SQL 쿼리합니다. Lake Formation으로 접근 제어와 거버넌스를 중앙화합니다. Kinesis/Kafka로 실시간 수집, EMR/Redshift Spectrum으로 대규모 분석을 수행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS Graviton 프로세서의 장점과 마이그레이션 방법을 설명해주세요.', '에이더블유에스 Graviton 프로세서의 장점과 마이그레이션 방법을 설명해주세요.', 'AWS_COMPUTE',
 'Graviton(ARM64)은 x86 대비 최대 40% 가성비가 좋고 에너지 효율이 높습니다. EC2(m7g, c7g), Lambda, RDS, ElastiCache에서 사용 가능합니다. 마이그레이션 시 ARM64 호환성 확인(네이티브 라이브러리), 멀티 아키텍처 Docker 이미지(buildx), CI/CD에 ARM 빌드 추가가 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 보안 자동화(Security Hub, GuardDuty, Config)를 설명해주세요.', '에이더블유에스에서 보안 자동화(Security Hub, GuardDuty, Config)를 설명해주세요.', 'AWS_SECURITY',
 'Security Hub는 보안 발견사항을 중앙 집계하고 CIS/PCI DSS 기준으로 평가합니다. GuardDuty는 ML 기반으로 비정상 활동(무단 접근, 악성 IP)을 탐지합니다. Config Rules로 리소스 설정 준수를 자동 감사합니다. 이벤트 기반 자동 대응(Lambda + EventBridge)으로 보안 위반 시 즉시 조치합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'FinOps의 원칙과 AWS에서의 실천을 설명해주세요.', 'FinOps의 원칙과 에이더블유에스에서의 실천을 설명해주세요.', 'AWS_ARCHITECTURE',
 'FinOps는 클라우드 비용을 엔지니어링 문화로 관리하는 방법론입니다. Inform(비용 가시성: Cost Explorer, 태그 전략), Optimize(최적화: Right-sizing, RI/Savings Plans, Spot), Operate(운영: Budget 알람, 팀별 비용 할당)의 3단계 사이클입니다. 단위 경제학(비용/트랜잭션)으로 효율을 측정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 Zero-Downtime 데이터베이스 마이그레이션을 수행하는 방법을 설명해주세요.', '에이더블유에스에서 Zero-Downtime 데이터베이스 마이그레이션을 수행하는 방법을 설명해주세요.', 'AWS_DATABASE',
 'DMS(Database Migration Service)로 전체 로드 + CDC(Change Data Capture)를 수행하여 소스와 타겟을 동기화합니다. SCT(Schema Conversion Tool)로 스키마를 변환합니다. 동기화 확인 후 애플리케이션 연결을 타겟으로 전환합니다. Blue-Green Deployment(RDS)로 DB 버전 업그레이드도 무중단 수행이 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 추가 83문항 (INFRA_CICD 및 CLOUD 보충)
-- ============================================================

-- 추가 4문항 (INFRA_CICD JUNIOR)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'Shell 스크립트의 기본 작성법과 CI 자동화에서의 활용을 설명해주세요.', 'Shell 스크립트의 기본 작성법과 씨아이 자동화에서의 활용을 설명해주세요.', 'CICD',
 'Shebang(#!/bin/bash)으로 시작하고, 변수($VAR), 조건문(if), 반복문(for/while), 함수로 구성합니다. set -euo pipefail로 에러 발생 시 스크립트를 즉시 중단합니다. CI 파이프라인에서 빌드, 테스트, 배포 자동화에 사용합니다. 인수($1, $@), 종료 코드($?)를 활용하여 재사용 가능한 스크립트를 작성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'SonarQube와 같은 코드 품질 도구를 CI에 통합하는 방법을 설명해주세요.', 'SonarQube와 같은 코드 품질 도구를 씨아이에 통합하는 방법을 설명해주세요.', 'CICD',
 'SonarQube는 코드 품질(버그, 취약점, 코드 스멜), 테스트 커버리지, 중복 코드를 분석합니다. CI 파이프라인에서 sonar-scanner를 실행하고 SonarCloud로 결과를 전송합니다. Quality Gate 통과 여부로 PR 머지를 제어할 수 있습니다. OWASP Dependency Check로 의존성 취약점도 함께 검사합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', '컨테이너 오케스트레이션이 필요한 이유를 설명해주세요.', '컨테이너 오케스트레이션이 필요한 이유를 설명해주세요.', 'KUBERNETES',
 '수십~수천 개 컨테이너를 수동으로 관리하면 배포, 스케일링, 장애 복구가 불가합니다. 오케스트레이터는 원하는 상태(desired state)를 선언하면 자동으로 컨테이너를 배포하고 장애 시 재시작합니다. 서비스 디스커버리, 로드 밸런싱, 롤링 업데이트, 리소스 스케줄링을 자동화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:INFRA_CICD', 'DevOps 문화의 핵심 원칙을 설명해주세요.', 'DevOps 문화의 핵심 원칙을 설명해주세요.', 'CICD',
 'Dev와 Ops의 협업으로 빠른 피드백과 신뢰할 수 있는 릴리스를 목표로 합니다. 자동화(CI/CD, IaC), 측정(메트릭, 로그, 추적), 공유(지식, 책임), 지속적 개선이 핵심입니다. "You build it, you run it" 원칙으로 개발팀이 프로덕션 책임을 공유합니다. 사일로를 제거하여 협력을 강화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 14문항 (INFRA_CICD MID)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s Namespace의 역할과 멀티테넌시 구현 방법을 설명해주세요.', '쿠버네티스 Namespace의 역할과 멀티테넌시 구현 방법을 설명해주세요.', 'KUBERNETES',
 'Namespace는 클러스터를 논리적으로 분리하는 단위입니다. 팀별, 환경별(dev/staging/prod) 분리에 사용합니다. ResourceQuota로 Namespace별 리소스를 제한하고, LimitRange로 Pod 기본 리소스를 설정합니다. NetworkPolicy로 Namespace 간 트래픽을 제어하고, RBAC으로 접근 권한을 분리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'GitOps의 개념과 ArgoCD를 활용한 구현을 설명해주세요.', 'GitOps의 개념과 ArgoCD를 활용한 구현을 설명해주세요.', 'CICD',
 'GitOps는 Git을 인프라/앱 설정의 단일 진실 소스로 사용하는 운영 방식입니다. Git 커밋이 유일한 변경 방법이며, 자동화 에이전트가 Git 상태를 클러스터에 동기화합니다. ArgoCD는 Git과 K8s 클러스터를 모니터링하다가 차이(drift)가 생기면 자동 또는 수동으로 동기화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s NetworkPolicy로 Pod 간 통신을 제어하는 방법을 설명해주세요.', '쿠버네티스 NetworkPolicy로 Pod 간 통신을 제어하는 방법을 설명해주세요.', 'KUBERNETES',
 'NetworkPolicy는 L3/L4 수준에서 Pod 간 ingress/egress 트래픽을 허용/차단합니다. podSelector로 정책을 적용할 Pod를, namespaceSelector로 허용할 Namespace를 지정합니다. 기본 deny-all 정책 후 필요한 통신만 허용하는 화이트리스트 방식이 권장됩니다. Calico, Cilium 등 CNI 플러그인이 NetworkPolicy를 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Prometheus와 Grafana를 활용한 K8s 모니터링 구성을 설명해주세요.', '프로메테우스와 그라파나를 활용한 쿠버네티스 모니터링 구성을 설명해주세요.', 'CICD',
 'Prometheus는 pull 방식으로 메트릭을 수집하고, PromQL로 쿼리합니다. kube-prometheus-stack Helm Chart로 Prometheus+Grafana+AlertManager를 한 번에 구성합니다. 서비스에 /metrics 엔드포인트를 노출하거나, ServiceMonitor 리소스로 수집 대상을 정의합니다. Grafana 대시보드로 시각화하고, AlertManager로 알람을 Slack에 전송합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Istio 서비스 메시의 핵심 기능을 설명해주세요.', 'Istio 서비스 메시의 핵심 기능을 설명해주세요.', 'KUBERNETES',
 'Istio는 sidecar proxy(Envoy)를 각 Pod에 주입하여 앱 코드 변경 없이 트래픽을 제어합니다. mTLS 자동 암호화, Circuit Breaker, 트래픽 가중치 분배(Canary), 재시도/타임아웃, 분산 추적(Jaeger)을 제공합니다. VirtualService와 DestinationRule로 트래픽 라우팅을 세밀하게 제어합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s RBAC(Role-Based Access Control) 설정 방법을 설명해주세요.', '쿠버네티스 RBAC(Role-Based Access Control) 설정 방법을 설명해주세요.', 'KUBERNETES',
 'Role(Namespace 범위)과 ClusterRole(클러스터 범위)로 권한을 정의합니다. RoleBinding/ClusterRoleBinding으로 ServiceAccount/User/Group에 Role을 연결합니다. 최소 권한 원칙(Principle of Least Privilege)으로 필요한 리소스와 동사(get, list, watch, create, delete)만 허용합니다. 서비스 계정으로 Pod의 API 접근을 제어합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Terraform state 관리와 팀 협업 방법을 설명해주세요.', '테라폼 state 관리와 팀 협업 방법을 설명해주세요.', 'CICD',
 'State를 원격 백엔드(S3 + DynamoDB lock)에 저장하여 팀 협업을 지원합니다. State lock으로 동시 실행을 방지합니다. terraform workspace로 환경을 분리하거나, 디렉토리 구조(environments/dev, prod)로 분리합니다. terraform plan을 PR 리뷰에 포함하고, apply는 머지 후 CI에서 자동화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s DaemonSet과 Job, CronJob의 용도를 설명해주세요.', '쿠버네티스 DaemonSet과 Job, CronJob의 용도를 설명해주세요.', 'KUBERNETES',
 'DaemonSet은 모든(또는 선택된) 노드에 Pod를 하나씩 배포합니다. 로그 수집(Fluentd), 모니터링 에이전트, 네트워크 플러그인에 사용합니다. Job은 완료까지 Pod를 실행하며, 완료 후 종료됩니다. 배치 처리, 데이터 마이그레이션에 적합합니다. CronJob은 cron 표현식으로 Job을 주기적으로 실행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'EKS에서 ALB Ingress Controller를 설정하는 방법을 설명해주세요.', '이케이에스에서 ALB Ingress Controller를 설정하는 방법을 설명해주세요.', 'KUBERNETES',
 'AWS Load Balancer Controller를 Helm으로 설치하고, IAM 역할(IRSA)로 ELB 권한을 부여합니다. Ingress 리소스에 kubernetes.io/ingress.class: alb 어노테이션을 추가합니다. 어노테이션으로 SSL(alb.ingress.kubernetes.io/certificate-arn), 대상 타입(ip/instance), WAF 연결을 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Loki와 Promtail을 활용한 K8s 로그 집계를 설명해주세요.', 'Loki와 Promtail을 활용한 쿠버네티스 로그 집계를 설명해주세요.', 'CICD',
 'Promtail은 DaemonSet으로 각 노드의 컨테이너 로그를 수집하고 Loki로 전송합니다. Loki는 로그를 레이블 기반으로 인덱싱하여 저렴하게 저장합니다. Grafana에서 LogQL로 로그를 쿼리합니다. ELK 스택 대비 운영 비용이 낮고 Prometheus와 통합이 자연스럽습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s에서 Secret 관리를 강화하는 방법(External Secrets, Sealed Secrets)을 설명해주세요.', '쿠버네티스에서 Secret 관리를 강화하는 방법(External Secrets, Sealed Secrets)을 설명해주세요.', 'KUBERNETES',
 'K8s Secret은 기본적으로 Base64 인코딩만 되어 Git에 커밋하면 안 됩니다. External Secrets Operator는 AWS Secrets Manager, HashiCorp Vault에서 비밀을 가져와 K8s Secret으로 동기화합니다. Sealed Secrets는 공개키로 암호화하여 Git에 커밋 가능한 SealedSecret 리소스를 생성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Docker 이미지 취약점 스캔(Trivy, Snyk)을 CI에 통합하는 방법을 설명해주세요.', '도커 이미지 취약점 스캔(Trivy, Snyk)을 씨아이에 통합하는 방법을 설명해주세요.', 'DOCKER',
 'Trivy는 OS 패키지, 언어별 의존성(npm, Maven), IaC 파일의 취약점을 스캔합니다. CI 파이프라인에서 이미지 빌드 후 즉시 스캔하고, CRITICAL/HIGH 취약점 발견 시 파이프라인을 실패시킵니다. GitHub Actions의 trivy-action, ECR의 기본 스캔 기능을 활용합니다. 베이스 이미지를 정기적으로 업데이트하여 취약점을 줄입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'K8s Pod Disruption Budget(PDB)의 역할을 설명해주세요.', '쿠버네티스 Pod Disruption Budget(PDB)의 역할을 설명해주세요.', 'KUBERNETES',
 'PDB는 자발적 중단(노드 드레인, 업그레이드) 시 최소 가용 Pod 수를 보장합니다. minAvailable(최소 실행 Pod 수) 또는 maxUnavailable(최대 중단 Pod 수)을 설정합니다. 클러스터 업그레이드나 노드 유지보수 시 서비스 가용성을 유지합니다. Deployment와 함께 사용하여 무중단 운영을 보장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:INFRA_CICD', 'Ansible을 활용한 서버 구성 관리를 설명해주세요.', 'Ansible을 활용한 서버 구성 관리를 설명해주세요.', 'CICD',
 'Ansible은 에이전트리스(SSH) 구성 관리 도구로, YAML로 Playbook을 작성합니다. Inventory에 대상 서버를 정의하고, Role로 재사용 가능한 작업을 모듈화합니다. 멱등성(여러 번 실행해도 동일한 결과)을 보장합니다. Terraform으로 인프라를 프로비저닝하고, Ansible로 소프트웨어 구성을 관리하는 조합이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 20문항 (INFRA_CICD SENIOR)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s 클러스터 업그레이드 전략과 Zero-Downtime 유지 방법을 설명해주세요.', '쿠버네티스 클러스터 업그레이드 전략과 Zero-Downtime 유지 방법을 설명해주세요.', 'KUBERNETES',
 '컨트롤 플레인을 먼저 업그레이드하고 워커 노드를 순차 업그레이드합니다. 노드를 drain하여 워크로드를 이동 후 업그레이드합니다. PDB로 최소 가용 Pod를 보장하고, 롤링 업데이트 전략을 적용합니다. EKS는 관리형 노드 그룹의 자동 업그레이드를 활용합니다. 한 번에 한 마이너 버전씩 업그레이드하는 것이 원칙입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s Operator 패턴의 개념과 구현 방법을 설명해주세요.', '쿠버네티스 Operator 패턴의 개념과 구현 방법을 설명해주세요.', 'KUBERNETES',
 'Operator는 CRD(Custom Resource Definition)와 컨트롤러로 K8s API를 확장하여 도메인 지식을 자동화합니다. 상태를 모니터링하고 조정하는 Reconciliation Loop로 동작합니다. Operator SDK, Kubebuilder로 Go로 구현하거나, Shell/Python Operator도 가능합니다. DB 운영, 복잡한 앱 라이프사이클 관리에 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', '멀티 클러스터 K8s 관리 전략을 설명해주세요.', '멀티 클러스터 쿠버네티스 관리 전략을 설명해주세요.', 'KUBERNETES',
 '멀티 클러스터는 장애 격리, 규제 준수(데이터 지역화), 팀/환경 분리를 위해 필요합니다. 클러스터 페더레이션(KubeFed, Karmada), Fleet 관리(Rancher, ArgoCD ApplicationSet)로 중앙 관리합니다. 클러스터 간 통신은 Service Mesh(Istio 멀티클러스터)로 처리합니다. 클러스터당 전용 컨텍스트와 RBAC을 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'Chaos Engineering의 원칙과 실천 방법을 설명해주세요.', 'Chaos Engineering의 원칙과 실천 방법을 설명해주세요.', 'CICD',
 'Chaos Engineering은 프로덕션 시스템에 장애를 의도적으로 주입하여 회복력을 검증합니다. Chaos Monkey, Chaos Mesh(K8s 네이티브), LitmusChaos로 Pod 종료, 네트워크 지연, CPU 스파이크를 주입합니다. 정상 상태 가설을 설정하고, 최소 폭발 반경으로 시작하여 점진적으로 확대합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'SRE의 SLI, SLO, SLA, Error Budget 개념을 설명해주세요.', 'SRE의 에스엘아이, 에스엘오, 에스엘에이, Error Budget 개념을 설명해주세요.', 'CICD',
 'SLI(Service Level Indicator)는 서비스 수준을 측정하는 지표(가용성, 지연, 오류율)입니다. SLO(Service Level Objective)는 SLI의 목표값(가용성 99.9%)입니다. SLA(Service Level Agreement)는 고객과의 계약입니다. Error Budget은 1 - SLO로, 허용 가능한 오류량입니다. Error Budget 소진 시 기능 개발을 중단하고 안정성에 집중합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s 보안 강화(Pod Security Standards, OPA Gatekeeper)를 설명해주세요.', '쿠버네티스 보안 강화(Pod Security Standards, OPA Gatekeeper)를 설명해주세요.', 'KUBERNETES',
 'Pod Security Standards(PSS)는 privileged/baseline/restricted 3단계로 Pod 보안 정책을 적용합니다. Namespace 레이블로 정책 수준을 설정합니다. OPA(Open Policy Agent) Gatekeeper는 K8s AdmissionWebhook으로 Rego 정책을 실행하여 규정 비준수 리소스 생성을 차단합니다. Kyverno도 유사한 정책 엔진입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'OpenTelemetry를 활용한 분산 추적 시스템 구축을 설명해주세요.', 'OpenTelemetry를 활용한 분산 추적 시스템 구축을 설명해주세요.', 'CICD',
 'OpenTelemetry(OTel)는 분산 추적, 메트릭, 로그를 위한 벤더 중립 표준입니다. SDK로 앱을 계측하고, OTel Collector로 데이터를 수집/처리/전송합니다. Jaeger나 Zipkin으로 트레이스를 시각화하고, Tempo(Grafana)로 로그-트레이스를 연결합니다. TraceID로 마이크로서비스 간 요청 흐름을 추적합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'Platform Engineering과 Internal Developer Platform(IDP) 구축을 설명해주세요.', 'Platform Engineering과 Internal Developer Platform(IDP) 구축을 설명해주세요.', 'CICD',
 'Platform Engineering은 개발자 셀프서비스를 위한 내부 플랫폼을 구축하는 분야입니다. IDP는 환경 프로비저닝, 배포, 모니터링, 시크릿 관리를 추상화합니다. Backstage(Spotify)로 서비스 카탈로그와 개발자 포털을 구축합니다. "Paved Road" 원칙으로 모범 사례를 기본값으로 제공하여 개발자 인지 부하를 줄입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s eBPF 기반 CNI(Cilium)의 장점을 설명해주세요.', '쿠버네티스 eBPF 기반 CNI(Cilium)의 장점을 설명해주세요.', 'KUBERNETES',
 'Cilium은 eBPF(커널 레벨 프로그래밍)로 kube-proxy 없이 L3/L4/L7 네트워킹과 보안을 구현합니다. NetworkPolicy를 넘어 DNS 기반 정책, HTTP 메서드 레벨 정책을 지원합니다. iptables 대비 성능이 우수하고, Hubble로 네트워크 트래픽을 실시간 관측합니다. 서비스 메시 기능도 내장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'CI/CD 파이프라인의 보안(DevSecOps)을 강화하는 방법을 설명해주세요.', '씨아이 씨디 파이프라인의 보안(DevSecOps)을 강화하는 방법을 설명해주세요.', 'CICD',
 'Shift Left Security: 코드 작성 시점부터 보안을 통합합니다. SAST(정적 분석: CodeQL, SonarQube), SCA(의존성 취약점: Dependabot), DAST(동적 분석: OWASP ZAP), 이미지 스캔(Trivy)을 파이프라인에 통합합니다. Secret 스캔(git-secrets, truffleHog)으로 코드에 비밀 정보 유출을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s 클러스터 비용 최적화 전략을 설명해주세요.', '쿠버네티스 클러스터 비용 최적화 전략을 설명해주세요.', 'KUBERNETES',
 'Kubecost로 네임스페이스/팀별 비용을 가시화합니다. Spot 인스턴스(노드 그룹), Karpenter(효율적인 노드 프로비저닝)로 컴퓨트 비용을 줄입니다. 정확한 Request 설정(VPA 권고 활용)으로 오버프로비저닝을 방지합니다. 야간/주말 개발 클러스터를 자동 축소합니다. Graviton 노드로 동일 비용에 더 높은 성능을 확보합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'Karpenter를 활용한 K8s 노드 자동 프로비저닝을 설명해주세요.', 'Karpenter를 활용한 쿠버네티스 노드 자동 프로비저닝을 설명해주세요.', 'KUBERNETES',
 'Karpenter는 클러스터 오토스케일러의 대안으로, 스케줄링 불가 Pod를 감지하여 즉시 최적의 인스턴스를 프로비저닝합니다. 인스턴스 타입/크기를 동적으로 선택하여 낭비를 최소화합니다. Spot 통합, 노드 통합(consolidation)으로 비용을 최적화합니다. NodePool과 NodeClass로 정책을 정의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'Progressive Delivery(피처 플래그, 카나리)를 Argo Rollouts으로 구현하는 방법을 설명해주세요.', 'Progressive Delivery(피처 플래그, 카나리)를 Argo Rollouts으로 구현하는 방법을 설명해주세요.', 'CICD',
 'Argo Rollouts는 K8s Deployment를 대체하는 고급 배포 컨트롤러입니다. Canary 전략으로 트래픽 비율을 단계적으로 늘리고, Analysis(Prometheus 메트릭)로 자동 승인/롤백을 결정합니다. Istio/ALB와 통합하여 정밀한 트래픽 분배를 제어합니다. 배포 단계마다 메트릭 임계값을 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s etcd 백업과 복구 전략을 설명해주세요.', '쿠버네티스 etcd 백업과 복구 전략을 설명해주세요.', 'KUBERNETES',
 'etcd는 K8s 클러스터의 모든 상태를 저장하는 분산 키-값 스토어입니다. etcdctl snapshot save로 스냅샷을 생성하고, S3/GCS에 정기 백업합니다. 복구 시 컨트롤 플레인을 중단하고 etcdctl snapshot restore로 복원합니다. EKS 관리형 클러스터는 AWS가 자동으로 백업합니다. ETCD 암호화(encryption at rest)도 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'Terraform 모듈화와 엔터프라이즈 IaC 설계를 설명해주세요.', '테라폼 모듈화와 엔터프라이즈 IaC 설계를 설명해주세요.', 'CICD',
 '모듈은 특정 기능(VPC, EKS, RDS)을 캡슐화하여 재사용합니다. 레지스트리(Terraform Registry, 사설 레지스트리)로 모듈을 공유합니다. 모노레포에서 Terragrunt로 DRY(반복 제거)를 유지합니다. 워크스페이스별 상태 분리, 모듈 버저닝, 자동화된 plan 리뷰(Atlantis)가 엔터프라이즈 운영에 필수입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', '대규모 K8s 클러스터의 성능 튜닝을 설명해주세요.', '대규모 쿠버네티스 클러스터의 성능 튜닝을 설명해주세요.', 'KUBERNETES',
 'API Server 부하 감소: 과도한 watch 요청 제한, kubeconfig의 QPS/Burst 설정. etcd I/O 최적화: NVMe SSD, 전용 etcd 노드. 노드 수준: kubelet 설정(maxPods, 이미지 GC), 커널 파라미터(fs.inotify.max_user_watches). 스케줄러 성능: Pod topology spread로 균등 분배. 대규모 클러스터(1000+ 노드)에서 Control Plane 수평 확장이 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'DORA 메트릭과 DevOps 성숙도 평가 방법을 설명해주세요.', 'DORA 메트릭과 DevOps 성숙도 평가 방법을 설명해주세요.', 'CICD',
 'DORA 4가지 메트릭: 1) 배포 빈도, 2) 변경 리드 타임(커밋→프로덕션), 3) 변경 실패율, 4) 서비스 복구 시간(MTTR). Elite 팀은 일 1회 이상 배포, 1시간 이내 리드 타임, 5% 미만 실패율, 1시간 이내 복구를 달성합니다. 이 메트릭을 CI/CD 파이프라인에서 자동으로 측정하고 대시보드로 시각화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s 멀티테넌시 보안(Hard vs Soft Tenancy)을 설명해주세요.', '쿠버네티스 멀티테넌시 보안(Hard vs Soft Tenancy)을 설명해주세요.', 'KUBERNETES',
 'Soft Tenancy(단일 클러스터): Namespace, RBAC, NetworkPolicy, ResourceQuota로 테넌트를 논리 분리합니다. 경량이지만 완전한 격리는 아닙니다. Hard Tenancy(전용 클러스터/노드): 테넌트별 전용 클러스터나 전용 노드(nodeSelector, Taint/Toleration)로 강한 격리를 제공합니다. Kata Containers로 VM 수준 컨테이너 격리도 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', '서비스 카탈로그와 골든 패스(Golden Path) 구축을 설명해주세요.', '서비스 카탈로그와 골든 패스(Golden Path) 구축을 설명해주세요.', 'CICD',
 '골든 패스는 개발팀이 가장 쉽게 따를 수 있는 모범 사례 경로입니다. Backstage 서비스 카탈로그로 표준 템플릿(앱 스캐폴딩, CI/CD 파이프라인, 인프라)을 제공합니다. 보안, 관측성, 테스트가 기본으로 포함된 프로젝트 템플릿을 제공합니다. 선택이 아닌 기본값으로 모범 사례를 내장하여 팀의 인지 부하를 줄입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:INFRA_CICD', 'K8s Admission Webhook의 종류와 활용을 설명해주세요.', '쿠버네티스 Admission Webhook의 종류와 활용을 설명해주세요.', 'KUBERNETES',
 'Validating Webhook은 리소스 생성/수정 요청을 검증하고 거부합니다. Mutating Webhook은 요청을 수정합니다(사이드카 자동 주입, 기본값 설정). Admission Controller 체인을 통과해야 리소스가 etcd에 저장됩니다. OPA Gatekeeper, Kyverno, Istio의 사이드카 인젝터가 대표적인 Admission Webhook 활용 사례입니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 15문항 (CLOUD JUNIOR)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS의 공동 책임 모델(Shared Responsibility Model)을 설명해주세요.', '에이더블유에스의 공동 책임 모델(Shared Responsibility Model)을 설명해주세요.', 'AWS_SECURITY',
 'AWS는 클라우드의 보안(인프라, 물리 데이터센터, 하드웨어)을 책임지고, 고객은 클라우드 내 보안(데이터, IAM, 운영체제 패치, 앱 보안)을 책임집니다. EC2는 고객 책임 범위가 크고(OS 관리), RDS나 Lambda는 AWS 관리 범위가 큽니다. 책임 경계를 명확히 이해하는 것이 보안 설계의 출발점입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS IAM의 핵심 개념(사용자, 역할, 정책)을 설명해주세요.', '에이더블유에스 IAM의 핵심 개념(사용자, 역할, 정책)을 설명해주세요.', 'AWS_SECURITY',
 'IAM User는 사람을 위한 장기 자격증명입니다. IAM Role은 AWS 서비스/앱이 임시로 권한을 가져가는 방식으로, EC2가 S3에 접근하거나 Lambda가 DynamoDB에 접근할 때 사용합니다. Policy는 허용/거부 권한을 JSON으로 정의합니다. 루트 계정은 사용하지 않고 MFA를 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS S3의 스토리지 클래스와 선택 기준을 설명해주세요.', '에이더블유에스 에스쓰리의 스토리지 클래스와 선택 기준을 설명해주세요.', 'AWS_STORAGE',
 'Standard(자주 접근), Standard-IA(월 1회 미만, 저장 비용 낮고 접근 비용 높음), Glacier Instant/Flexible/Deep Archive(아카이브, 매우 저렴, 복원 시간 다양)가 있습니다. Intelligent-Tiering은 접근 패턴에 따라 자동으로 클래스를 전환합니다. S3 Lifecycle 정책으로 오래된 객체를 자동으로 저렴한 클래스로 이동합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS CloudWatch의 기본 기능과 활용을 설명해주세요.', '에이더블유에스 CloudWatch의 기본 기능과 활용을 설명해주세요.', 'AWS_COMPUTE',
 'CloudWatch는 AWS 리소스의 모니터링 서비스입니다. Metrics(CPU, 네트워크 등 시계열 지표), Logs(로그 수집/분석), Alarms(임계값 초과 시 SNS/이메일 알림), Dashboards(시각화)를 제공합니다. 커스텀 메트릭을 게시하고, Logs Insights로 로그를 쿼리합니다. EventBridge와 연동하여 자동화도 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS EC2 인스턴스 타입(범용, 컴퓨팅 최적화, 메모리 최적화)을 설명해주세요.', '에이더블유에스 이씨투 인스턴스 타입(범용, 컴퓨팅 최적화, 메모리 최적화)을 설명해주세요.', 'AWS_COMPUTE',
 '범용(t3/t4g, m6i/m7g)은 균형 잡힌 리소스로 대부분 워크로드에 적합합니다. 컴퓨팅 최적화(c6i/c7g)는 고성능 CPU가 필요한 배치, 미디어 처리에 적합합니다. 메모리 최적화(r6i/r7g, x2)는 대용량 인메모리 DB에 적합합니다. 스토리지 최적화(i3/i4i)는 높은 IOPS가 필요한 NoSQL에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS Load Balancer의 종류(ALB, NLB, CLB)를 설명해주세요.', '에이더블유에스 Load Balancer의 종류(ALB, NLB, CLB)를 설명해주세요.', 'AWS_NETWORKING',
 'ALB(Application Load Balancer)는 L7 HTTP/HTTPS를 처리하며 경로/호스트 기반 라우팅, WebSocket, HTTP/2를 지원합니다. NLB(Network Load Balancer)는 L4 TCP/UDP로 초저지연, 고성능이 필요할 때 사용합니다. CLB는 레거시로 신규 프로젝트에 권장되지 않습니다. 대부분의 웹 앱은 ALB를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS RDS와 자동 백업, 읽기 복제본을 설명해주세요.', '에이더블유에스 알디에스와 자동 백업, 읽기 복제본을 설명해주세요.', 'AWS_DATABASE',
 'RDS는 관리형 관계형 DB 서비스로 MySQL, PostgreSQL, Oracle 등을 지원합니다. 자동 백업(최대 35일 보존, S3 저장)과 수동 스냅샷을 제공합니다. Read Replica로 읽기 트래픽을 분산하고, Multi-AZ Deployment로 장애 조치를 자동화합니다. 유지보수 윈도우에서 패치가 자동 적용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS Security Group과 NACL의 차이를 설명해주세요.', '에이더블유에스 Security Group과 NACL의 차이를 설명해주세요.', 'AWS_NETWORKING',
 'Security Group은 인스턴스 레벨의 상태 저장(stateful) 방화벽으로, 허용 규칙만 설정합니다(기본 거부). NACL(Network Access Control List)은 서브넷 레벨의 상태 비저장(stateless) 방화벽으로, 허용/거부 규칙과 순서 번호가 있습니다. Security Group은 세밀한 제어, NACL은 넓은 서브넷 수준 방어에 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS CloudFront의 개념과 S3 정적 웹사이트 배포를 설명해주세요.', '에이더블유에스 클라우드프론트의 개념과 에스쓰리 정적 웹사이트 배포를 설명해주세요.', 'AWS_NETWORKING',
 'CloudFront는 글로벌 CDN으로 엣지 로케이션에서 콘텐츠를 캐싱하여 지연을 줄입니다. S3를 오리진으로 설정하고, CloudFront 배포를 생성합니다. OAC(Origin Access Control)로 S3를 CloudFront를 통해서만 접근하도록 제한합니다. SSL 인증서(ACM), 커스텀 도메인, 캐시 정책을 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS Auto Scaling Group의 동작 원리를 설명해주세요.', '에이더블유에스 Auto Scaling Group의 동작 원리를 설명해주세요.', 'AWS_COMPUTE',
 'ASG는 EC2 인스턴스 수를 자동으로 조절합니다. 최소/최대/원하는 용량을 설정하고, 스케일링 정책(CPU 기반, 스케줄, 스텝)에 따라 인스턴스를 추가/제거합니다. 런치 템플릿으로 인스턴스 설정을 정의합니다. ELB와 통합하여 추가된 인스턴스에 자동으로 트래픽을 분배합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS Elastic Cache(Redis)의 활용 사례를 설명해주세요.', '에이더블유에스 Elastic Cache(레디스)의 활용 사례를 설명해주세요.', 'AWS_DATABASE',
 'ElastiCache Redis는 인메모리 캐시로 DB 조회 결과 캐싱(세션, API 응답), 세션 저장소, 리더보드(Sorted Set), pub/sub 메시징에 사용합니다. RDS 앞에 캐시 레이어를 두어 DB 부하를 줄입니다. 클러스터 모드로 수평 확장하고, Multi-AZ로 고가용성을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS SQS와 SNS의 차이와 조합 패턴을 설명해주세요.', '에이더블유에스 에스큐에스와 SNS의 차이와 조합 패턴을 설명해주세요.', 'AWS_SERVERLESS',
 'SQS(Simple Queue Service)는 메시지 큐로 하나의 소비자가 메시지를 처리합니다. 디커플링과 비동기 처리에 사용합니다. SNS(Simple Notification Service)는 pub/sub으로 여러 구독자에게 동시에 메시지를 발송합니다. SNS→SQS 팬아웃 패턴으로 이벤트를 여러 큐에 동시 전달하는 것이 표준 패턴입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS Lambda의 기본 개념과 콜드 스타트를 설명해주세요.', '에이더블유에스 람다의 기본 개념과 콜드 스타트를 설명해주세요.', 'AWS_SERVERLESS',
 'Lambda는 서버 없이 코드를 실행하는 서버리스 서비스입니다. 이벤트 트리거(API Gateway, S3, DynamoDB Streams)로 실행되고 사용한 만큼만 과금됩니다. 콜드 스타트는 처음 또는 유휴 후 실행 시 컨테이너를 초기화하는 지연입니다. Provisioned Concurrency로 콜드 스타트를 방지하고, 함수를 경량하게 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS Route 53의 라우팅 정책을 설명해주세요.', '에이더블유에스 Route 53의 라우팅 정책을 설명해주세요.', 'AWS_NETWORKING',
 'Simple(단순 DNS), Failover(헬스체크 기반 장애조치), Geolocation(사용자 위치 기반), Geoproximity(지리적 거리 기반), Latency(낮은 지연 선택), Weighted(비율 기반 분산), Multivalue Answer(다중 IP 반환)가 있습니다. Health Check와 연동하여 비정상 엔드포인트를 자동 제외합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:JUNIOR:AWS_K8S:CLOUD', 'AWS KMS(Key Management Service)의 개념과 활용을 설명해주세요.', '에이더블유에스 KMS(Key Management Service)의 개념과 활용을 설명해주세요.', 'AWS_SECURITY',
 'KMS는 암호화 키를 관리하는 완전관리형 서비스입니다. CMK(Customer Managed Key)로 S3, RDS, EBS 등 AWS 서비스의 데이터를 암호화합니다. 키 정책과 IAM으로 키 사용 권한을 제어합니다. CloudTrail로 키 사용을 감사합니다. Envelope Encryption으로 데이터 키를 CMK로 암호화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 15문항 (CLOUD MID)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS VPC 설계 시 서브넷 구성 전략을 설명해주세요.', '에이더블유에스 브이피씨 설계 시 서브넷 구성 전략을 설명해주세요.', 'AWS_NETWORKING',
 'Public 서브넷(Internet Gateway 연결)에는 로드 밸런서, 배스천 호스트를 배치합니다. Private 서브넷에는 앱 서버와 DB를 배치하고, NAT Gateway로 아웃바운드 인터넷을 허용합니다. 고가용성을 위해 Multi-AZ(최소 2개)로 구성합니다. /16 VPC에서 /24 서브넷으로 분할하는 것이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS ECS Fargate를 활용한 서버리스 컨테이너 배포를 설명해주세요.', '에이더블유에스 이씨에스 Fargate를 활용한 서버리스 컨테이너 배포를 설명해주세요.', 'AWS_COMPUTE',
 'Fargate는 EC2 없이 컨테이너를 실행합니다. Task Definition에서 컨테이너 이미지, CPU/메모리, 환경변수를 정의합니다. Service로 원하는 Task 수를 유지하고, ALB와 통합합니다. IAM Task Role로 AWS 서비스 접근 권한을 부여합니다. VPC Networking 모드로 ENI를 각 Task에 할당합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS DynamoDB의 파티션 키 설계와 핫 파티션 방지를 설명해주세요.', '에이더블유에스 다이나모디비의 파티션 키 설계와 핫 파티션 방지를 설명해주세요.', 'AWS_DATABASE',
 'DynamoDB는 파티션 키로 데이터를 분산합니다. 카디널리티가 높은 속성(userId, orderId)을 파티션 키로 선택해야 핫 파티션을 방지합니다. Write-heavy 패턴에서는 파티션 키에 랜덤 접미사를 추가하거나, 복합 키(파티션 + 정렬)를 사용합니다. DAX로 읽기 캐싱을 추가합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS SQS FIFO와 Standard 큐의 차이를 설명해주세요.', '에이더블유에스 에스큐에스 FIFO와 Standard 큐의 차이를 설명해주세요.', 'AWS_SERVERLESS',
 'Standard 큐는 최소 1회 전달, 순서 미보장, 높은 처리량을 제공합니다. FIFO 큐는 정확히 1회 전달, 순서 보장이지만 처리량이 제한됩니다(초당 3000건, 배치 포함). Message Group ID로 FIFO 내 병렬 처리를 지원합니다. 금융 거래, 순서가 중요한 이벤트는 FIFO를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS Lambda Layer와 컨테이너 이미지 배포의 장단점을 설명해주세요.', '에이더블유에스 람다 Layer와 컨테이너 이미지 배포의 장단점을 설명해주세요.', 'AWS_SERVERLESS',
 'Lambda Layer는 공통 의존성을 최대 5개 레이어로 분리하여 패키지 크기를 줄이고 배포를 빠르게 합니다. 컨테이너 이미지 배포(최대 10GB)는 기존 Docker 워크플로우 재사용, 무거운 ML 모델 탑재, Dockerfile로 환경 표준화에 적합합니다. 250MB 이상이거나 특수 런타임이 필요한 경우 컨테이너가 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS CloudTrail과 Config의 차이와 감사 활용을 설명해주세요.', '에이더블유에스 CloudTrail과 Config의 차이와 감사 활용을 설명해주세요.', 'AWS_SECURITY',
 'CloudTrail은 AWS API 호출 이력(누가, 언제, 무엇을)을 기록합니다. 보안 조사, 규정 준수 감사에 사용합니다. Config는 AWS 리소스의 구성 변경 이력을 추적하고, Rules로 구성 규정 준수를 평가합니다. 보안 이벤트 발생 시 CloudTrail로 API 호출을 추적하고, Config로 변경 전후 설정을 비교합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS Transit Gateway를 활용한 멀티 VPC 네트워킹을 설명해주세요.', '에이더블유에스 Transit Gateway를 활용한 멀티 브이피씨 네트워킹을 설명해주세요.', 'AWS_NETWORKING',
 'Transit Gateway는 VPC와 온프레미스 네트워크를 중앙 허브로 연결합니다. VPC 피어링(N:N 메시)보다 관리가 단순하고 확장성이 있습니다. 라우팅 테이블로 VPC 간 트래픽을 제어하고, VPN/Direct Connect도 연결합니다. 계정 간(Cross-Account) VPC 연결도 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS EKS IRSA(IAM Roles for Service Accounts)를 설명해주세요.', '에이더블유에스 이케이에스 IRSA(IAM Roles for Service Accounts)를 설명해주세요.', 'AWS_COMPUTE',
 'IRSA는 K8s Service Account에 IAM Role을 연결하여 Pod에 최소 권한의 AWS 접근을 부여합니다. 노드 인스턴스 역할로 모든 Pod에 동일 권한을 주는 것보다 세밀한 제어가 가능합니다. OIDC 공급자와 IAM Role trust policy로 구성합니다. eksctl create iamserviceaccount로 간편하게 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS Secrets Manager와 Parameter Store의 차이를 설명해주세요.', '에이더블유에스 Secrets Manager와 Parameter Store의 차이를 설명해주세요.', 'AWS_SECURITY',
 'Secrets Manager는 자동 비밀 교체(RDS, Redshift 기본 지원), 다중 리전 복제, 버전 관리를 제공하며 비용이 더 높습니다. Parameter Store는 계층적 파라미터 저장, 일반 텍스트/SecureString(KMS), 무료 Standard Tier를 제공합니다. 자동 교체가 필요한 DB 비밀번호는 Secrets Manager, 설정값과 인증서는 Parameter Store가 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS CDK(Cloud Development Kit)와 CloudFormation의 차이를 설명해주세요.', '에이더블유에스 CDK(Cloud Development Kit)와 CloudFormation의 차이를 설명해주세요.', 'AWS_ARCHITECTURE',
 'CDK는 TypeScript/Python/Java 등 일반 프로그래밍 언어로 인프라를 정의하고 CloudFormation 템플릿으로 합성됩니다. 조건문, 반복문, 함수를 사용할 수 있어 재사용성이 높습니다. 높은 수준의 Construct(L3)로 복잡한 패턴을 간결하게 표현합니다. CloudFormation을 직접 작성하는 것보다 개발자 친화적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS Kinesis Data Streams와 SQS의 차이를 설명해주세요.', '에이더블유에스 Kinesis Data Streams와 에스큐에스의 차이를 설명해주세요.', 'AWS_SERVERLESS',
 'Kinesis는 실시간 스트리밍 데이터(로그, 클릭스트림, IoT)를 순서 보장하며 여러 소비자가 동시에 소비합니다. 데이터는 최대 7일 보존됩니다. SQS는 분리된 서비스 간 비동기 메시지 전달에 적합하고, 메시지는 소비 후 삭제됩니다. 대용량 실시간 분석은 Kinesis, 작업 큐는 SQS가 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS MSK(Managed Kafka)를 활용한 이벤트 스트리밍 아키텍처를 설명해주세요.', '에이더블유에스 MSK(Managed 카프카)를 활용한 이벤트 스트리밍 아키텍처를 설명해주세요.', 'AWS_SERVERLESS',
 'MSK는 Apache Kafka를 완전 관리형으로 제공합니다. 토픽으로 이벤트를 분류하고, 파티션으로 병렬 처리합니다. 컨슈머 그룹으로 여러 서비스가 독립적으로 이벤트를 처리합니다. MSK Serverless는 용량 관리 없이 사용한 만큼 과금합니다. Debezium으로 DB CDC를 Kafka로 스트리밍합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS Cost Allocation Tags와 예산 관리를 설명해주세요.', '에이더블유에스 Cost Allocation Tags와 예산 관리를 설명해주세요.', 'AWS_ARCHITECTURE',
 'Cost Allocation Tags로 리소스에 팀, 환경, 프로젝트 등 태그를 붙여 비용을 분류합니다. AWS Budgets로 예산 임계값을 설정하고 초과 시 SNS 알림을 받습니다. Cost Explorer로 서비스별, 태그별 비용 트렌드를 분석합니다. 태그 강제를 위해 AWS Config Rules나 SCP로 미태그 리소스 생성을 차단합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS Cognito를 활용한 사용자 인증 구현을 설명해주세요.', '에이더블유에스 Cognito를 활용한 사용자 인증 구현을 설명해주세요.', 'AWS_SECURITY',
 'Cognito User Pool은 사용자 디렉토리로 회원가입/로그인, MFA, 소셜 로그인(Google, Facebook)을 처리합니다. JWT 토큰(idToken, accessToken, refreshToken)을 발급합니다. Identity Pool은 게스트/인증 사용자에게 임시 AWS 자격증명을 부여합니다. API Gateway와 연동하여 JWT 자동 검증이 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:MID:AWS_K8S:CLOUD', 'AWS S3 이벤트 알림과 Lambda를 연동한 서버리스 파이프라인을 설명해주세요.', '에이더블유에스 에스쓰리 이벤트 알림과 람다를 연동한 서버리스 파이프라인을 설명해주세요.', 'AWS_SERVERLESS',
 'S3 이벤트(ObjectCreated, ObjectRemoved)가 발생하면 Lambda, SNS, SQS로 알림을 전송합니다. 파일 업로드 시 Lambda로 자동 처리(이미지 리사이징, 문서 파싱, 데이터 검증)하는 패턴이 일반적입니다. EventBridge를 통해 더 세밀한 이벤트 필터링과 라우팅이 가능합니다. Dead Letter Queue(DLQ)로 처리 실패를 대비합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 15문항 (CLOUD SENIOR)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 Cell-Based Architecture를 구현하는 방법을 설명해주세요.', '에이더블유에스에서 Cell-Based Architecture를 구현하는 방법을 설명해주세요.', 'AWS_ARCHITECTURE',
 'Cell-Based Architecture는 서비스를 독립된 셀(Cell)로 나누어 장애 반경을 제한합니다. 각 셀은 완전한 스택(앱+DB+캐시)을 포함하며, 사용자를 특정 셀에 고정합니다. Route 53과 Shuffle Sharding으로 트래픽을 분산합니다. 하나의 셀 장애가 다른 셀에 영향을 주지 않아 폭발 반경(Blast Radius)을 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 CQRS와 이벤트 소싱 패턴을 구현하는 방법을 설명해주세요.', '에이더블유에스에서 씨큐알에스와 이벤트 소싱 패턴을 구현하는 방법을 설명해주세요.', 'AWS_ARCHITECTURE',
 'CQRS는 쓰기(Command)와 읽기(Query) 모델을 분리합니다. 쓰기는 DynamoDB/RDS에 이벤트를 저장하고, EventBridge/Kinesis로 이벤트를 발행합니다. Lambda가 이벤트를 소비하여 읽기 전용 뷰(OpenSearch, Redshift)를 갱신합니다. 이벤트 소싱은 상태 변경 이력 전체를 이벤트로 저장하여 시간 여행 쿼리가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 실시간 대용량 데이터 파이프라인을 설계하는 방법을 설명해주세요.', '에이더블유에스에서 실시간 대용량 데이터 파이프라인을 설계하는 방법을 설명해주세요.', 'AWS_ARCHITECTURE',
 '수집: Kinesis Data Streams(초당 수백만 이벤트)로 실시간 수집합니다. 처리: Kinesis Data Analytics(SQL/Flink)로 실시간 분석하거나, Lambda로 개별 이벤트를 처리합니다. 저장: S3(원시 데이터), Redshift(분석), DynamoDB(저지연 조회)에 적재합니다. Firehose로 자동으로 S3/Redshift로 배치 전달합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS Outposts와 Local Zones의 활용 사례를 설명해주세요.', '에이더블유에스 Outposts와 Local Zones의 활용 사례를 설명해주세요.', 'AWS_COMPUTE',
 'Outposts는 AWS 인프라를 온프레미스에 설치하여 동일한 AWS API로 로컬에서 실행합니다. 데이터 레지던시, 초저지연이 필요한 온프레미스 워크로드에 적합합니다. Local Zones는 대도시 근처에 AWS 인프라를 확장하여 ms 단위 지연을 제공합니다. 미디어 렌더링, 게임, 라이브 스트리밍에 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 ML 모델 서빙 아키텍처를 설계하는 방법을 설명해주세요.', '에이더블유에스에서 ML 모델 서빙 아키텍처를 설계하는 방법을 설명해주세요.', 'AWS_ARCHITECTURE',
 'SageMaker Endpoint는 완전관리형 ML 모델 서빙으로 자동 스케일링을 지원합니다. 배치 추론은 SageMaker Batch Transform을 사용합니다. Lambda + EFS에 모델을 탑재하면 서버리스 추론이 가능합니다. EKS + Nvidia GPU 노드로 자체 서빙 인프라를 구성할 수도 있습니다. A/B 테스트는 Endpoint Production Variant로 트래픽을 분할합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 Database per Service 패턴과 데이터 일관성 관리를 설명해주세요.', '에이더블유에스에서 Database per Service 패턴과 데이터 일관성 관리를 설명해주세요.', 'AWS_ARCHITECTURE',
 '마이크로서비스 패턴에서 각 서비스가 독립 DB를 소유합니다. 서비스 간 데이터 일관성은 Saga 패턴(Choreography: EventBridge, Orchestration: Step Functions)으로 관리합니다. 분산 트랜잭션 대신 최종 일관성(Eventual Consistency)을 수용합니다. 보상 트랜잭션(Compensating Transaction)으로 실패 시 롤백합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 API Gateway의 고급 기능(Throttling, Caching, Custom Authorizer)을 설명해주세요.', '에이더블유에스에서 에이피아이 Gateway의 고급 기능(Throttling, Caching, Custom Authorizer)을 설명해주세요.', 'AWS_SERVERLESS',
 'Throttling은 계정/단계/메서드별 초당 요청 수(Rate)와 버스트를 제한합니다. Usage Plan과 API Key로 클라이언트별 쿼터를 관리합니다. Caching(최대 3600초)으로 반복 요청의 Lambda 호출을 줄입니다. Lambda Authorizer는 커스텀 인증(JWT, OAuth)로 요청을 검증하고 IAM 정책을 반환합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 Zero Trust 보안 모델을 구현하는 방법을 설명해주세요.', '에이더블유에스에서 Zero Trust 보안 모델을 구현하는 방법을 설명해주세요.', 'AWS_SECURITY',
 'Zero Trust는 "절대 신뢰하지 말고 항상 검증하라"는 원칙입니다. IAM의 최소 권한, VPC PrivateLink(서비스 노출 최소화), mTLS(서비스 간 인증), GuardDuty(위협 탐지), Inspector(취약점 스캔)를 조합합니다. AWS Verified Access는 VPN 없이 애플리케이션 레벨 신원 검증을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 GraphQL API(AppSync)를 활용한 아키텍처를 설명해주세요.', '에이더블유에스에서 그래프큐엘 에이피아이(AppSync)를 활용한 아키텍처를 설명해주세요.', 'AWS_SERVERLESS',
 'AppSync는 완전관리형 GraphQL 서비스로, 실시간 구독(WebSocket), 오프라인 지원(Amplify)이 내장됩니다. Resolver는 DynamoDB, Lambda, HTTP 엔드포인트에 연결됩니다. VTL(Velocity Template Language) 또는 JavaScript로 매핑 템플릿을 작성합니다. Cognito, IAM, API Key로 인증하고 세밀한 필드 레벨 권한을 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS SageMaker MLOps 파이프라인을 구축하는 방법을 설명해주세요.', '에이더블유에스 SageMaker MLOps 파이프라인을 구축하는 방법을 설명해주세요.', 'AWS_ARCHITECTURE',
 'SageMaker Pipelines로 데이터 전처리→학습→평가→배포 파이프라인을 코드로 정의합니다. Model Registry로 모델 버전과 메타데이터를 관리하고, 승인 워크플로를 구성합니다. Feature Store로 특성을 온/오프라인에서 일관되게 제공합니다. Model Monitor로 데이터/모델 드리프트를 감지하고 재학습을 트리거합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 고가용성 캐싱 전략(Multi-Layer Cache)을 설명해주세요.', '에이더블유에스에서 고가용성 캐싱 전략(Multi-Layer Cache)을 설명해주세요.', 'AWS_ARCHITECTURE',
 'L1: CDN(CloudFront) - 정적 콘텐츠와 API 응답 캐싱. L2: API Gateway 캐싱 - 엔드포인트별 TTL 설정. L3: ElastiCache Redis - DB 앞단 캐시, 세션, 집계 데이터. L4: DAX - DynamoDB 전용 인메모리 캐시. 캐시 무효화는 TTL, 이벤트 기반(데이터 변경 시 즉시 삭제)을 조합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 서버리스 아키텍처의 관측성(Observability)을 구축하는 방법을 설명해주세요.', '에이더블유에스에서 서버리스 아키텍처의 관측성(Observability)을 구축하는 방법을 설명해주세요.', 'AWS_SERVERLESS',
 'AWS X-Ray로 Lambda, API Gateway, DynamoDB 간 분산 추적을 구성합니다. CloudWatch Lambda Insights로 CPU, 메모리, 네트워크 메트릭을 수집합니다. 구조화된 로그(JSON)를 CloudWatch Logs로 전송하고, Log Insights로 분석합니다. CloudWatch Contributor Insights로 고트래픽 패턴을 식별합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS 글로벌 가속기(Global Accelerator)와 CloudFront의 차이를 설명해주세요.', '에이더블유에스 글로벌 가속기(Global Accelerator)와 클라우드프론트의 차이를 설명해주세요.', 'AWS_NETWORKING',
 'CloudFront는 L7 HTTP/HTTPS CDN으로 콘텐츠 캐싱에 특화됩니다. Global Accelerator는 L4 TCP/UDP를 anycast IP로 AWS 백본을 통해 라우팅하여 지연을 줄입니다. 캐싱이 없고 모든 요청이 오리진에 전달되지만 일관된 성능을 제공합니다. 게임, 금융 거래, IoT 등 저지연 비HTTP 프로토콜에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 Bedrock을 활용한 생성형 AI 애플리케이션 아키텍처를 설명해주세요.', '에이더블유에스에서 Bedrock을 활용한 생성형 AI 애플리케이션 아키텍처를 설명해주세요.', 'AWS_ARCHITECTURE',
 'Bedrock은 Claude, Llama, Titan 등 FM(Foundation Model)을 API로 제공합니다. Knowledge Base로 RAG(Retrieval-Augmented Generation)를 구현하고 기업 데이터를 활용합니다. Agents로 멀티스텝 작업을 자동화합니다. S3+OpenSearch Serverless로 벡터 스토어를 구성합니다. VPC 엔드포인트로 프라이빗 통신을 보장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('DEVOPS:SENIOR:AWS_K8S:CLOUD', 'AWS에서 멀티 계정 보안 자동화(CSPM) 전략을 설명해주세요.', '에이더블유에스에서 멀티 계정 보안 자동화(CSPM) 전략을 설명해주세요.', 'AWS_SECURITY',
 'CSPM(Cloud Security Posture Management)은 멀티 계정 보안 설정을 자동으로 평가합니다. Security Hub는 모든 계정의 발견사항을 중앙 집계하고 FSBP(기본 보안 모범 사례)로 자동 평가합니다. Delegated Administrator 계정으로 Organizations 전체를 관리합니다. EventBridge + Lambda로 정책 위반 시 자동 수정합니다.',
 'MODEL_ANSWER', TRUE, NOW());
