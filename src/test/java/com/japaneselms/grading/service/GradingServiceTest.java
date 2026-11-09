package com.japaneselms.grading.service;

import com.japaneselms.entity.*;
import com.japaneselms.grading.dto.GradeForm;
import com.japaneselms.grading.repository.GradingRepository;
import com.japaneselms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradingServiceTest {
    private GradingRepository submissions;
    private UserRepository users;
    private GradingService service;
    private Submission submission;

    @BeforeEach
    void setUp() {
        submissions = mock(GradingRepository.class);
        users = mock(UserRepository.class);
        service = new GradingService(submissions, users);
        User instructor = new User();
        instructor.setUserId(1L);
        instructor.setRole("INSTRUCTOR");
        instructor.setActive(true);
        when(users.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.of(instructor));
        Course course = new Course();
        course.setInstructor(instructor);
        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        submission = new Submission();
        submission.setAssignment(assignment);
        when(submissions.findDetailed(10L)).thenReturn(Optional.of(submission));
    }

    @Test
    void instructorCanGradeOwnCourse() {
        GradeForm form = new GradeForm();
        form.setGrade(new BigDecimal("8.5"));
        form.setFeedback("  Tốt  ");
        service.saveGrade("teacher@example.com", 10L, form);
        assertEquals(new BigDecimal("8.5"), submission.getGrade());
        assertEquals("Tốt", submission.getFeedback());
        assertNotNull(submission.getGradedAt());
        verify(submissions).save(submission);
    }

    @Test
    void instructorCannotGradeAnotherCourse() {
        User other = new User();
        other.setUserId(2L);
        submission.getAssignment().getCourse().setInstructor(other);
        assertThrows(AccessDeniedException.class,
                () -> service.gradingDetail("teacher@example.com", 10L));
        verify(submissions, never()).save(any());
    }

    @Test
    void invalidGradeIsRejected() {
        GradeForm form = new GradeForm();
        form.setGrade(new BigDecimal("10.5"));
        assertThrows(ResponseStatusException.class,
                () -> service.saveGrade("teacher@example.com", 10L, form));
        verify(submissions, never()).save(any());
    }
}
