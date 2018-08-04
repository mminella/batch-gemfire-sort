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

import java.io.IOException;

import io.spring.batch.geode.SortedFileWriterFunctionExecution;
import org.apache.shiro.util.Assert;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * @author Michael Minella
 */
public class FileWritingTasklet implements Tasklet {

	private final SortedFileWriterFunctionExecution function;

	public FileWritingTasklet(SortedFileWriterFunctionExecution function) {
		this.function = function;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

		try {
			Object done = function.readLocalPartition();

			Assert.notNull(done);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return RepeatStatus.FINISHED;
	}
}
