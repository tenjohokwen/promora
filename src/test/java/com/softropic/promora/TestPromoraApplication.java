package com.softropic.promora;

import org.springframework.boot.SpringApplication;

public class TestPromoraApplication {

	public static void main(String[] args) {
		SpringApplication.from(PromoraApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
