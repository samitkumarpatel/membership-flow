package com.example.membership_flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableResilientMethods
@EnableScheduling
public class MembershipFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(MembershipFlowApplication.class, args);
	}

}
