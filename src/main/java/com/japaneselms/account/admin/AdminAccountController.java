
package com.japaneselms.account.admin;

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

@Controller
public class AdminAccountController {

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

        return "admin/accounts";
    }

    // ==================== TAO TAI KHOAN ====================

    @GetMapping("/admin/accounts/new")
    public String showCreateForm(Model model) {
        model.addAttribute("account", new User());
        model.addAttribute("isEdit", false);

        return "admin/account-form";
    }

    @PostMapping("/admin/accounts")
    public String createAccount(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String initialPassword,
            @RequestParam String role,
            @RequestParam(required = false) String phone,
            Model model,
            RedirectAttributes redirectAttributes) {

        User formAccount = new User();
        formAccount.setFullName(fullName);
        formAccount.setEmail(email);
        formAccount.setPhone(phone);

        try {
            accountService.createAccount(
                    fullName, email, initialPassword, role, phone);

            redirectAttributes.addAttribute("success", "created");
            return "redirect:/admin/accounts";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("account", formAccount);
            model.addAttribute("isEdit", false);
            model.addAttribute("errorMessage", ex.getMessage());

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
                .map(account -> {
                    model.addAttribute("account", account);
                    model.addAttribute("isEdit", true);
                    return "admin/account-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute(
                            "errorMessage", "Khong tim thay tai khoan.");
                    return "redirect:/admin/accounts";
                });
    }

    @PostMapping("/admin/accounts/{id}/edit")
    public String updateAccount(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            accountService.updateAccount(id, fullName, email, phone);

            redirectAttributes.addAttribute("success", "updated");
            return "redirect:/admin/accounts";

        } catch (IllegalArgumentException ex) {
            User formAccount = userRepository.findById(id)
                    .orElseGet(User::new);

            formAccount.setFullName(fullName);
            formAccount.setEmail(email);
            formAccount.setPhone(phone);

            model.addAttribute("account", formAccount);
            model.addAttribute("isEdit", true);
            model.addAttribute("errorMessage", ex.getMessage());

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
                            ? "Da kich hoat tai khoan."
                            : "Da vo hieu hoa tai khoan.");

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());
        }

        return "redirect:/admin/accounts";
    }
}
