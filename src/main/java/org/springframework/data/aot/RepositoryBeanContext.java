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

package org.springframework.data.aot;

import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.util.ClassUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public interface RepositoryBeanContext {

	String getBeanName();

	boolean isTypePresent(String typeName);

	Optional<Class<?>> resolveType(String typeName);

	Class<?> resolveRequiredType(String typeName) throws ClassNotFoundException;

	@Nullable
	Class<?> resolveType(BeanReference beanReference);

	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	boolean containsProperty(String propertyName);

	@Nullable
	PropertyValue getPropertyValue(String propertyName);

	boolean hasConstructorArguments();

	Object getConstructorArgument(int index);

	<T> T getConstructorArgument(int index, Class<T> asType);

	RootBeanDefinition getRootBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	boolean isFactoryBean();

	boolean isFactoryBean(String beanName);

	default void ifTransactionManagerPresent(Consumer<String[]> beanNamesConsumer) {
		ClassUtils.ifPresent("org.springframework.transaction.TransactionManager", getBeanFactory().getBeanClassLoader(), it -> {
			String[] txMgrBeanNames = getBeanFactory().getBeanNamesForType(it);
			if(!ObjectUtils.isEmpty(txMgrBeanNames)) {
				beanNamesConsumer.accept(txMgrBeanNames);
			}
		});
	}

	ConfigurableListableBeanFactory getBeanFactory();
}
