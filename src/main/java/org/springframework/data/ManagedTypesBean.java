/*
 * Copyright 2022. the original author or authors.
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

/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2022/05
 */
public class ManagedTypesBean implements ManagedTypes {

	@Nullable
	private Lazy<Set<Class<?>>> types;

	public ManagedTypesBean(@Nullable Set<Class<?>> types) {
		this(() -> types);
	}

	public ManagedTypesBean(@Nullable Supplier<Set<Class<?>>> types) {
		this.types = types != null ? Lazy.of(types) : Lazy.empty();
	}

	public void setTypes(Set<Class<?>> types) {
		this.types = Lazy.of(types);
	}

	@Override
	public void forEach(Consumer<Class<?>> action) {
		if(types != null) {
			types.get().forEach(action);
		}
	}
}
