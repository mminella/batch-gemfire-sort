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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Michael Minella
 */
public class S3Partitioner implements Partitioner {

	private final AmazonS3Client s3Client;

	private final String bucketName;

	public S3Partitioner(AmazonS3Client s3Client, String bucketName) {
		this.s3Client = s3Client;
		this.bucketName = bucketName;
	}

	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		ObjectListing objectListing = this.s3Client.listObjects(this.bucketName);

		List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();

		Map<String, ExecutionContext> executionContexts = new HashMap<>(objectSummaries.size());

		int i = 0;

		for (S3ObjectSummary objectSummary : objectSummaries) {
			ExecutionContext context = new ExecutionContext();

			context.put("fileName", objectSummary.getKey());

			System.out.println(">> Current context = " + context.toString());

			executionContexts.put("partition" + i++, context);
		}

		return executionContexts;
	}
}
