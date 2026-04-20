package com.example.scanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ScannerProperties.class)
public class ScannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScannerApplication.class, args);
	}

}
