package com.example.membership_flow.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository repository;

    public Member register(MemberRequest request) {
        repository.findByEmail(request.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("Member already exists with email: " + request.email());
        });
        var member = Member.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .build();
        return repository.save(member);
    }

    public Member findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));
    }

    public List<Member> findAll() {
        return repository.findAll();
    }
}
