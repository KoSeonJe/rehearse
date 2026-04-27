package com.rehearse.api.domain.resume.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "resume_skeleton")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ResumeSkeletonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interview_id", nullable = false)
    private Long interviewId;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "candidate_level", nullable = false, length = 16)
    private String candidateLevel;

    @Column(name = "target_domain", length = 32)
    private String targetDomain;

    @Column(name = "skeleton_json", nullable = false, columnDefinition = "JSON")
    private String skeletonJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ResumeSkeletonEntity(Long interviewId, String fileHash, String candidateLevel,
                                String targetDomain, String skeletonJson) {
        this.interviewId = interviewId;
        this.fileHash = fileHash;
        this.candidateLevel = candidateLevel;
        this.targetDomain = targetDomain;
        this.skeletonJson = skeletonJson;
    }
}
