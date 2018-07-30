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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningMasterStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.gemfire.config.annotation.EnableLocator;
import org.springframework.integration.channel.DirectChannel;

/**
 * @author Michael Minella
 */
@EnableBatchIntegration
@Configuration
public class BatchConfiguration {

	@EnableLocator
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

}
