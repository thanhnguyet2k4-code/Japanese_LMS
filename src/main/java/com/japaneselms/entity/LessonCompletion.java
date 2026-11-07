package com.japaneselms.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lesson_completions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_completion_student_lesson",
                        columnNames = {"student_id", "lesson_id"}
                )
        }
)
public class LessonCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "completion_id")
    private Long completionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public LessonCompletion() {
    }

    @PrePersist
    protected void onCreate() {
        if (completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    public Long getCompletionId() {
        return completionId;
    }

    public void setCompletionId(Long completionId) {
        this.completionId = completionId;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
