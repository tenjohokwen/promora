package com.softropic.promora;

import com.softropic.promora.config.TestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
				properties = {"ledger.database.spy=true", "enable.test.mail=true"})
@Import(TestConfig.class)
class PromoraApplicationTests {

	@Test
	void contextLoads() {
	}

}
