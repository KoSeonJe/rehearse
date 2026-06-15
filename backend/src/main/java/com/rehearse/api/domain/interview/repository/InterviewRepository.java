package com.rehearse.api.domain.interview.repository;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @EntityGraph(attributePaths = {"interviewTypes", "csSubTopics"})
    @Query("SELECT i FROM Interview i WHERE i.id = :id")
    Optional<Interview> findByIdWithElementCollections(@Param("id") Long id);

    List<Interview> findByStatus(InterviewStatus status);

    @EntityGraph(attributePaths = {"interviewTypes", "csSubTopics"})
    @Query("SELECT i FROM Interview i WHERE i.publicId = :publicId")
    Optional<Interview> findByPublicId(@Param("publicId") String publicId);

    @Query(value = "SELECT i FROM Interview i WHERE i.userId = :userId ORDER BY i.createdAt DESC, i.id DESC",
            countQuery = "SELECT COUNT(i) FROM Interview i WHERE i.userId = :userId")
    Page<Interview> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT qs.interview.id, COUNT(qa)
            FROM QuestionAnswer qa
            JOIN qa.question q
            JOIN q.questionSet qs
            WHERE qs.interview.id IN :interviewIds
              AND (qs.fileMetadata IS NULL
                   OR qs.fileMetadata.status <> com.rehearse.api.domain.file.entity.FileStatus.FAILED)
            GROUP BY qs.interview.id
            """)
    List<Object[]> countAnswersByInterviewIds(@Param("interviewIds") List<Long> interviewIds);

    @Query("SELECT COUNT(i) FROM Interview i WHERE i.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(i) FROM Interview i WHERE i.userId = :userId AND i.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") InterviewStatus status);

    @Query("SELECT COUNT(i) FROM Interview i WHERE i.userId = :userId AND i.createdAt >= :from")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("from") LocalDateTime from);
}
