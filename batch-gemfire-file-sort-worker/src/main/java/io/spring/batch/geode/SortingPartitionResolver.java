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

import org.springframework.stereotype.Component;

/**
 * @author Michael Minella
 */
@Component("sortingPartitionResolver")
public class SortingPartitionResolver implements FixedPartitionResolver<byte[], Item> {

	private final List<BigInteger> partitionBorders = new ArrayList<>(4);

	public SortingPartitionResolver() {
		byte [] max = new byte[] {127, 127, 127, 127, 127, 127, 127, 127, 127, 127};

		BigInteger maxInteger = new BigInteger(max);

		BigInteger partitionKeySize = maxInteger.divide(new BigInteger("4"));

		BigInteger part1 = partitionKeySize;
		BigInteger part2 = part1.add(partitionKeySize);
		BigInteger part3 = part2.add(partitionKeySize);
		BigInteger part4 = maxInteger;

		partitionBorders.add(part1);
		partitionBorders.add(part2);
		partitionBorders.add(part3);
		partitionBorders.add(part4);
	}

	@Override
	public Object getRoutingObject(EntryOperation<byte[], Item> entryOperation) {

		BigInteger key = new BigInteger(1, entryOperation.getKey());

		for(int i = 0; i < partitionBorders.size(); i++) {
			if(key.compareTo(partitionBorders.get(i)) < 0) {
				System.out.println("for key " + key + " partition " + i + " is being used");
				return new RoutingObject(i);
			}
		}

		return new RoutingObject(3);
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
