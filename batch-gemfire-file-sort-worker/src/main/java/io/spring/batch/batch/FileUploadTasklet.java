/*
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
package io.spring.batch.batch;

import java.io.File;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * @author Michael Minella
 */
public class FileUploadTasklet implements Tasklet {

	private final AmazonS3Client s3Client;

	private final String workingDir;

	private final String bucketName;

	private final File file;

	public FileUploadTasklet(AmazonS3Client s3Client, String workingDir, String bucketName, File file) {
		this.s3Client = s3Client;
		this.workingDir = workingDir;
		this.bucketName = bucketName;
		this.file = file;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		System.out.println(">> Uploading " + file.getName());

		s3Client.putObject(new PutObjectRequest(this.bucketName, file.getName(), file)
				.withCannedAcl(CannedAccessControlList.PublicRead));

		return RepeatStatus.FINISHED;
	}
}
