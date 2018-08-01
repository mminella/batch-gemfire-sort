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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import io.spring.batch.batch.FileDownloadTasklet;
import io.spring.batch.batch.FileWritingTasklet;
import io.spring.batch.batch.GemfireItemWriter;
import io.spring.batch.batch.SortFileItemReader;
import io.spring.batch.domain.Item;
import io.spring.batch.geode.SortedFileWriterFunctionExecution;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.integration.channel.DirectChannel;

/**
 * @author Michael Minella
 */
@EnableBatchIntegration
@Configuration
public class BatchConfiguration {

	@Autowired
	private RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory;

	@StepScope
	@Bean
	public FileDownloadTasklet fileDownloadTasklet(@Value("${amazonProperties.accessKey}") String accessKey,
			@Value("${amazonProperties.secretKey}") String secretKey,
			@Value("#{stepExecutionContext['fileName']}") String fileName,
			@Value("${spring.batch.working-directory}") String workingDir,
			@Value("${amazonProperties.bucketName}") String bucketName) {

		AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
		AmazonS3Client s3Client = new AmazonS3Client(creds);

		return new FileDownloadTasklet(s3Client, fileName, workingDir, bucketName);
	}

	@Bean
	public Step fileDownloadStep(@Qualifier("fileDownloadRequests") DirectChannel fileDownloadRequests,
			@Qualifier("fileDownloadReplies") DirectChannel fileDownloadReplies) {
		return this.workerStepBuilderFactory.get("fileDownloadStep")
				.inputChannel(fileDownloadRequests)
				.outputChannel(fileDownloadReplies)
				.tasklet(fileDownloadTasklet(null, null, null, null, null))
				.build();
	}

	@Bean
	public Step ingestStep(@Qualifier("requests") DirectChannel requests,
			@Qualifier("replies") DirectChannel replies) {
		return this.workerStepBuilderFactory.get("ingestStep")
				.inputChannel(requests)
				.outputChannel(replies)
				.<Item, Item>chunk(1050000)
				.reader(reader(null))
				.writer(gemfireItemWriter(null))
				.build();
	}

	@Bean
	@StepScope
	public SortFileItemReader reader(@Value("file://${spring.batch.working-directory}/input") Resource file) {
		SortFileItemReader reader = new SortFileItemReader();

		reader.setName("reader");
		reader.setResource(file);

		return reader;
	}

	@Bean
	@StepScope
	public GemfireItemWriter gemfireItemWriter(GemfireTemplate template) {
		return new GemfireItemWriter(template);
	}

	@Bean
	public Step fileWriterStep(@Qualifier("fileRequests") DirectChannel requests,
			@Qualifier("fileReplies") DirectChannel replies,
			SortedFileWriterFunctionExecution functionExecution) {
		return this.workerStepBuilderFactory.get("fileWriterStep")
				.inputChannel(requests)
				.outputChannel(replies)
				.tasklet(new FileWritingTasklet(functionExecution))
				.build();
	}
}
