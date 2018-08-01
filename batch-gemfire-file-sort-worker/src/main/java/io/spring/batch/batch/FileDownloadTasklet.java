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
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.io.FileUtils;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * @author Michael Minella
 */
public class FileDownloadTasklet implements Tasklet {

	private final AmazonS3Client s3Client;

	private final String objectKey;

	private final String workingDir;

	private final String bucketName;

	public FileDownloadTasklet(AmazonS3Client s3Client, String objectKey, String workingDir, String bucketName) {
		this.s3Client = s3Client;
		this.objectKey = objectKey;
		this.workingDir = workingDir;
		this.bucketName = bucketName;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		System.out.println(String.format(">> bucketName = %s objectKey = %s", this.bucketName, this.objectKey));

		S3Object object = s3Client.getObject(this.bucketName, this.objectKey);
		S3ObjectInputStream objectContent = object.getObjectContent();

		File destination = new File(workingDir + "/input");
		System.out.println(">> destination: " + destination.getAbsolutePath());

		FileUtils.copyInputStreamToFile(objectContent, destination);

		return RepeatStatus.FINISHED;
	}
}
