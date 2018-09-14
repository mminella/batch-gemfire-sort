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
import org.apache.geode.cache.FixedPartitionAttributes;
import org.apache.geode.cache.FixedPartitionResolver;
import org.apache.geode.cache.RegionAttributes;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author Michael Minella
 */
@Component("sortingPartitionResolver")
public class SortingPartitionResolver implements FixedPartitionResolver<byte[], Item> {

	private List<BigInteger> partitionBorders = new ArrayList<>(4);

	private List<String> partitionNames = new ArrayList<>(4);

	private RegionAttributes regionAttributes;

	private boolean initialized = false;

	private ApplicationContext context;

	public SortingPartitionResolver() {

	}

	@Override
	public Object getRoutingObject(EntryOperation<byte[], Item> entryOperation) {

		if(!this.initialized) {
			init();
		}

		BigInteger key = new BigInteger(1, entryOperation.getKey());

		for(int i = 0; i < this.partitionBorders.size(); i++) {
			if(key.compareTo(this.partitionBorders.get(i)) >= 0) {

				System.out.println(String.format(">> for key %s partition %s was used", key, this.partitionNames.get(i - 1)));
				return new RoutingObject(this.partitionNames.get(i - 1));
			}
		}

		int index = this.partitionNames.size() - 1;

		System.out.println(String.format(">> for key %s partition %s was used", key, this.partitionNames.get(index)));

		return new RoutingObject(this.partitionNames.get(index));
	}

	private void init() {
		this.regionAttributes = context.getBean("regionAttributes", RegionAttributes.class);

		List<FixedPartitionAttributes> fixedPartitionAttributes =
				this.regionAttributes.getPartitionAttributes().getFixedPartitionAttributes();

		partitionNames = new ArrayList<>(fixedPartitionAttributes.size());

		for (FixedPartitionAttributes fixedPartitionAttribute : fixedPartitionAttributes) {
			partitionNames.add(fixedPartitionAttribute.getPartitionName());
		}

		byte [] max = new byte[] {127, 127, 127, 127, 127, 127, 127, 127, 127, 127};

		BigInteger maxInteger = new BigInteger(max);

		BigInteger partitionKeySize = maxInteger.divide(new BigInteger(String.valueOf(partitionNames.size())));

		this.partitionBorders = new ArrayList<>(partitionNames.size());

		BigInteger curBorder = new BigInteger("0");

		for(int i = 0; i < partitionNames.size(); i++) {
			curBorder = curBorder.add(partitionKeySize);
			this.partitionBorders.add(curBorder);
		}

		this.partitionBorders.set(this.partitionNames.size() - 1, maxInteger);

		System.out.println(">>> partition boarders:");
		this.partitionBorders.forEach(System.out::println);

		System.out.println(">>> partition names: ");
		this.partitionNames.forEach(System.out::println);

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

	public static class RoutingObject {

		private final String partitionId;

		public RoutingObject(String value) {
			this.partitionId = value;
		}

		@Override
		public int hashCode() {
			return partitionId.hashCode();
		}
	}
}
