/**
 * Copyright 2018 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.batch.configuration;

import io.spring.batch.batch.CountingItemWriter;
import io.spring.batch.batch.GemfireCountTasklet;
import io.spring.batch.batch.SortFileItemReader;
import io.spring.batch.domain.Item;
import org.apache.geode.cache.Region;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.data.GemfireItemWriter;
import org.springframework.batch.item.data.builder.GemfireItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.gemfire.GemfireTemplate;

import java.util.Arrays;

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
		return this.jobBuilderFactory.get("sortJob").start(workerStep()).next(validationStep(null)).build();
	}

	@Bean
	public Step validationStep(GemfireCountTasklet tasklet) {
		return this.stepBuilderFactory.get("validationStep").tasklet(tasklet).build();
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
		return this.stepBuilderFactory.get("workerStep").<Item, Item>chunk(1050000).reader(reader())
			.writer(itemWriter())
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
	@StepScope
	public CompositeItemWriter<Item> itemWriter() {
		return new CompositeItemWriterBuilder<Item>().delegates(
			Arrays.asList(new CountingItemWriter(), gemfireItemWriter(null))).build();
	}

	@Bean
	@StepScope
	public GemfireItemWriter<byte[], Item> gemfireItemWriter(GemfireTemplate template) {
		return new GemfireItemWriterBuilder<byte[], Item>().itemKeyMapper(Item::getKey).template(template).build();
	}

	@Bean
	@StepScope
	public GemfireTemplate gemfireTemplate(Region region) {
		GemfireTemplate template = new GemfireTemplate(region);

		return template;
	}
}
