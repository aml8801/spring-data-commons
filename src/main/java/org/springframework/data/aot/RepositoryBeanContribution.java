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
package org.springframework.data.aot;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.data.repository.core.RepositoryInformation;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class RepositoryBeanContribution implements BeanInstantiationContribution {

	private final RepositoryInformation repositoryInformation;
	private Set<Class<?>> discoveredTypes;

	public RepositoryBeanContribution(RepositoryInformation repositoryInformation, Collection<Class<?>> discoveredTypes) {

		this.repositoryInformation = repositoryInformation;
		this.discoveredTypes = new LinkedHashSet<>(discoveredTypes);
	}



	@Override
	public void applyTo(CodeContribution contribution) {



		discoveredTypes
				.stream()
				.filter(this::contributeTypeInfo)
				.forEach(it -> contributeType(it, contribution));



	}

    protected boolean contributeTypeInfo(Class<?> type) {
		return true;
	}

	protected void contributeType(Class<?> type, CodeContribution contribution) {
		// todo
	}

	protected void contributeRepositoryInformation(RepositoryInformation repositoryInformation, CodeContribution contribution) {
		// todo
	}

	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	public Set<Class<?>> getDiscoveredTypes() {
		return discoveredTypes;
	}

	public void setDiscoveredTypes(Set<Class<?>> discoveredTypes) {
		this.discoveredTypes = discoveredTypes;
	}
}
