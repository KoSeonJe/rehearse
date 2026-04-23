package com.rehearse.api.domain.questionset.repository;

import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.ConvertStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuestionSetAnalysisRepository extends JpaRepository<QuestionSetAnalysis, Long> {

    Optional<QuestionSetAnalysis> findByQuestionSetId(Long questionSetId);

    List<QuestionSetAnalysis> findByAnalysisStatusAndUpdatedAtBefore(
            AnalysisStatus status, LocalDateTime threshold);

    List<QuestionSetAnalysis> findByAnalysisStatusInAndUpdatedAtBefore(
            List<AnalysisStatus> statuses, LocalDateTime threshold);

    List<QuestionSetAnalysis> findByConvertStatusAndUpdatedAtBefore(
            ConvertStatus status, LocalDateTime threshold);

    long countByQuestionSetInterviewIdAndAnalysisStatusIn(
            Long interviewId, List<AnalysisStatus> statuses);

    @Modifying
    @Query("DELETE FROM QuestionSetAnalysis a WHERE a.questionSet.interview.id = :interviewId")
    void deleteAllByInterviewId(@Param("interviewId") Long interviewId);
}
