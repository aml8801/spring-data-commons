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

import java.util.Optional;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
record AotBeanContext(String beanName, RootBeanDefinition beanDefinition, ConfigurableListableBeanFactory beanFactory) {

	String getBeanName() {
		return beanName;
	}

	RootBeanDefinition getBeanDefinition() {
		return beanDefinition;
	}

	ConfigurableListableBeanFactory getBeanFactory() {
		return beanFactory;
	}

	boolean isTypePresent(String typeName) {
		return ClassUtils.isPresent(typeName, beanFactory.getBeanClassLoader());
	}

	Optional<Class<?>> resolveType(String typeName) {
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

	Class<?> resolveRequiredType(String typeName) throws ClassNotFoundException {
		return ClassUtils.forName(typeName, beanFactory.getBeanClassLoader());
	}

	@Nullable
	Class<?> resolveType(BeanReference beanReference) {
		return beanFactory.getType(beanReference.getBeanName(), false);
	}

	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		return beanFactory.getBeanDefinition(beanName);
	}

	boolean containsProperty(String propertyName) {
		return beanDefinition.getPropertyValues().contains(propertyName);
	}

	@Nullable
	PropertyValue getPropertyValue(String propertyName) {
		return beanDefinition.getPropertyValues().getPropertyValue(propertyName);
	}

	boolean hasConstructorArguments() {
		return beanDefinition.getConstructorArgumentValues().getArgumentCount() > 0;
	}

	Object getConstructorArgument(int index) {
		ValueHolder arg = beanDefinition.getConstructorArgumentValues().getArgumentValue(index, null);
		return arg.getValue();
	}

	<T> T getConstructorArgument(int index, Class<T> asType) {

		Object value = getConstructorArgument(index);
		if (value == null) {
			return null;
		}

		if (!ClassUtils.isAssignable(asType, value.getClass())) {
			throw new IllegalArgumentException(String.format("Found %s but wanted %s.", value.getClass(), asType));
		}

		return asType.cast(value);
	}

	RootBeanDefinition getRootBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		BeanDefinition val = beanFactory.getBeanDefinition(beanName);
		if (!(val instanceof RootBeanDefinition)) {
			throw new IllegalStateException("oh oh");
		}
		return RootBeanDefinition.class.cast(val);
	}

	boolean isFactoryBean() {
		return isFactoryBean(beanName);
	}

	boolean isFactoryBean(String beanName) {
		return beanFactory.isFactoryBean(beanName);
	}
}
