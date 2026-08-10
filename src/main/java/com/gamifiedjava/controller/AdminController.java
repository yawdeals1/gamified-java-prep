package com.gamifiedjava.controller;

import com.gamifiedjava.auth.AuthUser;
import com.gamifiedjava.auth.MembershipService;
import com.gamifiedjava.model.AppUser;
import com.gamifiedjava.service.DeploroEndUserService;
import com.gamifiedjava.service.InvitationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.regex.Pattern;

@Controller
@Validated
@RequestMapping("/admin")
public class AdminController {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final MembershipService memberships;
    private final InvitationService invitations;
    private final DeploroEndUserService endUsers;
    private final String publicBaseUrl;

    public AdminController(MembershipService memberships, InvitationService invitations,
                           DeploroEndUserService endUsers,
                           @Value("${app.public-base-url:https://gamified-java-prep.deploro.app}") String publicBaseUrl) {
        this.memberships = memberships;
        this.invitations = invitations;
        this.endUsers = endUsers;
        this.publicBaseUrl = publicBaseUrl;
    }

    @GetMapping
    public String panel(Model model) {
        model.addAttribute("members", memberships.activeUsers());
        model.addAttribute("pendingInvites", invitations.pending());
        model.addAttribute("emailReady", invitations.emailReady());
        return "admin";
    }

    @PostMapping("/invite")
    public String invite(@RequestParam @Size(max = 320) String email,
                         HttpServletRequest request, RedirectAttributes flash) {
        String clean = MembershipService.normalize(email);
        if (!EMAIL.matcher(clean).matches()) {
            flash.addFlashAttribute("error", "Enter a valid email address.");
            return "redirect:/admin";
        }
        AuthUser admin = (AuthUser) request.getAttribute("authUser");
        try {
            invitations.invite(clean, admin.email(), publicBaseUrl);
            flash.addFlashAttribute("success", "Invitation sent to " + clean + ".");
        } catch (Exception e) {
            flash.addFlashAttribute("error", safeMessage(e));
        }
        return "redirect:/admin";
    }

    @PostMapping("/members/{id}/remove")
    public String remove(@PathVariable @Positive Integer id,
                         HttpServletRequest request, RedirectAttributes flash) {
        AuthUser admin = (AuthUser) request.getAttribute("authUser");
        AppUser member = memberships.findById(id).orElse(null);
        if (member == null || !member.isActive()) {
            flash.addFlashAttribute("error", "Member not found.");
        } else if (member.getRole() == AppUser.Role.ADMIN || admin.id().equals(member.getAuthUserId())) {
            flash.addFlashAttribute("error", "The admin account cannot be removed.");
        } else {
            memberships.deactivate(member);
            boolean deleted = endUsers.delete(member.getAuthUserId());
            flash.addFlashAttribute("success", deleted
                    ? "Member access and login were removed."
                    : "Member access was removed. Their provider identity could not be deleted automatically.");
        }
        return "redirect:/admin";
    }

    private String safeMessage(Exception e) {
        return "The invitation could not be sent. Check the email service configuration.";
    }
}
