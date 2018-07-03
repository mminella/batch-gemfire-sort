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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.spring.batch.batch.CountingItemWriter;
import io.spring.batch.batch.GemfireCountTasklet;
import io.spring.batch.batch.SortFileItemReader;
import io.spring.batch.domain.Item;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.data.GemfireItemWriter;
import org.springframework.batch.item.data.builder.GemfireItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.partition.CommandLineArgsProvider;
import org.springframework.cloud.task.batch.partition.DeployerPartitionHandler;
import org.springframework.cloud.task.batch.partition.DeployerStepExecutionHandler;
import org.springframework.cloud.task.batch.partition.NoOpEnvironmentVariablesProvider;
import org.springframework.cloud.task.batch.partition.SimpleCommandLineArgsProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;

/**
 * @author Michael Minella
 */
@Configuration
public class BatchConfiguration {

	@Profile("master")
	@Configuration
	public static class MasterConfiguration {

		@Bean
		public Step master(StepBuilderFactory stepBuilderFactory,
				Partitioner partitioner,
				PartitionHandler partitionHandler) {
			return stepBuilderFactory.get("master")
					.partitioner("workerStep", partitioner)
					.partitionHandler(partitionHandler)
					.build();
		}

		@Bean
		public Job batchGemfireSort(JobBuilderFactory jobBuilderFactory) throws Exception {
			return jobBuilderFactory.get("batchGemfireSort")
					.start(master(null, null, null))
					.build();
		}

		@Bean
		public Partitioner partitioner(ResourcePatternResolver resourcePatternResolver) throws IOException {
			Resource[] resources = resourcePatternResolver.getResources("classpath:data/part*");

			MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
			partitioner.setResources(resources);

			return partitioner;
		}

		@Bean
		public SimpleCommandLineArgsProvider commandLineArgsProvider() {
			SimpleCommandLineArgsProvider provider = new SimpleCommandLineArgsProvider();

			List<String> commandLineArgs = new ArrayList<>(4);
			commandLineArgs.add("--spring.profiles.active=worker");
			commandLineArgs.add("--spring.cloud.task.initialize.enable=false");
			commandLineArgs.add("--spring.batch.initialize-schema=never");
			commandLineArgs.add("--spring.datasource.initialize=false");
			provider.setAppendedArgs(commandLineArgs);

			return provider;
		}

		@Bean
		public DeployerPartitionHandler partitionHandler(
				@Value("${spring.application.name}") String applicationName,
				DelegatingResourceLoader resourceLoader,
				TaskLauncher taskLauncher,
				JobExplorer jobExplorer,
				CommandLineArgsProvider commandLineArgsProvider) {

			Resource resource = resourceLoader.getResource("maven://io.spring:batch-gemfire-file-sort:0.0.1-SNAPSHOT");

			DeployerPartitionHandler partitionHandler =
					new DeployerPartitionHandler(taskLauncher,
							jobExplorer,
							resource,
							"workerStep");

			partitionHandler.setCommandLineArgsProvider(commandLineArgsProvider);
			partitionHandler.setEnvironmentVariablesProvider(new NoOpEnvironmentVariablesProvider());
			partitionHandler.setMaxWorkers(10);
			partitionHandler.setApplicationName(applicationName);

			return partitionHandler;
		}
	}

	@Profile("worker")
	@PeerCacheApplication
	@EnableEntityDefinedRegions
	@Configuration
	public static class WorkerConfiguration {

		@Bean
		public DeployerStepExecutionHandler stepExecutionHandler(ApplicationContext context, JobExplorer jobExplorer, JobRepository jobRepository) {
			return new DeployerStepExecutionHandler(context, jobExplorer, jobRepository);
		}

		@Bean
		public Step workerStep(StepBuilderFactory stepBuilderFactory) {
			return stepBuilderFactory.get("workerStep")
					.<Item, Item>chunk(1050000)
					.reader(reader(null))
					.writer(itemWriter())
					.build();
		}

		@Bean
		@StepScope
		public SortFileItemReader reader(@Value("#{stepExecutionContext['fileName']}")Resource file) {
			SortFileItemReader reader = new SortFileItemReader();

			reader.setName("reader");
			reader.setResource(file);

			return reader;
		}

		@Bean
		public CompositeItemWriter<Item> itemWriter() {
			return new CompositeItemWriterBuilder<Item>()
					.delegates(Arrays.asList(new CountingItemWriter(), gemfireItemWriter(null)))
					.build();
		}

		@Bean
		@StepScope
		public GemfireItemWriter<byte[], Item> gemfireItemWriter(GemfireTemplate template) {
			return new GemfireItemWriterBuilder<byte[], Item>()
					.itemKeyMapper(Item::getKey)
					.template(template)
					.build();
		}

		@Bean
		public Step validationStep(StepBuilderFactory stepBuilderFactory, GemfireCountTasklet tasklet) {
			return stepBuilderFactory.get("validationStep")
					.tasklet(tasklet)
					.build();
		}

		@Bean
		@StepScope
		public GemfireTemplate gemfireTemplate (org.apache.geode.cache.Region region) {
			GemfireTemplate template = new GemfireTemplate(region);

			return template;
		}

		@Bean
		@StepScope
		public GemfireCountTasklet tasklet() {
			return new GemfireCountTasklet(gemfireTemplate(null));
		}

	}
}
