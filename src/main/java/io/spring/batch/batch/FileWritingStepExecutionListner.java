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

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * @author Michael Minella
 */
public class FileWritingStepExecutionListner implements StepExecutionListener {

	private SortedFileWriterFunctionExecution function;

	public FileWritingStepExecutionListner(SortedFileWriterFunctionExecution function) {
		this.function = function;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {

	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		System.out.println(">> afterStep was called");
		try {
			function.readLocalPartition();

			System.out.println(">> file should be there...");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return stepExecution.getExitStatus();
	}
}
