
package com.japaneselms.course.controller;

import com.japaneselms.course.service.CourseManagementService;
import com.japaneselms.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller hien thi anh bia khoa hoc.
 */
@Controller
public class CourseImageController {

    private final CourseManagementService courseManagementService;

    public CourseImageController(
            CourseManagementService courseManagementService) {
        this.courseManagementService = courseManagementService;
    }

    @GetMapping("/files/course-images/{courseId}")
    public ResponseEntity<Resource> courseImage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId) {

        return courseManagementService.getCourseImageResponse(
                currentUser, courseId);
    }
}
