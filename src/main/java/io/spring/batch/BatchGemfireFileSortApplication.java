package io.spring.batch;

import io.spring.batch.domain.Item;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializer;
import org.apache.geode.pdx.PdxWriter;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@EnableTask
@EnableBatchProcessing
@SpringBootApplication
public class BatchGemfireFileSortApplication {

	public static void main(String[] args) {
		Properties properties = System.getProperties();
		properties.put("spring.profiles.active", "master");

		List<String> newArgs = new ArrayList<>(args.length + 1);

		Collections.addAll(newArgs, args);

//		newArgs.add("--debug");

		SpringApplication.run(BatchGemfireFileSortApplication.class, newArgs.toArray(new String[newArgs.size()]));
	}
}

