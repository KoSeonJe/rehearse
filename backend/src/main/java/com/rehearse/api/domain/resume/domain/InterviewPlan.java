package com.rehearse.api.domain.resume.domain;

import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "interview_plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterviewPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interview_id", unique = true)
    private Long interviewId;

    @Column(name = "session_plan_id", nullable = false)
    private String sessionPlanId;

    @Column(name = "duration_hint_min", nullable = false)
    private int durationHintMin;

    @Column(name = "total_projects", nullable = false)
    private int totalProjects;

    @Convert(converter = ProjectPlanListJsonConverter.class)
    @Column(name = "plan_json", columnDefinition = "JSON", nullable = false)
    private List<ProjectPlan> projectPlans;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InterviewPlan(String sessionPlanId, int durationHintMin, List<ProjectPlan> projectPlans) {
        if (sessionPlanId == null || sessionPlanId.isBlank()) {
            throw new IllegalArgumentException("sessionPlanId 는 필수입니다.");
        }
        if (durationHintMin <= 0) {
            throw new IllegalArgumentException("durationHintMin 은 0 보다 커야 합니다. durationHintMin=" + durationHintMin);
        }
        if (projectPlans == null) {
            throw new IllegalArgumentException("projectPlans 는 필수입니다.");
        }
        validateAscendingPriority(projectPlans);
        this.sessionPlanId = sessionPlanId;
        this.durationHintMin = durationHintMin;
        this.totalProjects = projectPlans.size();
        this.projectPlans = List.copyOf(projectPlans);
    }

    public void assignToInterview(Long interviewId) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 는 필수입니다.");
        }
        if (this.interviewId != null) {
            throw new BusinessException(ResumeErrorCode.INTERVIEW_PLAN_ALREADY_ASSIGNED);
        }
        this.interviewId = interviewId;
    }

    public String sessionPlanId() {
        return sessionPlanId;
    }

    public int durationHintMin() {
        return durationHintMin;
    }

    public int totalProjects() {
        return totalProjects;
    }

    public List<ProjectPlan> projectPlans() {
        return projectPlans;
    }

    private static void validateAscendingPriority(List<ProjectPlan> plans) {
        for (int i = 1; i < plans.size(); i++) {
            if (plans.get(i).priority() <= plans.get(i - 1).priority()) {
                throw new IllegalArgumentException(
                        "projectPlans 의 priority 는 중복 없이 오름차순이어야 합니다."
                );
            }
        }
    }
}
