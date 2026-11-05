package com.japaneselms.service;

import com.japaneselms.dto.request.AssignmentSaveRequest;
import com.japaneselms.dto.response.AssignmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface AssignmentService {

    AssignmentResponse createAssignment(AssignmentSaveRequest request, Long currentUserId);

    AssignmentResponse updateAssignment(Long assignmentId, AssignmentSaveRequest request, Long currentUserId);

    AssignmentResponse getAssignmentDetail(Long assignmentId, Long currentUserId);

    AssignmentSaveRequest getAssignmentForEdit(Long assignmentId, Long currentUserId);

    void deactivateAssignment(Long assignmentId, Long currentUserId);

    void activateAssignment(Long assignmentId, Long currentUserId);

    Page<AssignmentResponse> getAssignmentsByCourseForInstructor(
            Long courseId,
            String keyword,
            String status,
            Long currentUserId,
            Pageable pageable
    );

    Page<AssignmentResponse> getAssignmentsByCourseForStudent(
            Long courseId,
            Long currentUserId,
            Pageable pageable
    );

    List<AssignmentResponse> getAllActiveAssignmentsByCourse(Long courseId);
}
