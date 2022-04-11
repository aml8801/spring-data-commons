/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data;

import java.util.stream.Stream;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public interface EntityRegistrar {

	// leave out the module
	default void registerAll(String identifier, Iterable<Class<?>> types) {
		registerAll(DataModule.module(identifier), types);
	}

	default void registerAll(DataModule module, Iterable<Class<?>> types) {
		types.forEach(it -> register(module, it));
	}

	void register(DataModule module, Class<?> type);

	Stream<Class<?>> entitiesForModule(DataModule module);
}
