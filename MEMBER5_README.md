# MF-05 — Grading & Basic Learning Results

## Phạm vi
- S19 `/instructor/grading` — Instructor xem bài nộp, lọc tất cả/chưa chấm/đã chấm, phân trang.
- S20 `/instructor/grading/{id}` — Instructor chấm/sửa điểm (0–10, tối đa 1 chữ số thập phân) và phản hồi.
- S21 `/student/grades` — Student chỉ xem điểm đã chấm của chính mình.
- S22 `/instructor/results`, `/admin/results` — Instructor xem bài của khóa mình; Admin xem toàn bộ, lọc bằng courseId.
- S23 `/instructor/results/{id}`, `/student/results/{id}`, `/admin/results/{id}` — Xem chi tiết theo quyền.

## Các file đã thêm
- `src/main/java/com/japaneselms/grading/controller/GradingController.java`
- `src/main/java/com/japaneselms/grading/service/GradingService.java`
- `src/main/java/com/japaneselms/grading/dto/GradeForm.java`
- `src/main/java/com/japaneselms/grading/repository/GradingRepository.java`
- `src/main/resources/templates/grading/*.html` (5 file)

## File đã chỉnh nhẹ
- `src/main/resources/templates/fragments/navbar.html` — thêm menu tới trang điểm và chấm bài.

**Không chỉnh:** `User`, `Course`, `Assignment`, `Submission`, `SecurityConfig`, `pom.xml`, `application.properties`, và repository placeholder của thành viên khác.

## Luồng kỹ thuật
Controller -> Service -> GradingRepository -> bảng submissions (các cột grade, feedback, graded_by, graded_at sẵn có). `@Transactional` để lấy dữ liệu và lưu điểm. `@Valid` kiểm tra điểm, feedback tối đa 2000 ký tự; Spring Security phân quyền theo `/instructor`, `/student`, `/admin`, Service kiểm tra thêm owner thực tế.

## Cách chạy trong IntelliJ IDEA
1. Open thư mục `Japanese_LMS-main`, đợi Maven đồng bộ dependencies, chọn JDK 21.
2. SQL Server chạy ở cổng 1433; chuẩn bị database `japanese_lms` và schema phù hợp các Entity. Cấu hình tài khoản SQL Server trong `application.properties` ở máy của bạn (không commit mật khẩu thật).
3. Chạy `JapaneseLmsApplication`. Mở `http://localhost:8080/login`.
4. Cần dữ liệu thực do MF-02/04 tạo: Course, Assignment, Submission; chưa có bài nộp thì các danh sách trống là đúng. Đăng nhập Instructor phụ trách Course để chấm; Student để xem điểm; Admin để xem báo cáo.
5. Nếu schema SQL Server còn thiếu bảng/column, vì dự án gốc đặt `spring.jpa.hibernate.ddl-auto=validate`, ứng dụng sẽ không tự tạo bảng. Nhóm phải đồng bộ schema đúng trước khi chạy.

## Test thủ công nên làm
1. Instructor được phân công mở `/instructor/grading`: chỉ thấy submissions thuộc course của mình.
2. Chấm 8.5, để feedback: lưu rồi reload; grade, feedback, gradedBy, gradedAt vẫn đúng.
3. Điểm -1, 11, 8.55, trống: form báo validation; không ghi database.
4. Instructor khác thay ID trong URL và thử POST: phải bị từ chối.
5. Student xem `/student/grades`: chỉ thấy bài đã chấm của chính mình; sửa ID để xem điểm người khác phải bị từ chối.
6. Admin xem `/admin/results`: có phân trang/lọc theo courseId; không có form chấm điểm.
7. POST thiếu CSRF token phải bị Spring Security từ chối.
8. Khi MF-04 chưa có upload/download, MF-05 hiển thị tên tệp, chưa tự tạo endpoint tải tệp để tránh lộ file và xung đột với Member 4.

## Lưu ý tích hợp
- MF-05 phụ thuộc dữ liệu Submission tạo bởi Member 4; module này KHÔNG tạo bài nộp và KHÔNG tạo dữ liệu giả.
- Các link từ navbar đã được thêm, nhưng link Admin nằm trong dropdown account thay vì sidebar của nhóm.
- Chưa kiểm thử được compile/chạy SQL Server trong môi trường tạo bản này do Maven wrapper cần tải Maven từ Internet nhưng môi trường không tải được. Không tuyên bố đã chạy thành công.
