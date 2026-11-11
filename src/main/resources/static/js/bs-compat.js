/* ==========================================================================
   BS-COMPAT JS - thay thế phần Bootstrap JS mà các trang Assignment/Grading dùng:
     - data-bs-dismiss="alert"  : đóng thông báo
     - data-bs-toggle="modal" + data-bs-target="#id" : mở hộp thoại
     - data-bs-dismiss="modal"  : đóng hộp thoại (còn đóng khi bấm nền tối hoặc phím Esc)
   Dùng event delegation nên không phụ thuộc thời điểm nạp script.
   ========================================================================== */
(function () {
    'use strict';

    function openModal(modal) {
        if (!modal) { return; }
        modal.classList.add('show');
        modal.setAttribute('aria-modal', 'true');
        modal.removeAttribute('aria-hidden');
        document.body.style.overflow = 'hidden';
    }

    function closeModal(modal) {
        if (!modal) { return; }
        modal.classList.remove('show');
        modal.removeAttribute('aria-modal');
        modal.setAttribute('aria-hidden', 'true');
        if (!document.querySelector('.bs-compat .modal.show')) {
            document.body.style.overflow = '';
        }
    }

    document.addEventListener('click', function (event) {
        var target = event.target;
        if (!(target instanceof Element)) { return; }

        var dismiss = target.closest('[data-bs-dismiss]');
        if (dismiss) {
            var kind = dismiss.getAttribute('data-bs-dismiss');
            if (kind === 'alert') {
                var alertBox = dismiss.closest('.alert');
                if (alertBox) { alertBox.remove(); }
                return;
            }
            if (kind === 'modal') {
                closeModal(dismiss.closest('.modal'));
                return;
            }
        }

        var toggle = target.closest('[data-bs-toggle="modal"]');
        if (toggle) {
            var selector = toggle.getAttribute('data-bs-target') || toggle.getAttribute('href');
            if (selector) {
                try { openModal(document.querySelector(selector)); } catch (e) { /* selector không hợp lệ */ }
            }
            return;
        }

        // Bấm vào nền tối bên ngoài hộp thoại thì đóng
        if (target.classList.contains('modal') && target.classList.contains('show')) {
            closeModal(target);
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            document.querySelectorAll('.bs-compat .modal.show').forEach(closeModal);
        }
    });
})();
