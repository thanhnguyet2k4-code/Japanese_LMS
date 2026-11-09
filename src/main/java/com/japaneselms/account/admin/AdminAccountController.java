
package com.japaneselms.account.admin;

import com.japaneselms.account.dto.AccountUpdateForm;
import com.japaneselms.account.service.AccountService;
import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.japaneselms.account.dto.AccountCreateForm;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AdminAccountController {

    private static final Logger log =
            LoggerFactory.getLogger(AdminAccountController.class);
    private final UserRepository userRepository;
    private final AccountService accountService;

    public AdminAccountController(
            UserRepository userRepository,
            AccountService accountService) {
        this.userRepository = userRepository;
        this.accountService = accountService;
    }

    // ==================== DANH SACH TAI KHOAN ====================

    @GetMapping("/admin/accounts")
    public String listAccounts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(
                page, 10, Sort.by(Sort.Direction.DESC, "userId"));

        Specification<User> spec =
                (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchValue =
                    "%" + keyword.trim().toLowerCase() + "%";

            Specification<User> keywordSpec =
                    (root, query, cb) -> cb.or(
                            cb.like(cb.lower(root.get("fullName")),
                                    searchValue),
                            cb.like(cb.lower(root.get("email")),
                                    searchValue));

            spec = spec.and(keywordSpec);
        }

        if (role != null && !role.trim().isEmpty()) {
            String selectedRole = role.trim().toUpperCase();

            Specification<User> roleSpec =
                    (root, query, cb) ->
                            cb.equal(root.get("role"), selectedRole);

            spec = spec.and(roleSpec);
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            spec = spec.and(
                    (root, query, cb) -> cb.isTrue(root.get("active")));
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            spec = spec.and(
                    (root, query, cb) -> cb.isFalse(root.get("active")));
        }

        Page<User> accountsPage = userRepository.findAll(spec, pageable);

        model.addAttribute("accountsPage", accountsPage);
        model.addAttribute("accounts", accountsPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", accountsPage.getTotalPages());
        model.addAttribute("totalElements", accountsPage.getTotalElements());
        model.addAttribute(
                "activeAdminCount",
                userRepository.countByRoleIgnoreCaseAndActiveTrue("ADMIN")
        );

        model.addAttribute("currentTab", "accounts");
        long pendingCount = userRepository.findByAccountStatus("PENDING").stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .count();
        model.addAttribute("unreadNotificationsCount", pendingCount);

        return "admin/accounts";
    }

    // ==================== TAO TAI KHOAN ====================

    @GetMapping("/admin/accounts/new")
    public String showCreateForm(Model model) {
        model.addAttribute("account", new AccountCreateForm());
        model.addAttribute("isEdit", false);

        addAdminLayoutAttributes(model);

        return "admin/account-form";
    }

    @PostMapping("/admin/accounts")
    public String createAccount(
            @Valid @ModelAttribute("account") AccountCreateForm account,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra dữ liệu đầu vào
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            addAdminLayoutAttributes(model);
            return "admin/account-form";
        }

        try {
            accountService.createAccount(
                    account.getFullName(),
                    account.getEmail(),
                    account.getInitialPassword(),
                    account.getRole(),
                    account.getPhone()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Tạo tài khoản thành công."
            );
            return "redirect:/admin/accounts";

        }catch (IllegalArgumentException ex) {
        model.addAttribute("isEdit", false);
        model.addAttribute("errorMessage", ex.getMessage());

        addAdminLayoutAttributes(model);

        return "admin/account-form";
    }
    }

    // ==================== CHINH SUA TAI KHOAN ====================

    @GetMapping("/admin/accounts/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        return userRepository.findById(id)
                .map(user -> {
                    addAdminLayoutAttributes(model);
                    AccountUpdateForm form = new AccountUpdateForm();

                    form.setUserId(user.getUserId());
                    form.setFullName(user.getFullName());
                    form.setEmail(user.getEmail());
                    form.setPhone(user.getPhone());

                    model.addAttribute("account", form);
                    model.addAttribute("isEdit", true);

                    return "admin/account-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute(
                            "errorMessage", "Không tìm thấy tài khoản.");
                    return "redirect:/admin/accounts";
                });
    }

    @PostMapping("/admin/accounts/{id}/edit")
    public String updateAccount(
            @PathVariable Long id,
            @Valid @ModelAttribute("account") AccountUpdateForm account,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        account.setUserId(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            addAdminLayoutAttributes(model);
            return "admin/account-form";
        }

        try {
            accountService.updateAccount(
                    id,
                    account.getFullName(),
                    account.getEmail(),
                    account.getPhone()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật tài khoản thành công."
            );
            return "redirect:/admin/accounts";

        } catch (IllegalArgumentException ex) {
            log.warn("Khong the cap nhat tai khoan ID {}: {}", id, ex.getMessage());

            model.addAttribute("isEdit", true);
            model.addAttribute("errorMessage", ex.getMessage());
            addAdminLayoutAttributes(model);

            return "admin/account-form";

        } catch (Exception ex) {
            log.error("Loi khong mong doi khi cap nhat tai khoan ID {}", id, ex);

            model.addAttribute("isEdit", true);
            model.addAttribute(
                    "errorMessage",
                    "Đã xảy ra lỗi khi cập nhật tài khoản. Vui lòng thử lại."
            );
            addAdminLayoutAttributes(model);

            return "admin/account-form";
        }
    }


    // ==================== BAT / TAT TAI KHOAN ====================

    @PostMapping("/admin/accounts/{id}/toggle")
    public String toggleAccount(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            User updatedAccount = accountService.toggleAccountStatus(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    updatedAccount.isActive()
                            ? "Đã mở khóa tài khoản."
                            : "Đã khóa tài khoản."
            );

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());
        }

        return "redirect:/admin/accounts";
    }
    private void addAdminLayoutAttributes(Model model) {
        model.addAttribute("currentTab", "accounts");

        long pendingCount = userRepository.findByAccountStatus("PENDING")
                .stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .count();

        model.addAttribute("unreadNotificationsCount", pendingCount);
    }
}
