package io.spring.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class BatchGemfireFileSortApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchGemfireFileSortApplication.class, args);
	}
}
