ALTER TABLE resume_skeleton
    ADD CONSTRAINT uk_resume_skeleton_interview UNIQUE (interview_id);
