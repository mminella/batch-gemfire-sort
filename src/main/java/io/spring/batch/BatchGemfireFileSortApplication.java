package io.spring.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;

@EnableBatchProcessing
@SpringBootApplication
public class BatchGemfireFileSortApplication {

	public static void main(String[] args) {
		Properties properties = System.getProperties();
		properties.put("spring.profiles.active", "master");
		properties.put("spring.cloud.deployer.local.deleteFilesOnExit", "false");

		List<String> newArgs = new ArrayList<>(args.length + 1);

		Collections.addAll(newArgs, args);

//		newArgs.add("--debug");

		SpringApplication.run(BatchGemfireFileSortApplication.class, newArgs.toArray(new String[newArgs.size()]));
	}
}

