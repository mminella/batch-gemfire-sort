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

import java.util.Collections;

import io.spring.batch.domain.Item;
import io.spring.batch.geode.SortingPartitionResolver;
import org.apache.geode.cache.FixedPartitionAttributes;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.PartitionAttributes;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializer;
import org.apache.geode.pdx.PdxWriter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.FixedPartitionAttributesFactoryBean;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.PartitionAttributesFactoryBean;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;

/**
 * @author Michael Minella
 */
@Configuration
@PeerCacheApplication(name="SortClusterApplication", locators = "localhost[10334]")
@EnablePdx(serializerBeanName = "pdxSerializer")
//@EnableGemfireFunctionExecutions(basePackageClasses = SortedFileWriterFunction.class)
//@EnableGemfireFunctions
public class GeodeConfiguration {

	@Bean("Items")
	public PartitionedRegionFactoryBean<byte[], Item> partitionedRegion(GemFireCache gemfireCache,
			RegionAttributes regionAttributes) {

		PartitionedRegionFactoryBean<byte[], Item> partititonedRegion = new PartitionedRegionFactoryBean<>();

		partititonedRegion.setCache(gemfireCache);
		partititonedRegion.setClose(false);
		partititonedRegion.setPersistent(false);
		partititonedRegion.setAttributes(regionAttributes);

		return partititonedRegion;
	}

	@Bean
	public RegionAttributesFactoryBean regionAttributes(PartitionAttributes<byte[], Item> partitionAttributes) {

		RegionAttributesFactoryBean regionAttributes = new RegionAttributesFactoryBean();

		regionAttributes.setPartitionAttributes(partitionAttributes);

		return regionAttributes;
	}

	@Bean
	public PartitionAttributesFactoryBean<byte[], Item> partitionAttributes(
			FixedPartitionAttributes fixedPartitionAttributes,
			SortingPartitionResolver partitionResolver) {

		PartitionAttributesFactoryBean<byte[], Item> partitionAttributes = new PartitionAttributesFactoryBean<>();

		partitionAttributes.setFixedPartitionAttributes(Collections.singletonList(fixedPartitionAttributes));
		partitionAttributes.setPartitionResolver(partitionResolver);

		return partitionAttributes;
	}

	@Bean
	public FixedPartitionAttributesFactoryBean fixedPartitionAttributes(@Value("${partition.name}") String partitionName) {

		System.out.println(">> partitionName = " + partitionName);

		FixedPartitionAttributesFactoryBean fixedPartitionAttributes = new FixedPartitionAttributesFactoryBean();

		fixedPartitionAttributes.setPartitionName(partitionName);
		fixedPartitionAttributes.setPrimary(true);

		return fixedPartitionAttributes;

	}


	@Bean
	public GemfireTemplate gemfireTemplate(Region<?,?> region) {
		GemfireTemplate template = new GemfireTemplate(region);

		return template;
	}

	@Bean
	public PdxSerializer pdxSerializer() {
		return new PdxSerializer() {

			@Override
			public boolean toData(Object item, PdxWriter pdxWriter) {
				pdxWriter.writeByteArray("key", ((Item) item).getKey());
				pdxWriter.writeByteArray("record", ((Item) item).getRecord());
				return true;
			}

			@Override
			public Object fromData(Class<?> clazz, PdxReader pdxReader) {
				return new Item(pdxReader.readByteArray("key"), pdxReader.readByteArray("record"));
			}
		};
	}

}
