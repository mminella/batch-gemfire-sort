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

import io.spring.batch.batch.CountingItemWriter;
import io.spring.batch.batch.SortFileItemReader;
import io.spring.batch.domain.Item;
import org.apache.geode.internal.cache.LocalRegion;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.data.GemfireItemWriter;
import org.springframework.batch.item.data.builder.GemfireItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.gemfire.GemfireTemplate;

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
				.start(workerStep())
				.build();
	}

	@Bean
	public Step step1() throws Exception {

		MultiResourcePartitioner multiResourcePartitioner = new MultiResourcePartitioner();
		multiResourcePartitioner.setKeyName("partition");
		multiResourcePartitioner.setResources(applicationContext.getResources("classpath:data/part*"));

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
				.<Item, Item>chunk(1050000)
				.reader(reader())
				.writer(new CountingItemWriter())
				.build();
	}

	@Bean
	public SortFileItemReader reader() {
		SortFileItemReader reader = new SortFileItemReader();

		reader.setName("reader");
		reader.setResource(applicationContext.getResource("classpath:data/part0"));

		return reader;
	}

	@Bean
	public GemfireItemWriter<byte[], Item> gemfireItemWriter(GemfireTemplate template) {
		return new GemfireItemWriterBuilder<byte[], Item>()
				.itemKeyMapper(Item::getKey)
				.template(template)
				.build();
	}

	@Bean
	public GemfireTemplate gemfireTemplate () {
		LocalRegion region = new LocalRegion();

		GemfireTemplate template = new GemfireTemplate(region);

		return template;
	}
}
