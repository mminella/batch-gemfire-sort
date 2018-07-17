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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.spring.batch.domain.Item;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.apache.geode.cache.partition.PartitionRegionHelper;

import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.stereotype.Component;

/**
 * @author Michael Minella
 */
@Component
public class SortedFileWriterFunction {

	@GemfireFunction
	public void readLocalPartition(FunctionContext functionContext) throws IOException {
		System.out.println(">> in my gemfire function...");

		Region<byte[], Item> localData = PartitionRegionHelper.getLocalDataForContext((RegionFunctionContext) functionContext);

		Set<byte[]> keySet = localData.keySet();

		List<byte[]> keyList = Arrays.asList(keySet.toArray(new byte[keySet.size()][]));

		System.out.println(">> Number of keys found: " + keyList.size());

		byte[] next = keyList.iterator().next();

//		System.out.println("----B-E-F-O-R-E------");
//		System.out.println(DatatypeConverter.printHexBinary(next));
//		System.out.println("---------------------");
//		System.out.println(">> Does the region have the key: " + localData.containsKey(next));
//		System.out.println(">> Does the key have a value: " + localData.containsValueForKey(next));

		List<byte[]> keys = keyList.stream()
				.sorted((o1, o2) -> {
					BigInteger i1 = new BigInteger(o1);
					BigInteger i2 = new BigInteger(o2);

					return i1.compareTo(i2);
				})
				.collect(Collectors.toList());

		if(keys.size() > 0) {
			writeFile(localData, keys);
		}
	}

	private void writeFile(Region<byte[], Item> region, List<byte[]> keys) throws IOException {
		FileChannel channel =
				new RandomAccessFile(String.format("output%s-%s.dat", new BigInteger(keys.get(0)), new BigInteger(keys.get(keys.size() - 1))), "rw").getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(1000000 * 50);

		for (byte[] key: keys) {
//			System.out.println("----A-F-T-E-R------");
//			System.out.println(DatatypeConverter.printHexBinary(key));
//			System.out.println("---------------------");
//
//			System.out.println(">> Does the region have the key: " + region.containsKey(key));
//			System.out.println(">> Does the key have a value: " + region.containsValueForKey(key));
			Item item = region.get(key);
//			System.out.println(">> item = " + item);

			buffer.put(item.getRecord());
//			System.out.println(String.format(">> Position: %s  Limit: %s  Has Remaining: %s", buffer.position(), buffer.limit(), buffer.hasRemaining()));

			if(!buffer.hasRemaining()) {
				System.out.println(">> let's write the buffer out with the size: " + buffer.position() + " and channel size of " + channel.size());
				channel.write(buffer);
				channel.force(false);
				System.out.println(">> channel.size = " + channel.size());
				buffer.clear();
			}
		}

		System.out.println(">> let's write the buffer out");
		channel.write(buffer);
		channel.force(false);
		System.out.println(">> channel.size = " + channel.size());
		buffer.clear();
		System.out.println(">> file size: " + channel.size());
		channel.close();
	}
}
