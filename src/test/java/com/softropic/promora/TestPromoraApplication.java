package com.softropic.promora;

import com.softropic.promora.config.TestConfig;

import org.springframework.boot.SpringApplication;

public class TestPromoraApplication {

	public static void main(String[] args) {
		SpringApplication.from(PromoraApplication::main).with(TestConfig.class).run(args);
	}

}
