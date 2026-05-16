package com.example.membership_flow;

import org.springframework.boot.SpringApplication;

public class TestMembershipFlowApplication {

	public static void main(String[] args) {
		SpringApplication.from(MembershipFlowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
