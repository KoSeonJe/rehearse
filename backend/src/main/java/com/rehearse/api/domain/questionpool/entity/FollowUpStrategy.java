package com.rehearse.api.domain.questionpool.entity;

public enum FollowUpStrategy {
    PREPARED,   // 사전 생성 가능 — 정답이 있는 기술 질문의 꼬리 질문
    REALTIME    // 실시간 생성 필요 — 사용자 답변에 따라 달라지는 꼬리 질문
}
