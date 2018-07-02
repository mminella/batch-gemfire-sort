package io.spring.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;

@EnableBatchProcessing
@PeerCacheApplication
@EnableEntityDefinedRegions
@SpringBootApplication
public class BatchGemfireFileSortApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchGemfireFileSortApplication.class, args);
	}
}
