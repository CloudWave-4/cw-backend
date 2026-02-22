package com.cjolivenetworks.CloudWave_Backend;

import com.cjolivenetworks.CloudWave_Backend.global.kafka.TaskProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class CloudWaveBackendApplicationTests {

	@MockitoBean
	private TaskProducer taskProducer;

	@Test
	void contextLoads() {
	}
}