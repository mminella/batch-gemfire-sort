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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.spring.batch.domain.Item;
import org.apache.geode.cache.Region;
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
	public void readLocalPartition(RegionFunctionContext functionContext) throws IOException {
		System.out.println(">> in my gemfire function...");

		Region<byte[], Item> localData = PartitionRegionHelper.getLocalDataForContext(functionContext);

		Set<byte[]> bytes = localData.keySet();

		//			StringBuilder builder = new StringBuilder();
//			for (int i = 0; i < key.length; i++) {
//				builder.append(Integer.toBinaryString(key[i]));
//			}
//			return new BigInteger(builder.toString(), 2);
		List<BigInteger> keys = bytes.stream()
				.map(BigInteger::new)
				.sorted()
				.collect(Collectors.toList());

		writeFile(localData, keys);
	}

	private void writeFile(Region<byte[], Item> region, List<BigInteger> keys) throws IOException {
		FileChannel channel =
				new RandomAccessFile("output.dat", "rw").getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(1000000 * 50);

		for (BigInteger key: keys) {
			Item item = region.get(key);

			buffer.put(item.getRecord());

			if(!buffer.hasRemaining()) {
				channel.write(buffer);
				buffer.clear();
			}
		}

		channel.close();
	}
}
