package com.cjolivenetworks.CloudWave_Backend;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CloudWaveBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudWaveBackendApplication.class, args);
	}

	@PostConstruct
	public void imageUpdaterTest() {
		System.out.println("image-updater-test1");
	}
}