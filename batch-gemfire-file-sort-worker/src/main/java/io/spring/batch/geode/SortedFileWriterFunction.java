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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.stereotype.Component;

/**
 * @author Michael Minella
 */
@Component
public class SortedFileWriterFunction {

	private final String workingDirectory;

	public SortedFileWriterFunction(@Value("${spring.batch.working-directory}") String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	@GemfireFunction
	public Object readLocalPartition(FunctionContext functionContext) throws IOException {
		Region<byte[], Item> localData = PartitionRegionHelper.getLocalDataForContext((RegionFunctionContext) functionContext);

		Set<byte[]> keySet = localData.keySet();

		List<byte[]> keyList = Arrays.asList(keySet.toArray(new byte[keySet.size()][]));

		byte[] next = keyList.iterator().next();

		List<byte[]> keys = keyList.stream()
				.sorted((o1, o2) -> {
					BigInteger i1 = new BigInteger(1, o1);
					BigInteger i2 = new BigInteger(1, o2);

					return i1.compareTo(i2);
				})
				.collect(Collectors.toList());

		if(keys.size() > 0) {
			writeFile(localData, keys);
		}

		return "done";
	}

	private void writeFile(Region<byte[], Item> region, List<byte[]> keys) throws IOException {
		FileChannel channel =
				new RandomAccessFile(workingDirectory + "/" + String.format("output_%s-%s.dat", new BigInteger(1, keys.get(0)), new BigInteger(1, keys.get(keys.size() - 1))), "rw").getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(1000000 * 50);

		for (byte[] key: keys) {
			Item item = region.get(key);

			buffer.put(item.getRecord());

			if(!buffer.hasRemaining()) {
				buffer.flip();
				channel.write(buffer);
				buffer.clear();
			}
		}

		buffer.flip();
		channel.write(buffer);
		buffer.clear();
		channel.close();
	}
}
