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
package io.spring.batch.configuration;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

/**
 * @author Michael Minella
 */
@Configuration
public class RabbitConfiguration {

	@Configuration
	public static class FileDownloadConfiguration {
		@Bean
		public Queue fileDownloadRequestQueue() {
			return QueueBuilder.nonDurable("fileDownloadRequests")
					.build();
		}

		@Bean
		public Queue fileDownloadReplyQueue() {
			return QueueBuilder.nonDurable("fileDownloadReplies")
					.build();
		}

		@Bean
		public DirectChannel fileDownloadRequests() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel fileDownloadReplies() {
			return new DirectChannel();
		}

		@Bean
		public IntegrationFlow fileDownloadInboundFlow(ConnectionFactory connectionFactory) {
			return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "fileDownloadRequests"))
					.channel(fileDownloadRequests())
					.get();
		}

		@Bean
		public IntegrationFlow fileDownloadOutboundFlow(AmqpTemplate amqpTemplate) {
			return IntegrationFlows.from(fileDownloadReplies())
					.handle(Amqp.outboundAdapter(amqpTemplate)
							.routingKey("fileDownloadReplies"))
					.get();
		}
	}

	@Configuration
	public static class Step1Configuration {
		@Bean
		public Queue requestQueues() {
			return QueueBuilder.nonDurable("requests")
					.build();
		}

		@Bean
		public Queue replyQueues() {
			return QueueBuilder.nonDurable("replies")
					.build();
		}

		@Bean
		public DirectChannel requests() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel replies() {
			return new DirectChannel();
		}

		@Bean
		public IntegrationFlow inboundFlow(ConnectionFactory connectionFactory) {
			return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "requests"))
					.channel(requests())
					.get();
		}

		@Bean
		public IntegrationFlow outboundFlow(AmqpTemplate amqpTemplate) {
			return IntegrationFlows.from(replies())
					.handle(Amqp.outboundAdapter(amqpTemplate)
							.routingKey("replies"))
					.get();
		}
	}

	@Configuration
	public static class Step2Configuration {

		@Bean
		public Queue fileRequestQueues() {
			return QueueBuilder.nonDurable("fileRequest")
					.build();
		}

		@Bean
		public Queue fileReplyQueues() {
			return QueueBuilder.nonDurable("fileReply")
					.build();
		}

		@Bean
		public DirectChannel fileRequests() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel fileReplies() {
			return new DirectChannel();
		}

		@Bean
		public IntegrationFlow fileInboundFlow(ConnectionFactory connectionFactory) {
			return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "fileRequest"))
					.channel(fileRequests())
					.get();
		}

		@Bean
		public IntegrationFlow fileOutboundFlow(AmqpTemplate amqpTemplate) {
			return IntegrationFlows.from(fileReplies())
					.handle(Amqp.outboundAdapter(amqpTemplate)
							.routingKey("fileReply"))
					.get();
		}
	}

}
