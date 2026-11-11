/*
 * Validate form phía trình duyệt dùng chung cho toàn hệ thống.
 *
 * - Thay bong bóng cảnh báo mặc định của trình duyệt bằng dòng chữ đỏ
 *   hiển thị ngay dưới ô nhập (cùng phong cách trang đăng nhập).
 * - Chỉ chặn việc gửi form khi dữ liệu sai. Không thay đổi dữ liệu gửi lên,
 *   không thay đổi logic xử lý của server. Server vẫn validate lại toàn bộ.
 * - Bỏ qua form có thuộc tính novalidate (form đó dùng lỗi do server trả về).
 *
 * Các thuộc tính tùy chọn trên ô nhập:
 *   data-label="Tên hiển thị"      : tên trường dùng trong câu thông báo
 *   data-match="idOChinh"          : giá trị phải khớp với ô có id đó
 *   data-email-strict              : email phải có tên miền dạng a@b.c
 *   data-msg-required / data-msg-type / data-msg-short / data-msg-long /
 *   data-msg-pattern / data-msg-min / data-msg-max / data-msg-step /
 *   data-msg-match                 : thay câu thông báo mặc định
 */
(function () {
    'use strict';

    // Mặc định kiểm tra email giống trình duyệt (không làm khó tài khoản cũ).
    // Ô có data-email-strict (đăng ký, tạo tài khoản) yêu cầu có tên miền dạng a@b.c
    var EMAIL_LOOSE_RE = /^[^\s@]+@[^\s@]+$/;
    var EMAIL_STRICT_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
    var TEXT_TYPES = ['text', 'email', 'tel', 'url', 'search', 'password'];
    var SKIP_TYPES = ['hidden', 'submit', 'button', 'reset', 'image'];
    var SERVER_ERROR_SELECTOR = '.error-line, .invalid-feedback, .field-error:not(.js-field-error)';

    function isTextControl(el) {
        if (el.tagName === 'TEXTAREA') {
            return true;
        }
        return el.tagName === 'INPUT'
            && TEXT_TYPES.indexOf((el.getAttribute('type') || 'text').toLowerCase()) > -1;
    }

    function getControls(form) {
        return Array.prototype.filter.call(form.elements, function (el) {
            if (!/^(INPUT|SELECT|TEXTAREA)$/.test(el.tagName)) {
                return false;
            }
            if (SKIP_TYPES.indexOf((el.type || '').toLowerCase()) > -1) {
                return false;
            }
            return !el.disabled && !el.readOnly;
        });
    }

    function upperFirst(text) {
        return text ? text.charAt(0).toUpperCase() + text.slice(1) : text;
    }

    // Lấy tên trường từ label để ghép vào câu thông báo
    function getLabel(el) {
        if (el.dataset && el.dataset.label) {
            return el.dataset.label;
        }

        var label = null;
        if (el.id) {
            label = document.querySelector('label[for="' + el.id + '"]');
        }
        if (!label) {
            label = el.closest('label');
        }

        var text = '';
        if (label) {
            var copy = label.cloneNode(true);
            var noise = copy.querySelectorAll(
                '.required, .text-danger, .text-muted, .text-secondary, small, .form-text, svg, i'
            );
            Array.prototype.forEach.call(noise, function (n) {
                n.parentNode.removeChild(n);
            });
            text = copy.textContent;
        } else {
            text = el.getAttribute('placeholder') || el.name || 'giá trị này';
        }

        text = text.replace(/\([^)]*\)/g, '')
            .replace(/[*:：]/g, '')
            .replace(/\s+/g, ' ')
            .trim();

        if (!text) {
            return 'giá trị này';
        }

        // Hạ chữ cái đầu cho câu văn tự nhiên, trừ khi là từ viết tắt (JLPT, URL...)
        var rest = text.slice(1);
        if (rest === rest.toLowerCase()) {
            text = text.charAt(0).toLowerCase() + rest;
        }
        return text;
    }

    function custom(el, kind, fallback) {
        var key = 'msg' + upperFirst(kind);
        return (el.dataset && el.dataset[key]) ? el.dataset[key] : fallback;
    }

    function radioGroupChecked(el) {
        if (!el.name || !el.form) {
            return el.checked;
        }
        var group = el.form.querySelectorAll('input[type="radio"]');
        return Array.prototype.some.call(group, function (r) {
            return r.name === el.name && r.checked;
        });
    }

    // Trả về câu thông báo lỗi, hoặc chuỗi rỗng nếu hợp lệ
    function checkControl(el) {
        var type = (el.type || '').toLowerCase();
        var label = getLabel(el);
        var value = el.value || '';
        var text = isTextControl(el);
        var trimmed = text ? value.trim() : value;
        var validity = el.validity;

        if (validity && validity.badInput) {
            return custom(el, 'type', 'Vui lòng nhập ' + label + ' hợp lệ.');
        }

        if (el.required) {
            var missing;
            if (type === 'checkbox') {
                missing = !el.checked;
            } else if (type === 'radio') {
                missing = !radioGroupChecked(el);
            } else if (type === 'file') {
                missing = !(el.files && el.files.length);
            } else {
                missing = trimmed === '';
            }

            if (missing) {
                var verb = (el.tagName === 'SELECT' || type === 'checkbox'
                    || type === 'radio' || type === 'file') ? 'chọn' : 'nhập';
                return custom(el, 'required', 'Vui lòng ' + verb + ' ' + label + '.');
            }
        }

        if (trimmed === '') {
            return '';
        }

        if (type === 'email'
            && !(el.hasAttribute('data-email-strict') ? EMAIL_STRICT_RE : EMAIL_LOOSE_RE).test(trimmed)) {
            return custom(el, 'type', 'Email không đúng định dạng.');
        }
        if (validity && validity.typeMismatch) {
            return custom(el, 'type', upperFirst(label) + ' không đúng định dạng.');
        }

        if (text) {
            if (el.minLength > 0 && value.length < el.minLength) {
                return custom(el, 'short',
                    upperFirst(label) + ' phải có ít nhất ' + el.minLength + ' ký tự.');
            }
            if (el.maxLength > 0 && value.length > el.maxLength) {
                return custom(el, 'long',
                    upperFirst(label) + ' không được vượt quá ' + el.maxLength + ' ký tự.');
            }
        }

        if (validity && validity.patternMismatch) {
            return custom(el, 'pattern',
                el.title || (upperFirst(label) + ' không đúng định dạng.'));
        }
        if (validity && validity.rangeUnderflow) {
            return custom(el, 'min', upperFirst(label) + ' tối thiểu là ' + el.min + '.');
        }
        if (validity && validity.rangeOverflow) {
            return custom(el, 'max', upperFirst(label) + ' tối đa là ' + el.max + '.');
        }
        if (validity && validity.stepMismatch) {
            return custom(el, 'step', upperFirst(label) + ' không hợp lệ.');
        }

        if (el.dataset && el.dataset.match) {
            var other = document.getElementById(el.dataset.match);
            if (other && other.value !== el.value) {
                return custom(el, 'match', 'Giá trị xác nhận không khớp.');
            }
        }

        return '';
    }

    function singleControlContainer(el) {
        var box = el.closest('.field, .form-group, .mb-3, .mb-4');
        if (!box) {
            return null;
        }
        var inner = box.querySelectorAll('input, select, textarea');
        var count = Array.prototype.filter.call(inner, function (c) {
            return SKIP_TYPES.indexOf((c.type || '').toLowerCase()) === -1;
        }).length;
        return count === 1 ? box : null;
    }

    // Ẩn lỗi cũ do server render khi người dùng đã sửa / hoặc đã có lỗi mới
    function hideServerError(el) {
        var box = singleControlContainer(el);
        if (box) {
            Array.prototype.forEach.call(box.querySelectorAll(SERVER_ERROR_SELECTOR), function (n) {
                n.style.display = 'none';
            });
        }
        el.classList.remove('is-invalid');
        var shell = el.closest('.input-shell');
        if (shell) {
            shell.classList.remove('invalid');
        }
    }

    function clearError(el) {
        if (el.__jsErrorNode && el.__jsErrorNode.parentNode) {
            el.__jsErrorNode.parentNode.removeChild(el.__jsErrorNode);
        }
        el.__jsErrorNode = null;
        el.classList.remove('field-invalid');
        el.removeAttribute('aria-invalid');
        var shell = el.closest('.input-shell');
        if (shell) {
            shell.classList.remove('invalid');
            shell.classList.remove('field-invalid');
        }
    }

    function showError(el, message) {
        if (el.__jsErrorNode) {
            el.__jsErrorNode.textContent = message;
            return;
        }

        hideServerError(el);

        var anchor = el.closest('.input-shell, .input-group, .password-wrapper') || el;
        var node = document.createElement('p');
        node.className = 'field-error js-field-error';
        node.setAttribute('role', 'alert');
        node.textContent = message;
        anchor.parentNode.insertBefore(node, anchor.nextSibling);

        el.__jsErrorNode = node;
        el.classList.add('field-invalid');
        el.setAttribute('aria-invalid', 'true');

        var shell = el.closest('.input-shell');
        if (shell) {
            shell.classList.add('invalid');
        }
    }

    function validateForm(form) {
        var firstInvalid = null;

        getControls(form).forEach(function (el) {
            var message = checkControl(el);
            if (message) {
                showError(el, message);
                if (!firstInvalid) {
                    firstInvalid = el;
                }
            } else {
                clearError(el);
            }
        });

        return firstInvalid;
    }

    function recheckErroredControls(form) {
        getControls(form).forEach(function (el) {
            if (!el.__jsErrorNode) {
                return;
            }
            var message = checkControl(el);
            if (message) {
                el.__jsErrorNode.textContent = message;
            } else {
                clearError(el);
            }
        });
    }

    function init() {
        Array.prototype.forEach.call(document.forms, function (form) {
            if (form.hasAttribute('novalidate')) {
                form.setAttribute('data-js-skip', 'true');
            } else {
                // Tắt bong bóng mặc định, việc kiểm tra do script này đảm nhiệm
                form.noValidate = true;
            }
        });
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;

        if (!(form instanceof HTMLFormElement) || form.hasAttribute('data-js-skip')) {
            return;
        }
        if (event.submitter && event.submitter.formNoValidate) {
            return;
        }

        var firstInvalid = validateForm(form);
        if (firstInvalid) {
            event.preventDefault();
            firstInvalid.focus({preventScroll: true});
            if (firstInvalid.scrollIntoView) {
                firstInvalid.scrollIntoView({block: 'center', behavior: 'smooth'});
            }
        }
    }, true);

    function onEdit(event) {
        var el = event.target;
        if (!el || !el.form || form_skipped(el.form)) {
            return;
        }
        if (SKIP_TYPES.indexOf((el.type || '').toLowerCase()) > -1) {
            return;
        }

        // Người dùng bắt đầu sửa thì bỏ lỗi cũ do server trả về ở ô này
        if (!el.__jsErrorNode) {
            hideServerError(el);
        }
        recheckErroredControls(el.form);
    }

    function form_skipped(form) {
        return form.hasAttribute('data-js-skip');
    }

    document.addEventListener('input', onEdit, true);
    document.addEventListener('change', onEdit, true);

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
