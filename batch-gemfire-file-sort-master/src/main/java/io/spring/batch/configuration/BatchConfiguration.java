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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import io.spring.batch.batch.S3Partitioner;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningMasterStepBuilderFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
				.start(fileDownloadMaster(null, null, null))
				.next(ingestFileMaster(null, null, null))
				.next(fileWriter(null, null))
				.next(fileUpload(null, null))
				.build();
	}

	@Bean
	public Partitioner filePartitioner(@Value("${amazonProperties.accessKey}") String accessKey,
			@Value("${amazonProperties.secretKey}") String secretKey,
			@Value("${amazonProperties.bucketName}") String bucket) {

		AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
		AmazonS3Client s3Client = new AmazonS3Client(creds);

		return new S3Partitioner(s3Client, bucket);
	}

	@Bean
	public Step fileDownloadMaster(@Value("${spring.batch.grid-size}") Integer gridSize,
			@Qualifier("fileDownloadRequests") DirectChannel fileDownloadRequests,
			@Qualifier("fileDownloadReplies") DirectChannel fileDownloadReplies) {
		return this.masterStepBuilderFactory.get("fileDownloadMaster")
				.partitioner("fileDownloadStep", filePartitioner(null, null, null))
				.gridSize(gridSize)
				.outputChannel(fileDownloadRequests)
				.inputChannel(fileDownloadReplies)
				.build();
	}

	@Bean
	public Step ingestFileMaster(@Qualifier("requests") DirectChannel requests,
			@Qualifier("replies") DirectChannel replies,
			@Value("${spring.batch.grid-size}") Integer gridSize) throws IOException {
		return this.masterStepBuilderFactory.get("ingestFileMaster")
				.partitioner("ingestStep", filePartitioner(null, null, null))
				.gridSize(gridSize)
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

	@Bean
	public Step fileUpload(@Qualifier("fileUploadRequests") DirectChannel fileUploadRequests, @Qualifier("fileUploadReplies") DirectChannel fileUploadReplies) {
		return this.masterStepBuilderFactory.get("fileWriter")
				.partitioner("fileUpload", filePartitioner())
				.gridSize(2)
				.outputChannel(fileUploadRequests)
				.inputChannel(fileUploadReplies)
				.build();
	}
}
