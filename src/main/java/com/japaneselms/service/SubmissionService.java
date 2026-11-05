package com.japaneselms.service;

import com.japaneselms.dto.request.SubmissionCreateRequest;
import com.japaneselms.dto.response.SubmissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubmissionService {

    SubmissionResponse submitAssignment(SubmissionCreateRequest request, Long studentId);

    SubmissionResponse getSubmissionDetail(Long submissionId, Long currentUserId);

    SubmissionResponse getStudentSubmission(Long assignmentId, Long studentId);

    Page<SubmissionResponse> getSubmissionsForAssignment(
            Long assignmentId,
            String gradingStatus,
            Long currentUserId,
            Pageable pageable
    );

    org.springframework.core.io.Resource getSubmissionFileResource(Long submissionId, Long currentUserId);
}
