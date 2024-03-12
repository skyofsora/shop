package com.shop.controller;

import com.shop.dto.MemberFormDto;
import com.shop.dto.SessionUser;
import com.shop.entity.Member;
import com.shop.service.MemberService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final HttpSession httpSession;

    @GetMapping("/new/{email}")
    public String newMemberForm(@PathVariable("email") String email, Model model) {
        SessionUser temp = (SessionUser) httpSession.getAttribute("email");
        if (temp == null || !Objects.equals(temp.getEmail(), email)) {
            return "member/memberLoginForm";
        }
        httpSession.removeAttribute("email");
        MemberFormDto memberFormDto = new MemberFormDto();
        memberFormDto.setEmail(email);
        model.addAttribute("memberFormDto", memberFormDto);
        return "member/memberForm";

    }

    @GetMapping("/new")
    public String memberForm() {
        return "member/memberFormEmail";
    }

    @PostMapping("/new")
    public String memberForm(@Valid MemberFormDto memberFormDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "member/memberForm";
        }
        try {
            Member member = Member.createMember(memberFormDto, passwordEncoder);
            memberService.saveMember(member);
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "member/memberForm";
        }
        return "redirect:/";
    }

    @GetMapping("/login")
    public String loginMember() {
        return "member/memberLoginForm";
    }

    @GetMapping("/login/error")
    public String loginError(Model model) {
        model.addAttribute("loginErrorMsg", "아이디 또는 비밀번호를 확인해주세요.");
        return "member/memberLoginForm";
    }
}
