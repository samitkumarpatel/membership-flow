package com.example.membership_flow.member;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    @PostMapping("/api/members")
    @ResponseStatus(HttpStatus.CREATED)
    public Member register(@RequestBody MemberRequest request) {
        return service.register(request);
    }

    @GetMapping("/api/members/{id}")
    public Member getOne(@PathVariable String id) {
        return service.findById(id);
    }

    @GetMapping("/api/admin/members")
    public List<Member> listAll() {
        return service.findAll();
    }
}
