/**
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.batch.configuration;

import java.io.IOException;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * @author Michael Minella
 */
@Configuration
public class BatchConfiguration {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private ApplicationContext applicationContext;

	@Bean
	public Job job() throws Exception {
		return this.jobBuilderFactory.get("sortJob")
				.start(step1())
				.build();
	}

	@Bean
	public Step step1() throws Exception {

		MultiResourcePartitioner multiResourcePartitioner = new MultiResourcePartitioner();
		multiResourcePartitioner.setKeyName("partition");
		multiResourcePartitioner.setResources(applicationContext.getResources("file://data/part*"));

		TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
		partitionHandler.setStep(workerStep());
		partitionHandler.setTaskExecutor(new SimpleAsyncTaskExecutor());

		return this.stepBuilderFactory.get("sortStep")
				.partitioner("workerStep", multiResourcePartitioner)
				.partitionHandler(partitionHandler)
				.gridSize(10)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.build();
	}

	@Bean
	public Step workerStep() {
		return this.stepBuilderFactory.get("workerStep")
				.chunk(10000)
				.reader()
				.writer()
				.build();
	}

	@Bean
	@StepScope
	public FlatFileItemReader<Item> reader(@Value("#{stepExecutionContext['fileName']}") Resource inputFile) {
		return new FlatFileItemReaderBuilder<Item>()
				.;
	}
}
