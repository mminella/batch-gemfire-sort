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

import org.apache.geode.cache.query.SelectResults;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.gemfire.GemfireTemplate;

/**
 * @author Michael Minella
 */
public class GemfireCountTasklet implements Tasklet {

	private final GemfireTemplate template;

	public GemfireCountTasklet(GemfireTemplate template) {
		this.template = template;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		System.out.println(">>> We're in the tasklet");

		SelectResults<Integer> results =
				this.template.find(String.format("SELECT count(*) FROM %s", this.template.getRegion().getFullPath()));

		System.out.println(String.format(">> We put %d items in the cache", Long.valueOf(results.iterator().next())));

		return RepeatStatus.FINISHED;
	}
}
