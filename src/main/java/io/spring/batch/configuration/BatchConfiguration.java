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

import java.io.IOException;
import java.util.Arrays;

import io.spring.batch.batch.CountingItemWriter;
import io.spring.batch.batch.FileWritingStepExecutionListner;
import io.spring.batch.batch.SortFileItemReader;
import io.spring.batch.domain.Item;
import io.spring.batch.geode.SortedFileWriterFunction;
import io.spring.batch.geode.SortedFileWriterFunctionExecution;
import org.apache.geode.cache.Region;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializer;
import org.apache.geode.pdx.PdxWriter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningMasterStepBuilderFactory;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.batch.item.data.GemfireItemWriter;
import org.springframework.batch.item.data.builder.GemfireItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctionExecutions;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctions;
import org.springframework.integration.channel.DirectChannel;

/**
 * @author Michael Minella
 */
@EnableBatchIntegration
@Configuration
public class BatchConfiguration {

	@Profile("master")
	@EnableTask
	@Configuration
	public static class MasterConfiguration {

		@Autowired
		private RemotePartitioningMasterStepBuilderFactory masterStepBuilderFactory;

//		@Bean
//		public Step master(StepBuilderFactory stepBuilderFactory, Partitioner partitioner,
//			PartitionHandler partitionHandler) {
//			return stepBuilderFactory.get("master")
//				.partitioner("workerStep", partitioner)
//				.partitionHandler(partitionHandler)
//				.build();
//		}

		@Bean
		public Job batchGemfireSort(JobBuilderFactory jobBuilderFactory) throws Exception {
			return jobBuilderFactory.get("batchGemfireSort").start(master(null, null)).build();
		}

		@Bean
		public Partitioner partitioner(ResourcePatternResolver resourcePatternResolver) throws IOException {
			Resource[] resources = resourcePatternResolver.getResources("classpath:data/part*");

			MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
			partitioner.setResources(resources);

			return partitioner;
		}

		@Bean
		public Step master(@Qualifier("requests") DirectChannel requests, @Qualifier("replies") DirectChannel replies) throws IOException {
			return this.masterStepBuilderFactory.get("master")
					.partitioner("workerStep", partitioner(null))
					.gridSize(4)
					.outputChannel(requests)
					.inputChannel(replies)
					.build();
		}

//		@Bean
//		public SimpleCommandLineArgsProvider commandLineArgsProvider() {
//			SimpleCommandLineArgsProvider provider = new SimpleCommandLineArgsProvider();
//
//			List<String> commandLineArgs = new ArrayList<>(4);
//			commandLineArgs.add("--spring.profiles.active=worker");
//			commandLineArgs.add("--spring.cloud.task.initialize.enable=false");
//			commandLineArgs.add("--spring.batch.initialize-schema=never");
//			commandLineArgs.add("--spring.datasource.initialize=false");
//			provider.setAppendedArgs(commandLineArgs);
//
//			return provider;
//		}
//
//		@Bean
//		public DeployerPartitionHandler partitionHandler(@Value("${spring.application.name}") String applicationName,
//			DelegatingResourceLoader resourceLoader, TaskLauncher taskLauncher, JobExplorer jobExplorer,
//			CommandLineArgsProvider commandLineArgsProvider) {
//
//			Resource resource = resourceLoader.getResource("maven://io.spring:batch-gemfire-file-sort:0.0.1-SNAPSHOT");
//
//			DeployerPartitionHandler partitionHandler = new DeployerPartitionHandler(taskLauncher, jobExplorer,
//				resource, "workerStep");
//
//			partitionHandler.setCommandLineArgsProvider(commandLineArgsProvider);
//			partitionHandler.setEnvironmentVariablesProvider(new NoOpEnvironmentVariablesProvider());
//			partitionHandler.setMaxWorkers(10);
//			partitionHandler.setApplicationName(applicationName);
//
//			return partitionHandler;
//		}
	}

	@Profile("worker")
	@Configuration
	@PeerCacheApplication
	@EnableEntityDefinedRegions(basePackageClasses = Item.class)
	@EnablePdx(serializerBeanName = "pdxSerializer")
	@EnableGemfireFunctionExecutions(basePackageClasses = SortedFileWriterFunction.class)
	@EnableGemfireFunctions
	public static class WorkerConfiguration {

		@Autowired
		private RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory;

		@Bean
		public GemfireTemplate gemfireTemplate(Region<?,?> region) {
			GemfireTemplate template = new GemfireTemplate(region);

			return template;
		}

		@Bean
		public PdxSerializer pdxSerializer() {
			return new PdxSerializer() {

				@Override
				public boolean toData(Object item, PdxWriter pdxWriter) {
					pdxWriter.writeByteArray("key", ((Item) item).getKey());
					pdxWriter.writeByteArray("record", ((Item) item).getRecord());
					return true;
				}

				@Override
				public Object fromData(Class<?> clazz, PdxReader pdxReader) {
					return new Item(pdxReader.readByteArray("key"), pdxReader.readByteArray("record"));
				}
			};
		}

		@Bean
		public Step workerStep(@Qualifier("requests") DirectChannel requests,
				@Qualifier("replies") DirectChannel replies,
				FileWritingStepExecutionListner listener) {
			return this.workerStepBuilderFactory.get("workerStep")
					.inputChannel(requests)
					.outputChannel(replies)
					.<Item, Item>chunk(1050000)
					.reader(reader(null))
					.writer(writer())
					.listener(listener)
					.build();
		}

//		@Bean
//		public DeployerStepExecutionHandler stepExecutionHandler(ApplicationContext context, JobExplorer jobExplorer,
//			JobRepository jobRepository) {
//			return new DeployerStepExecutionHandler(context, jobExplorer, jobRepository);
//		}
//
//		@Bean
//		public Step workerStep(StepBuilderFactory stepBuilderFactory, FileWritingStepExecutionListner listener) {
//			return stepBuilderFactory.get("workerStep")
//					.<Item, Item>chunk(1050000)
//					.reader(reader(null))
//					.writer(writer())
//					.listener(listener)
//					.build();
//		}

		@Bean
		@StepScope
		public SortFileItemReader reader(@Value("#{stepExecutionContext['fileName']}") Resource file) {
			SortFileItemReader reader = new SortFileItemReader();

			reader.setName("reader");
			reader.setResource(file);

			return reader;
		}

		@Bean
		public CompositeItemWriter<Item> writer() {
			return new CompositeItemWriterBuilder<Item>().delegates(
				Arrays.asList(new CountingItemWriter(), gemfireItemWriter(null))).build();
		}

		@Bean
		@StepScope
		public GemfireItemWriter<byte[], Item> gemfireItemWriter(GemfireTemplate template) {
			return new GemfireItemWriterBuilder<byte[], Item>().itemKeyMapper(Item::getKey).template(template).build();
		}

		@Bean
		public FileWritingStepExecutionListner listener(SortedFileWriterFunctionExecution functionExecution) {
			return new FileWritingStepExecutionListner(functionExecution);
		}
//
//		@Bean
//		public Step validationStep(StepBuilderFactory stepBuilderFactory, GemfireCountTasklet tasklet) {
//			return stepBuilderFactory.get("validationStep").tasklet(tasklet).build();
//		}
//
//		@Bean
//		@StepScope
//		public GemfireCountTasklet tasklet() {
//			return new GemfireCountTasklet(gemfireTemplate(null));
//		}

	}
}
