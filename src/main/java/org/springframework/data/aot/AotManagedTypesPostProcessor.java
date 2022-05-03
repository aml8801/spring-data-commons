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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.data.ManagedTypes;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 3.0
 */
public class AotManagedTypesPostProcessor implements AotContributingBeanPostProcessor, BeanFactoryAware {

	private BeanFactory beanFactory;

	@Nullable
	@Override
	public BeanInstantiationContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType,
			String beanName) {

		if (!(ClassUtils.isAssignable(ManagedTypes.class, beanType) && beanName.startsWith(modulePrefix()))
				&& beanDefinition.hasConstructorArgumentValues()) {
			return null;
		}

		ValueHolder argumentValue = beanDefinition.getConstructorArgumentValues().getArgumentValue(0, Set.class);
		if (argumentValue.getValue() == null) {
			return null;
		}

		return contribute((ManagedTypes) argumentValue.getValue());
	}

	protected ManagedTypesContribution contribute(ManagedTypes managedTypes) {
		return new ManagedTypesContribution(managedTypes);
	}

	protected String modulePrefix() {
		return "";
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public static class ManagedTypesContribution implements BeanInstantiationContribution {

		private ManagedTypes managedTypes;

		public ManagedTypesContribution(ManagedTypes managedTypes) {
			this.managedTypes = managedTypes;
		}

		@Override
		public void applyTo(CodeContribution contribution) {

			List<Class<?>> types = new ArrayList<>(100);
			managedTypes.forEach(types::add);

			if (types.isEmpty()) {
				return;
			}

			TypeCollector.inspect(types).forEach(type -> {
				contributeType(type, contribution);
			});
		}

		protected void contributeType(ResolvableType type, CodeContribution contribution) {

			TypeContributor.contribute(type.toClass(), Collections.singleton("org.springframework.data"), contribution);
			TypeUtils.resolveUsedAnnotations(type.toClass()).forEach(annotation -> TypeContributor
					.contribute(annotation.getType(), Collections.singleton("org.springframework.data"), contribution));
		}
	}
}
