-- ==============================================================
-- DỮ LIỆU MẪU TEST TOÀN DIỆN CHO JAPANESE LMS
-- Sử dụng: Mở Query mới trong SSMS trên database [japanese_lms] và Execute (F5)
-- ==============================================================

USE [japanese_lms];
GO

-- 1. XÓA DỮ LIỆU CŨ THEO ĐÚNG THỨ TỰ RÀNG BUỘC KHÓA NGOẠI
DELETE FROM dbo.submissions;
DELETE FROM dbo.learning_materials;
DELETE FROM dbo.assignments;
DELETE FROM dbo.lessons;
DELETE FROM dbo.enrollments;
DELETE FROM dbo.courses;
DELETE FROM dbo.users;

-- Reset lại cột IDENTITY về 0
DBCC CHECKIDENT ('dbo.submissions', RESEED, 0);
DBCC CHECKIDENT ('dbo.learning_materials', RESEED, 0);
DBCC CHECKIDENT ('dbo.assignments', RESEED, 0);
DBCC CHECKIDENT ('dbo.lessons', RESEED, 0);
DBCC CHECKIDENT ('dbo.enrollments', RESEED, 0);
DBCC CHECKIDENT ('dbo.courses', RESEED, 0);
DBCC CHECKIDENT ('dbo.users', RESEED, 0);
GO

-- ==============================================================
-- 2. BẢNG USERS (Mật khẩu mặc định đều là: 123456)
-- Hash BCrypt: $2a$10$wK1WvJmGq7p9E9K.pQdDPe45K2NlP.l28Z7p3d/Vz1f7A9.f8k4Xq
-- ==============================================================
INSERT INTO dbo.users (full_name, email, password_hash, role, account_status, phone, avatar_path, reviewed_by, reviewed_at, review_note, is_active, created_at, updated_at)
VALUES 
-- ID 1: Quản trị viên
(N'Admin Quản Trị Viên', 'admin@lms.com', '$2a$10$wK1WvJmGq7p9E9K.pQdDPe45K2NlP.l28Z7p3d/Vz1f7A9.f8k4Xq', 'ADMIN', 'APPROVED', '0901234567', NULL, NULL, NULL, NULL, 1, SYSDATETIME(), SYSDATETIME()),

-- ID 2: Giảng viên 1
(N'Thầy Takahashi Kenji', 'takahashi@lms.com', '$2a$10$wK1WvJmGq7p9E9K.pQdDPe45K2NlP.l28Z7p3d/Vz1f7A9.f8k4Xq', 'INSTRUCTOR', 'APPROVED', '0912345678', NULL, 1, SYSDATETIME(), N'Tài khoản giảng viên N5-N4', 1, SYSDATETIME(), SYSDATETIME()),

-- ID 3: Giảng viên 2
(N'Cô Yamada Sakura', 'yamada@lms.com', '$2a$10$wK1WvJmGq7p9E9K.pQdDPe45K2NlP.l28Z7p3d/Vz1f7A9.f8k4Xq', 'INSTRUCTOR', 'APPROVED', '0923456789', NULL, 1, SYSDATETIME(), N'Tài khoản giảng viên N3', 1, SYSDATETIME(), SYSDATETIME()),

-- ID 4: Học viên 1
(N'Nguyễn Văn An', 'an.student@lms.com', '$2a$10$wK1WvJmGq7p9E9K.pQdDPe45K2NlP.l28Z7p3d/Vz1f7A9.f8k4Xq', 'STUDENT', 'APPROVED', '0934567890', NULL, 1, SYSDATETIME(), N'Đã duyệt hồ sơ', 1, SYSDATETIME(), SYSDATETIME()),

-- ID 5: Học viên 2
(N'Trần Thị Bình', 'binh.student@lms.com', '$2a$10$wK1WvJmGq7p9E9K.pQdDPe45K2NlP.l28Z7p3d/Vz1f7A9.f8k4Xq', 'STUDENT', 'APPROVED', '0945678901', NULL, 1, SYSDATETIME(), N'Đã duyệt hồ sơ', 1, SYSDATETIME(), SYSDATETIME()),

-- ID 6: Học viên 3
(N'Lê Hoàng Cường', 'cuong.student@lms.com', '$2a$10$wK1WvJmGq7p9E9K.pQdDPe45K2NlP.l28Z7p3d/Vz1f7A9.f8k4Xq', 'STUDENT', 'APPROVED', '0956789012', NULL, 1, SYSDATETIME(), N'Đã duyệt hồ sơ', 1, SYSDATETIME(), SYSDATETIME()),

-- ID 7: Học viên 4
(N'Phạm Minh Đức', 'duc.student@lms.com', '$2a$10$wK1WvJmGq7p9E9K.pQdDPe45K2NlP.l28Z7p3d/Vz1f7A9.f8k4Xq', 'STUDENT', 'APPROVED', '0967890123', NULL, 1, SYSDATETIME(), N'Đã duyệt hồ sơ', 1, SYSDATETIME(), SYSDATETIME());
GO

-- ==============================================================
-- 3. BẢNG COURSES (Khóa học tiếng Nhật)
-- ==============================================================
INSERT INTO dbo.courses (course_name, description, jlpt_level, instructor_id, course_status, created_at, updated_at)
VALUES
-- ID 1
(N'Tiếng Nhật Sơ Cấp N5 - Minna no Nihongo I', N'Khóa học tiếng Nhật dành cho người mới bắt đầu từ con số 0. Làm quen bảng chữ cái Hiragana, Katakana, từ vựng và ngữ pháp 25 bài đầu giáo trình Minna no Nihongo.', 'N5', 2, 'ACTIVE', SYSDATETIME(), SYSDATETIME()),

-- ID 2
(N'Tiếng Nhật Sơ Trung Cấp N4 - Minna no Nihongo II', N'Nâng cao trình độ ngữ pháp và từ vựng từ bài 26 đến bài 50, làm quen thể khả năng, thể bị động, sai khiến và thể tôn kính ngữ.', 'N4', 2, 'ACTIVE', SYSDATETIME(), SYSDATETIME()),

-- ID 3
(N'Luyện Thi JLPT N3 Cấp Tốc', N'Khóa học chuyên sâu luyện đề thi JLPT N3, giải đề Đọc hiểu, Nghe hiểu và Kanji trung cấp.', 'N3', 3, 'ACTIVE', SYSDATETIME(), SYSDATETIME());
GO

-- ==============================================================
-- 4. BẢNG ENROLLMENTS (Học viên ghi danh khóa học)
-- ==============================================================
INSERT INTO dbo.enrollments (student_id, course_id, enrolled_at)
VALUES
(4, 1, DATEADD(day, -10, SYSDATETIME())), -- An ghi danh Khóa N5
(4, 2, DATEADD(day, -5, SYSDATETIME())),  -- An ghi danh Khóa N4
(5, 1, DATEADD(day, -8, SYSDATETIME())),  -- Bình ghi danh Khóa N5
(6, 1, DATEADD(day, -7, SYSDATETIME())),  -- Cường ghi danh Khóa N5
(7, 2, DATEADD(day, -3, SYSDATETIME()));  -- Đức ghi danh Khóa N4
GO

-- ==============================================================
-- 5. BẢNG LESSONS (Bài học của khóa học)
-- ==============================================================
INSERT INTO dbo.lessons (course_id, lesson_title, lesson_description, lesson_order, lesson_status, created_at, updated_at)
VALUES
-- Khóa 1 (N5)
(1, N'Bài 1: Giới thiệu bản thân & Đại từ nhân xưng', N'Ngữ pháp: N1 は N2 です, N1 は N2 じゃありません, N1 は N2 ですか.', 1, 'ACTIVE', SYSDATETIME(), SYSDATETIME()),
(1, N'Bài 2: Đồ vật xung quanh ta (これ、それ、あれ)', N'Từ vựng về đồ dùng học tập, sách vở. Ngữ pháp xác định đồ vật và sở hữu.', 2, 'ACTIVE', SYSDATETIME(), SYSDATETIME()),
(1, N'Bài 3: Hỏi đường & Nơi chốn (ここ、そこ、あそこ)', N'Từ vựng địa điểm, phương hướng, cách hỏi giá tiền và phòng học.', 3, 'ACTIVE', SYSDATETIME(), SYSDATETIME());
GO

-- ==============================================================
-- 6. BẢNG ASSIGNMENTS (Bài tập - Trọng tâm kiểm thử S14, S15, S16)
-- ==============================================================
INSERT INTO dbo.assignments (course_id, lesson_id, title, instructions, due_at, created_by, assignment_status, created_at, updated_at)
VALUES
-- Bài tập 1: Đang mở (ACTIVE), gắn với Bài 1, hạn nộp còn xa
(1, 1, N'Bài tập 1: Luyện dịch câu giới thiệu bản thân Minna Bài 1', 
 N'Học viên hãy dịch các câu sau sang tiếng Nhật bằng bảng chữ cái Hiragana/Kanji:
1. Tôi là sinh viên trường Đại học Hà Nội.
2. Anh Tanaka không phải là bác sĩ, anh ấy là kỹ sư máy tính.
3. Chị Sakura có phải là giáo viên không? - Vâng, đúng vậy.
Học viên có thể nhập trực tiếp câu trả lời vào ô văn bản hoặc đính kèm file làm bài (PDF/DOCX).', 
 DATEADD(day, 7, SYSDATETIME()), 2, 'ACTIVE', DATEADD(day, -6, SYSDATETIME()), SYSDATETIME()),

-- Bài tập 2: ĐÃ QUÁ HẠN NỘP (để kiểm tra badge "Đã quá hạn" trên màn hình S14/S16)
(1, 1, N'Bài tập 2: Luyện viết 20 chữ Kanji cơ bản tuần 1', 
 N'Học viên viết 20 chữ Hán tự (Nhất, Nhị, Tam, Tứ, Ngũ, Lục, Thất, Bát, Cửu, Thập, Nhật, Nguyệt...) ra giấy kẻ ô, sau đó chụp ảnh (PNG/JPG) hoặc scan thành file PDF tải lên nộp bài.', 
 DATEADD(day, -2, SYSDATETIME()), 2, 'ACTIVE', DATEADD(day, -10, SYSDATETIME()), SYSDATETIME()),

-- Bài tập 3: Gắn với Bài 2, không giới hạn hạn nộp (due_at = NULL)
(1, 2, N'Bài tập 3: Phân biệt cách dùng これ, それ, あれ', 
 N'Chọn từ thích hợp điền vào chỗ trống và giải thích ngắn gọn tại sao chọn từ đó. Hạn nộp bài linh hoạt, khuyến khích nộp sớm trước buổi học tiếp theo.', 
 NULL, 2, 'ACTIVE', DATEADD(day, -3, SYSDATETIME()), SYSDATETIME()),

-- Bài tập 4: Bài tập chung cho cả khóa học (lesson_id = NULL)
(1, NULL, N'Bài tập tổng hợp: Viết đoạn văn ngắn về gia đình bạn', 
 N'Viết đoạn văn từ 5 - 8 câu tiếng Nhật giới thiệu các thành viên trong gia đình bạn (tên, tuổi, nghề nghiệp). Sử dụng ngữ pháp bài 1 và bài 2.', 
 DATEADD(day, 14, SYSDATETIME()), 2, 'ACTIVE', DATEADD(day, -1, SYSDATETIME()), SYSDATETIME()),

-- Bài tập 5: Bài tập đã ngừng hoạt động (INACTIVE) để test lọc trên S14/S15
(1, 1, N'Bài tập bổ trợ cũ (Đã đóng)', 
 N'Bài tập này giảng viên đã tạm ngưng nhận bài.', 
 DATEADD(day, -20, SYSDATETIME()), 2, 'INACTIVE', DATEADD(day, -30, SYSDATETIME()), SYSDATETIME());
GO

-- ==============================================================
-- 7. BẢNG SUBMISSIONS (Bài nộp của sinh viên - Test S16, S17, S18)
-- ==============================================================
INSERT INTO dbo.submissions (assignment_id, student_id, response_text, storage_path, original_file_name, file_type, submitted_at, grade, feedback, graded_by, graded_at)
VALUES
-- Bài nộp 1: Nguyễn Văn An (id=4) nộp Bài tập 1 -> ĐÃ ĐƯỢC CHẤM ĐIỂM (9.0 điểm)
(1, 4, 
 N'1. わたしは ハノイだいがくの がくせいです。(Watashi wa Hanoi daigaku no gakusei desu)
2. タナカさんは いしゃじゃありません。エンジニアです。(Tanaka-san wa isha ja arimasen. Enjinia desu)
3. さくらさんは せんせいですか。はい、そうです。(Sakura-san wa sensei desu ka. Hai, sou desu)', 
 'uploads/submissions/sample_an_assignment1.pdf', N'BaiLam_NguyenVanAn.pdf', 'PDF', 
 DATEADD(day, -4, SYSDATETIME()), 9.0, 
 N'Em dịch rất tốt và chính xác ngữ pháp は và です. Chú ý viết hoa tên riêng Tanaka và dùng đúng Katakana cho chữ Engineer (エンジニア). Phát huy nhé!', 
 2, DATEADD(day, -2, SYSDATETIME())),

-- Bài nộp 2: Trần Thị Bình (id=5) nộp Bài tập 1 -> CHƯA CHẤM ĐIỂM (grade = NULL) để test S18
(1, 5, 
 N'Em xin gửi bài dịch của em:
1. わたしは だいがくせいです。
2. タナカさんは いしゃではありません。
3. さくらさんは せんせいです。', 
 NULL, NULL, NULL, 
 DATEADD(day, -1, SYSDATETIME()), NULL, NULL, NULL, NULL);
GO

-- ==============================================================
-- KIỂM TRA LẠI DỮ LIỆU ĐÃ TẠO THÀNH CÔNG
-- ==============================================================
SELECT 'users' AS [Table], COUNT(*) AS [TotalRows] FROM dbo.users UNION ALL
SELECT 'courses', COUNT(*) FROM dbo.courses UNION ALL
SELECT 'enrollments', COUNT(*) FROM dbo.enrollments UNION ALL
SELECT 'lessons', COUNT(*) FROM dbo.lessons UNION ALL
SELECT 'assignments', COUNT(*) FROM dbo.assignments UNION ALL
SELECT 'submissions', COUNT(*) FROM dbo.submissions;
GO
