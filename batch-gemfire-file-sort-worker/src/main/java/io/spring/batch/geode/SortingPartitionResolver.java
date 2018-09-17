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
package io.spring.batch.geode;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import io.spring.batch.domain.Item;
import org.apache.geode.cache.EntryOperation;
import org.apache.geode.cache.FixedPartitionResolver;
import org.apache.geode.cache.RegionAttributes;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author Michael Minella
 */
@Component("sortingPartitionResolver")
public class SortingPartitionResolver implements FixedPartitionResolver<byte[], Item>, ApplicationContextAware {

	private List<BigInteger> partitionBorders = new ArrayList<>(4);

	private List<Integer> partitionNames;

	private RegionAttributes regionAttributes;

	private boolean initialized = false;

	private SortedFileWriterFunctionExecution partitionNamesFunction;

	private ApplicationContext context;

	public SortingPartitionResolver() {

	}

	@Override
	public Object getRoutingObject(EntryOperation<byte[], Item> entryOperation) {
		if(!this.initialized) {
			initialize();
		}

		BigInteger key = new BigInteger(1, entryOperation.getKey());

		for(int i = this.partitionBorders.size() - 1; i >= 0; i--) {

			if(key.compareTo(this.partitionBorders.get(i)) >= 0) {
				System.out.println(String.format(">> for key %s partition %s was used", key, this.partitionNames.get(i)));
				return new RoutingObject(this.partitionNames.get(i));
			}
		}

		System.out.println("invalid key: " + key);
		throw new RuntimeException("Invalid key : " + key);
	}

	private void initialize() {

		System.out.println(">> In initialize");
		this.partitionNamesFunction = context.getBean(SortedFileWriterFunctionExecution.class);

		System.out.println(">> 1");

		List<String> partitionIds = (List<String>) this.partitionNamesFunction.getPartitionNames();
		System.out.println(">> partitionNames = " + partitionIds.getClass().getName());

		this.partitionNames = new ArrayList<>(partitionIds.size());

		for (String partitionId : partitionIds) {
			this.partitionNames.add(Integer.valueOf(partitionId));
		}

		byte [] max = new byte[] {127, 127, 127, 127, 127, 127, 127, 127, 127, 127};
		System.out.println(">> 2");

		BigInteger maxInteger = new BigInteger(max);
		System.out.println(">> 3");

		BigInteger partitionKeySize = maxInteger.divide(new BigInteger(String.valueOf(partitionNames.size())));
		System.out.println(">> 4");

		this.partitionBorders = new ArrayList<>(partitionNames.size() + 1);
		System.out.println(">> 5");

		BigInteger curBorder = new BigInteger("0");
		System.out.println(">> 6");

		this.partitionBorders.add(curBorder);

		for(int i = 0; i < partitionNames.size() - 1; i++) {
			System.out.println(">> 6.1");

			curBorder = curBorder.add(partitionKeySize);
			System.out.println(">> 6.2");
			this.partitionBorders.add(curBorder);
			System.out.println(">> 6.3");
		}

		System.out.println(">>> partition boarders:");
		this.partitionBorders.forEach(System.out::println);

		System.out.println(">>> partition names: ");
		this.partitionNames.forEach(System.out::println);

		System.out.println(">> 8");

		this.initialized = true;
	}

	@Override
	public String getName() {
		return "sort";
	}

	@Override
	public void close() {

	}

	@Override
	public String getPartitionName(EntryOperation<byte[], Item> entryOperation, Set<String> set) {
		return String.valueOf(getRoutingObject(entryOperation).hashCode());
	}

	@Override
	public void init(Properties props) {

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	public static class RoutingObject {

		private final int partitionId;

		public RoutingObject(int value) {
			this.partitionId = value;
		}

		@Override
		public int hashCode() {
			return partitionId;
		}
	}
}
