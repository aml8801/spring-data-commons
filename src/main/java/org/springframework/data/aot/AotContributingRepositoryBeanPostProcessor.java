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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.Repository;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/03
 */
public class AotContributingRepositoryBeanPostProcessor implements AotContributingBeanPostProcessor, BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;

	@Nullable
	@Override
	public RepositoryBeanContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {

		if (!ClassUtils.isAssignable(Repository.class, beanDefinition.getTargetType())) {
			return null;
		}

		AotBeanContext beanContext = new AotBeanContext(beanName, beanDefinition, beanFactory);
		AotRepositoryInformation repositoryInformation = AotRepositoryInformation.from(beanContext);

		return new RepositoryBeanContribution(beanContext, repositoryInformation, discoverTypes(repositoryInformation, typeFilter()));
	}

	protected Set<Class<?>> discoverTypes(AotRepositoryInformation repositoryInformation, Predicate<Class<?>> filter) {

		Set<Class<?>> types = new LinkedHashSet<>();
		// TODO:

		// read domain type info from the repo

		// look for additioinal types with the filter predicate

		


		return types;
	}

	public Predicate<Class<?>> typeFilter() { // like only document ones.
		return it -> true;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}
}
