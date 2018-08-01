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
package io.spring.batch.domain;

import javax.xml.bind.DatatypeConverter;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;

/**
 * @author Michael Minella
 */
public class Item {

	@Id
	private final byte[] key;
	private final byte[] record;

	@PersistenceConstructor
	public Item(byte[] key, byte[] record) {
		this.key = key;
		this.record = record;
	}

	public byte[] getKey() {
		return key;
	}

	public byte[] getRecord() {
		return record;
	}

	@Override
	public String toString() {
		return "Item{" +
				"key='" + DatatypeConverter.printHexBinary(key) + '\'' +
				", record='" + DatatypeConverter.printHexBinary(record) + '\'' +
				'}';
	}
}
