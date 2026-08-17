package com.miniproject.verificationApp;

import com.miniproject.verificationApp.service.AIVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VerificationAppApplicationTests {

	@MockitoBean
	private AIVerificationService aiVerificationService;

	@Test
	void contextLoads() {
	}

}
