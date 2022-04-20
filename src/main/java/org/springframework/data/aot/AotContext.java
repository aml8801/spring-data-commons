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
package org.springframework.data.aot;

import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public interface AotContext {

	ConfigurableListableBeanFactory getBeanFactory();

	default ClassLoader getClassLoader() {
		return getBeanFactory().getBeanClassLoader();
	}

	default boolean isTypePresent(String typeName) {
		return ClassUtils.isPresent(typeName, getBeanFactory().getBeanClassLoader());
	}

	default TypeScanner getTypeScanner() {
		return new TypeScanner(getClassLoader());
	}

	default Optional<Class<?>> resolveType(String typeName) {

		if (!isTypePresent(typeName)) {
			return Optional.empty();
		}
		try {
			return Optional.of(resolveRequiredType(typeName));
		} catch (ClassNotFoundException e) {
			// just do nothing
		}
		return Optional.empty();
	}

	default Class<?> resolveRequiredType(String typeName) throws ClassNotFoundException {
		return ClassUtils.forName(typeName, getClassLoader());
	}

	@Nullable
	default Class<?> resolveType(BeanReference beanReference) {
		return getBeanFactory().getType(beanReference.getBeanName(), false);
	}

	default BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getBeanDefinition(beanName);
	}

	default RootBeanDefinition getRootBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {

		BeanDefinition val = getBeanFactory().getBeanDefinition(beanName);
		if (!(val instanceof RootBeanDefinition)) {
			throw new IllegalStateException(String.format("%s is not a root bean", beanName));
		}
		return RootBeanDefinition.class.cast(val);
	}

	default boolean isFactoryBean(String beanName) {
		return getBeanFactory().isFactoryBean(beanName);
	}

	default void ifTransactionManagerPresent(Consumer<String[]> beanNamesConsumer) {

		org.springframework.data.repository.util.ClassUtils.ifPresent("org.springframework.transaction.TransactionManager",
				getBeanFactory().getBeanClassLoader(), it -> {
					String[] txMgrBeanNames = getBeanFactory().getBeanNamesForType(it);
					if (!ObjectUtils.isEmpty(txMgrBeanNames)) {
						beanNamesConsumer.accept(txMgrBeanNames);
					}
				});
	}

}
