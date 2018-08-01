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
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningMasterStepBuilderFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.integration.channel.DirectChannel;

/**
 * @author Michael Minella
 */
@EnableBatchIntegration
@Configuration
public class BatchConfiguration {

	@Autowired
	private RemotePartitioningMasterStepBuilderFactory masterStepBuilderFactory;

	@Bean
	public Job batchGemfireSort(JobBuilderFactory jobBuilderFactory) throws Exception {
		return jobBuilderFactory.get("batchGemfireSort")
				.start(master(null, null))
				.next(fileWriter(null, null))
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
	public Step master(@Qualifier("requests") DirectChannel requests, @Qualifier("replies") DirectChannel replies) throws IOException {
		return this.masterStepBuilderFactory.get("master")
				.partitioner("workerStep", partitioner(null))
				.gridSize(2)
				.outputChannel(requests)
				.inputChannel(replies)
				.build();
	}

	@Bean
	public Step fileWriter(@Qualifier("fileRequests") DirectChannel fileRequests, @Qualifier("fileReplies") DirectChannel fileReplies) {
		return this.masterStepBuilderFactory.get("fileWriter")
				.partitioner("fileWriterStep", filePartitioner())
				.gridSize(2)
				.outputChannel(fileRequests)
				.inputChannel(fileReplies)
				.build();
	}

	@Bean
	public Partitioner filePartitioner() {
		return gridSize -> {

			Map<String, ExecutionContext> partitions = new HashMap<>(gridSize);

			for(int i = 0; i < gridSize; i++) {
				partitions.put("partition" + i, new ExecutionContext());
			}

			return partitions;
		};
	}
}
