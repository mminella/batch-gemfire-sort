package io.spring.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class BatchGemfireFileSortApplication {

	public static void main(String[] args) {

		if(System.getenv("GEMFIRE_START-LOCATOR") != null) {
			System.setProperty("gemfire.start-locator", System.getenv("GEMFIRE_START-LOCATOR"));
		}

		List<String> newArgs = new ArrayList<>(args.length + 1);

		Collections.addAll(newArgs, args);

		SpringApplication.run(BatchGemfireFileSortApplication.class, newArgs.toArray(new String[newArgs.size()]));
	}
}

